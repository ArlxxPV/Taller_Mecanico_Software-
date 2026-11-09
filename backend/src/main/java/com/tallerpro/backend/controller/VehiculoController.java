package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.CambiarClienteVehiculoRequest;
import com.tallerpro.backend.dto.ClienteResponse;
import com.tallerpro.backend.dto.DisponibilidadResponse;
import com.tallerpro.backend.dto.HistorialEstatusVehiculoResponse;
import com.tallerpro.backend.dto.PaginaVehiculosResponse;
import com.tallerpro.backend.dto.VehiculoRequest;
import com.tallerpro.backend.dto.VehiculoResponse;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.UsuarioRepository;
import com.tallerpro.backend.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta y edicion (el registro se hace desde el cliente, en
 * ClienteController). Mismo aislamiento por taller que clientes.
 */
@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final UsuarioRepository usuarioRepository;

    public VehiculoController(VehiculoService vehiculoService, UsuarioRepository usuarioRepository) {
        this.vehiculoService = vehiculoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public PaginaVehiculosResponse listar(
            @RequestParam(required = false) Long estatusId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int porPagina,
            Authentication authentication
    ) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.listar(actor, estatusId, q, pagina, porPagina);
    }

    @GetMapping("/verificar-vin")
    public DisponibilidadResponse verificarVin(@RequestParam String vin) {
        return vehiculoService.verificarVin(vin);
    }

    @GetMapping("/verificar-placa")
    public DisponibilidadResponse verificarPlaca(@RequestParam String placa) {
        return vehiculoService.verificarPlaca(placa);
    }

    @GetMapping("/{id}")
    public VehiculoResponse obtener(@PathVariable Long id, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.obtener(id, actor);
    }

    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest datos, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.actualizar(id, datos, actor);
    }

    /** UC-CV-14: cliente vigente de este vehiculo. */
    @GetMapping("/{id}/cliente")
    public ClienteResponse obtenerCliente(@PathVariable Long id, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.obtenerClienteVigente(id, actor);
    }

    /** UC-CV-12: cambiar el cliente al que esta asociado este vehiculo (R2/R3: ver VehiculoService.cambiarPropietario). */
    @PatchMapping("/{id}/cliente")
    public VehiculoResponse cambiarCliente(@PathVariable Long id, @Valid @RequestBody CambiarClienteVehiculoRequest datos, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.cambiarPropietario(id, datos, actor);
    }

    /** El usuario ya confirmo en el frontend que de verdad quiere desactivarlo; aqui solo se aplica y se audita. */
    @PatchMapping("/{id}/desactivar")
    public VehiculoResponse desactivar(@PathVariable Long id, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.desactivar(id, actor);
    }

    @PatchMapping("/{id}/activar")
    public VehiculoResponse activar(@PathVariable Long id, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.activar(id, actor);
    }

    /** Historial de auditoria del vehiculo: altas, ediciones, activaciones/desactivaciones. */
    @GetMapping("/{id}/historial")
    public List<HistorialEstatusVehiculoResponse> historial(@PathVariable Long id, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return vehiculoService.historial(id, actor);
    }
}
