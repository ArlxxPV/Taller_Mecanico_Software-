package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.EstatusRequest;
import com.tallerpro.backend.dto.EstatusResponse;
import com.tallerpro.backend.service.EstatusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lectura (GET) accesible a quien trabaje con clientes (filtros de la lista,
 * ver SecurityConfig); crear/editar/dar de baja/restaurar es solo admin
 * (PERM_ESTATUS_ADMINISTRAR).
 */
@RestController
@RequestMapping("/api/estatus")
public class EstatusController {

    private final EstatusService estatusService;

    public EstatusController(EstatusService estatusService) {
        this.estatusService = estatusService;
    }

    @GetMapping
    public List<EstatusResponse> listar(@RequestParam(required = false, defaultValue = "false") boolean incluirBajas) {
        return estatusService.listar(incluirBajas);
    }

    @GetMapping("/{id}")
    public EstatusResponse obtener(@PathVariable Long id) {
        return estatusService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EstatusResponse> crear(@Valid @RequestBody EstatusRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estatusService.crear(datos));
    }

    @PutMapping("/{id}")
    public EstatusResponse actualizar(@PathVariable Long id, @Valid @RequestBody EstatusRequest datos) {
        return estatusService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        estatusService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    public ResponseEntity<Void> restaurar(@PathVariable Long id) {
        estatusService.restaurar(id);
        return ResponseEntity.noContent().build();
    }
}
