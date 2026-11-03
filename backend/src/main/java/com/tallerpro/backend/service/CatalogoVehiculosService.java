package com.tallerpro.backend.service;

import com.tallerpro.backend.dto.VinDecodificadoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catalogo de marcas/modelos de vehiculos via la API oficial y gratuita de
 * NHTSA vPIC (no requiere llave). Se consulta SOLO desde aqui (nunca desde
 * el navegador) y se cachea en memoria 7 dias, como pide la maqueta.
 * Si vPIC no responde, se regresa lo que haya en cache (aunque ya este
 * vencido) o una lista vacia - nunca se lanza un error que tumbe el
 * formulario: el usuario siempre puede escribir la marca/modelo a mano.
 */
@Service
public class CatalogoVehiculosService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoVehiculosService.class);
    private static final Duration TTL = Duration.ofDays(7);
    private static final List<String> TIPOS_VEHICULO = List.of("car", "mpv", "truck");

    private record Entrada<T>(T valor, Instant expiraEn) {
        boolean vigente() {
            return Instant.now().isBefore(expiraEn);
        }
    }

    private final RestClient restClient;
    private volatile Entrada<List<String>> marcasCache;
    private final Map<String, Entrada<List<String>>> modelosCache = new ConcurrentHashMap<>();

    public CatalogoVehiculosService() {
        this.restClient = RestClient.builder().baseUrl("https://vpic.nhtsa.dot.gov/api/vehicles").build();
    }

    public List<String> obtenerMarcas() {
        if (marcasCache != null && marcasCache.vigente()) {
            return marcasCache.valor();
        }
        try {
            Set<String> marcas = new LinkedHashSet<>();
            for (String tipo : TIPOS_VEHICULO) {
                Map<String, Object> cuerpo = ejecutar("/GetMakesForVehicleType/" + tipo + "?format=json");
                extraerResultados(cuerpo).forEach(r -> {
                    Object nombre = r.get("MakeName");
                    if (nombre != null) marcas.add(String.valueOf(nombre));
                });
            }
            List<String> ordenadas = marcas.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            marcasCache = new Entrada<>(ordenadas, Instant.now().plus(TTL));
            return ordenadas;
        } catch (Exception e) {
            log.warn("No se pudo consultar marcas en NHTSA vPIC: {}", e.getMessage());
            return marcasCache != null ? marcasCache.valor() : List.of();
        }
    }

    public List<String> obtenerModelos(String marca, Integer anio) {
        if (marca == null || marca.isBlank()) {
            return List.of();
        }
        boolean usaAnio = anio != null && anio > 1995;
        String clave = marca.toUpperCase() + "|" + (usaAnio ? anio : "TODOS");

        Entrada<List<String>> enCache = modelosCache.get(clave);
        if (enCache != null && enCache.vigente()) {
            return enCache.valor();
        }
        try {
            String ruta = usaAnio
                    ? "/GetModelsForMakeYear/make/" + marca + "/modelyear/" + anio + "?format=json"
                    : "/GetModelsForMake/" + marca + "?format=json";
            Map<String, Object> cuerpo = ejecutar(ruta);
            Set<String> modelos = new LinkedHashSet<>();
            extraerResultados(cuerpo).forEach(r -> {
                Object nombre = r.get("Model_Name");
                if (nombre != null) modelos.add(String.valueOf(nombre));
            });
            List<String> ordenados = modelos.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            modelosCache.put(clave, new Entrada<>(ordenados, Instant.now().plus(TTL)));
            return ordenados;
        } catch (Exception e) {
            log.warn("No se pudo consultar modelos en NHTSA vPIC para '{}': {}", marca, e.getMessage());
            return enCache != null ? enCache.valor() : List.of();
        }
    }

    public VinDecodificadoResponse decodificarVin(String vin) {
        if (vin == null || vin.isBlank()) {
            return VinDecodificadoResponse.vacio();
        }
        try {
            Map<String, Object> cuerpo = ejecutar("/DecodeVinValues/" + vin + "?format=json");
            List<Map<String, Object>> resultados = extraerResultados(cuerpo);
            if (resultados.isEmpty()) {
                return VinDecodificadoResponse.vacio();
            }
            Map<String, Object> r = resultados.get(0);
            return new VinDecodificadoResponse(
                    valorONulo(r.get("Make")),
                    valorONulo(r.get("Model")),
                    valorONulo(r.get("ModelYear")),
                    valorONulo(r.get("Trim"))
            );
        } catch (Exception e) {
            log.warn("No se pudo decodificar el VIN '{}' en NHTSA vPIC: {}", vin, e.getMessage());
            return VinDecodificadoResponse.vacio();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extraerResultados(Map<String, Object> cuerpo) {
        Object resultados = cuerpo.get("Results");
        return resultados instanceof List ? (List<Map<String, Object>>) resultados : List.of();
    }

    private String valorONulo(Object valor) {
        String texto = valor == null ? null : String.valueOf(valor).trim();
        return (texto == null || texto.isEmpty()) ? null : texto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ejecutar(String ruta) {
        try {
            return restClient.get().uri(ruta).retrieve().body(Map.class);
        } catch (RestClientResponseException ex) {
            log.warn("NHTSA vPIC respondio HTTP {} en {}", ex.getStatusCode(), ruta);
            throw ex;
        }
    }
}
