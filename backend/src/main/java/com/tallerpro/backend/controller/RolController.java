package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.ActualizarPermisosRolRequest;
import com.tallerpro.backend.dto.CambiarEstadoRolRequest;
import com.tallerpro.backend.dto.PermisoResponse;
import com.tallerpro.backend.dto.RolRequest;
import com.tallerpro.backend.dto.RolResponse;
import com.tallerpro.backend.service.RolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogo de roles y sus permisos (ver SecurityConfig: listar es accesible
 * para ROLES_ADMINISTRAR o USUARIOS_CREAR; crear/editar permisos es exclusivo
 * de ROLES_ADMINISTRAR).
 */
@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public List<RolResponse> listar() {
        return rolService.listar();
    }

    @GetMapping("/permisos-disponibles")
    public List<PermisoResponse> permisosDisponibles() {
        return rolService.permisosDisponibles();
    }

    @PostMapping
    public ResponseEntity<RolResponse> crear(@Valid @RequestBody RolRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(datos));
    }

    @PutMapping("/{id}/permisos")
    public RolResponse actualizarPermisos(@PathVariable Long id, @Valid @RequestBody ActualizarPermisosRolRequest datos) {
        return rolService.actualizarPermisos(id, datos);
    }

    @PatchMapping("/{id}/estado")
    public RolResponse cambiarEstado(@PathVariable Long id, @RequestBody CambiarEstadoRolRequest datos) {
        return rolService.cambiarEstado(id, datos.activo());
    }
}
