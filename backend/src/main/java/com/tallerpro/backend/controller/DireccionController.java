package com.tallerpro.backend.controller;

import com.tallerpro.backend.service.DireccionCatalogoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Catalogos de direccion (estados/municipios/colonias) para los selects en
 * cascada del formulario de clientes. Vive bajo /api/clientes/** a proposito:
 * hereda la misma regla de permisos (ADMIN/RECEPCIONISTA) sin tocar SecurityConfig.
 */
@RestController
@RequestMapping("/api/clientes/direcciones")
public class DireccionController {

    private final DireccionCatalogoService direccionCatalogoService;

    public DireccionController(DireccionCatalogoService direccionCatalogoService) {
        this.direccionCatalogoService = direccionCatalogoService;
    }

    @GetMapping("/estados")
    public List<String> estados() {
        return direccionCatalogoService.obtenerEstados();
    }

    @GetMapping("/municipios")
    public List<String> municipios(@RequestParam String estado) {
        return direccionCatalogoService.obtenerMunicipios(estado);
    }

    @GetMapping("/colonias")
    public Map<String, String> colonias(@RequestParam String estado, @RequestParam String municipio) {
        return direccionCatalogoService.obtenerColonias(estado, municipio);
    }
}
