package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.VinDecodificadoResponse;
import com.tallerpro.backend.service.CatalogoVehiculosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Proxy del backend hacia NHTSA vPIC (ver CatalogoVehiculosService): el
 * navegador nunca llama a vPIC directo. Vive bajo /api/clientes/** a
 * proposito, igual que DireccionController, para heredar la misma regla de
 * permisos sin tocar SecurityConfig.
 */
@RestController
@RequestMapping("/api/clientes/catalogos/vehiculos")
public class CatalogoVehiculosController {

    private final CatalogoVehiculosService catalogoVehiculosService;

    public CatalogoVehiculosController(CatalogoVehiculosService catalogoVehiculosService) {
        this.catalogoVehiculosService = catalogoVehiculosService;
    }

    @GetMapping("/marcas")
    public List<String> marcas() {
        return catalogoVehiculosService.obtenerMarcas();
    }

    @GetMapping("/modelos")
    public List<String> modelos(@RequestParam String marca, @RequestParam(required = false) Integer anio) {
        return catalogoVehiculosService.obtenerModelos(marca, anio);
    }

    @GetMapping("/vin/{vin}")
    public VinDecodificadoResponse decodificarVin(@PathVariable String vin) {
        return catalogoVehiculosService.decodificarVin(vin);
    }
}
