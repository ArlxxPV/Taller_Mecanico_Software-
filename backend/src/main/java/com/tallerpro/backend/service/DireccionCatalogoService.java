package com.tallerpro.backend.service;

import com.tallerpro.backend.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Catalogo de direcciones de Mexico (estados/municipios/colonias+CP) para los
 * selects en cascada del formulario de clientes. Usa Postali (postali.app,
 * catalogo oficial de SEPOMEX): gratuita, sin token y sin registro.
 * (Se cambio de COPOMEX a Postali porque COPOMEX exige un token real para
 * devolver nombres de verdad -con "pruebas" regresa nombres aleatorios- y
 * el espejo gratuito compatible con COPOMEX estaba caido).
 *
 * Postali identifica estados/municipios por "slug" (ej. "ciudad-de-mexico"),
 * no por su nombre visible. Aqui se guarda en memoria el nombre->slug de
 * cada nivel ya consultado, para no tener que re-pedir el catalogo completo
 * en cada llamada ni exponer el concepto de "slug" al resto de la app.
 */
@Service
public class DireccionCatalogoService {

    private static final Logger log = LoggerFactory.getLogger(DireccionCatalogoService.class);
    private static final String MENSAJE_FALLA_GENERICA =
            "No se pudo consultar el catalogo de direcciones. Intenta de nuevo en unos minutos.";

    private final RestClient restClient;
    private final Map<String, String> slugDeEstado = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> slugDeMunicipioPorEstado = new ConcurrentHashMap<>();

    public DireccionCatalogoService() {
        this.restClient = RestClient.builder().baseUrl("https://postali.app/api/v1/mx").build();
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerEstados() {
        Map<String, Object> cuerpo = ejecutar(uriBuilder -> uriBuilder.path("/estados").build());
        List<Map<String, Object>> estados = (List<Map<String, Object>>) cuerpo.get("estados");
        estados.forEach(e -> slugDeEstado.put((String) e.get("nombre"), (String) e.get("slug")));
        return estados.stream().map(e -> (String) e.get("nombre")).toList();
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerMunicipios(String estadoNombre) {
        String estadoSlug = resolverSlugEstado(estadoNombre);
        Map<String, Object> cuerpo = ejecutar(uriBuilder -> uriBuilder
                .path("/estado/{estado}/municipios")
                .build(estadoSlug));
        List<Map<String, Object>> municipios = (List<Map<String, Object>>) cuerpo.get("municipios");

        Map<String, String> slugsDeEsteEstado =
                slugDeMunicipioPorEstado.computeIfAbsent(estadoSlug, k -> new ConcurrentHashMap<>());
        municipios.forEach(m -> slugsDeEsteEstado.put((String) m.get("nombre"), (String) m.get("slug")));
        return municipios.stream().map(m -> (String) m.get("nombre")).toList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> obtenerColonias(String estadoNombre, String municipioNombre) {
        String estadoSlug = resolverSlugEstado(estadoNombre);
        String municipioSlug = resolverSlugMunicipio(estadoNombre, estadoSlug, municipioNombre);

        Map<String, Object> cuerpo = ejecutar(uriBuilder -> uriBuilder
                .path("/municipio/{estado}/{municipio}")
                .build(estadoSlug, municipioSlug));
        List<Map<String, Object>> asentamientos = (List<Map<String, Object>>) cuerpo.get("colonias");

        // Orden alfabetico (con acentos) por colonia, tal como lo pide el formulario.
        Map<String, String> colonias = new TreeMap<>(Collator.getInstance(new Locale("es", "MX")));
        if (asentamientos != null) {
            asentamientos.forEach(a -> colonias.put(String.valueOf(a.get("nombre")), String.valueOf(a.get("cp"))));
        }
        return colonias;
    }

    private String resolverSlugEstado(String estadoNombre) {
        String slug = slugDeEstado.get(estadoNombre);
        if (slug == null) {
            obtenerEstados();
            slug = slugDeEstado.get(estadoNombre);
        }
        if (slug == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El estado seleccionado no es valido.");
        }
        return slug;
    }

    private String resolverSlugMunicipio(String estadoNombre, String estadoSlug, String municipioNombre) {
        Map<String, String> slugsDeEsteEstado = slugDeMunicipioPorEstado.get(estadoSlug);
        String slug = slugsDeEsteEstado != null ? slugsDeEsteEstado.get(municipioNombre) : null;
        if (slug == null) {
            obtenerMunicipios(estadoNombre);
            slugsDeEsteEstado = slugDeMunicipioPorEstado.get(estadoSlug);
            slug = slugsDeEsteEstado != null ? slugsDeEsteEstado.get(municipioNombre) : null;
        }
        if (slug == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El municipio seleccionado no es valido.");
        }
        return slug;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ejecutar(Function<UriBuilder, URI> armarUri) {
        try {
            return restClient.get().uri(armarUri).retrieve().body(Map.class);
        } catch (RestClientResponseException ex) {
            log.warn("Postali respondio HTTP {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException(HttpStatus.BAD_REQUEST, MENSAJE_FALLA_GENERICA);
        }
    }
}
