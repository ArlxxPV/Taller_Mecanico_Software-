package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.AsignarAccesoRequest;
import com.tallerpro.backend.dto.CambiarEstatusClienteRequest;
import com.tallerpro.backend.dto.ClienteRequest;
import com.tallerpro.backend.dto.ClienteResponse;
import com.tallerpro.backend.dto.ClienteSinAccesoResponse;
import com.tallerpro.backend.dto.CurpDuplicadaResponse;
import com.tallerpro.backend.dto.EliminarClienteRequest;
import com.tallerpro.backend.dto.HistorialEstatusClienteResponse;
import com.tallerpro.backend.dto.PaginaClientesResponse;
import com.tallerpro.backend.dto.ReactivarClienteRequest;
import com.tallerpro.backend.dto.ResumenClientesResponse;
import com.tallerpro.backend.dto.SuspenderClienteRequest;
import com.tallerpro.backend.dto.VehiculoClienteHistorialResponse;
import com.tallerpro.backend.dto.VehiculoRequest;
import com.tallerpro.backend.dto.VehiculoResponse;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.UsuarioRepository;
import com.tallerpro.backend.service.ClienteService;
import com.tallerpro.backend.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Solo accesible para ADMIN/RECEPCIONISTA (ver SecurityConfig), con
 * aislamiento por taller resuelto en ClienteService a partir del usuario
 * autenticado. Crear y actualizar viajan como multipart/form-data: la parte
 * "datos" es un JSON (ClienteRequest) y "foto" es el archivo, opcional.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final UsuarioRepository usuarioRepository;
    private final VehiculoService vehiculoService;

    public ClienteController(ClienteService clienteService, UsuarioRepository usuarioRepository, VehiculoService vehiculoService) {
        this.clienteService = clienteService;
        this.usuarioRepository = usuarioRepository;
        this.vehiculoService = vehiculoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClienteResponse> crear(
            @Valid @RequestPart("datos") ClienteRequest datos,
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(datos, foto, actorAutenticado(authentication)));
    }

    @GetMapping
    public PaginaClientesResponse listar(
            @RequestParam(required = false) Long tallerId,
            @RequestParam(required = false) Long estatusId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "creadoEn") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direccion,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int porPagina,
            @RequestParam(defaultValue = "false") boolean incluirEliminados,
            Authentication authentication
    ) {
        return clienteService.listar(actorAutenticado(authentication), tallerId, estatusId, q, ordenarPor, direccion, pagina, porPagina, incluirEliminados);
    }

    /** Conteos para las tarjetas de resumen (total/activos/suspendidos/eliminados) de la lista de clientes. */
    @GetMapping("/resumen")
    public ResumenClientesResponse resumen(@RequestParam(required = false) Long tallerId, Authentication authentication) {
        return clienteService.resumen(actorAutenticado(authentication), tallerId);
    }

    /** Chequeo "al salir del campo CURP" del formulario de registro (ver maqueta). */
    @GetMapping("/verificar-curp")
    public CurpDuplicadaResponse verificarCurp(@RequestParam String curp) {
        return clienteService.verificarCurp(curp);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id, Authentication authentication) {
        return clienteService.obtener(id, actorAutenticado(authentication));
    }

    @GetMapping("/{id}/historial")
    public List<HistorialEstatusClienteResponse> historial(@PathVariable Long id, Authentication authentication) {
        return clienteService.historial(id, actorAutenticado(authentication));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClienteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestPart("datos") ClienteRequest datos,
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            Authentication authentication
    ) {
        return clienteService.actualizar(id, datos, foto, actorAutenticado(authentication));
    }

    /** Endpoint anterior (toggle sin motivo) - se deja funcionando, la pantalla nueva ya no lo llama. */
    @PatchMapping("/{id}/estatus")
    public ClienteResponse cambiarEstatus(@PathVariable Long id, @Valid @RequestBody CambiarEstatusClienteRequest body) {
        return clienteService.cambiarEstatus(id, body.estadoCliente());
    }

    @PatchMapping("/{id}/suspender")
    public ClienteResponse suspender(@PathVariable Long id, @Valid @RequestBody SuspenderClienteRequest body, Authentication authentication) {
        return clienteService.suspender(id, body.motivo(), actorAutenticado(authentication));
    }

    @PatchMapping("/{id}/reactivar")
    public ClienteResponse reactivar(@PathVariable Long id, @RequestBody(required = false) ReactivarClienteRequest body, Authentication authentication) {
        String comentario = body != null ? body.comentario() : null;
        return clienteService.reactivar(id, comentario, actorAutenticado(authentication));
    }

    @DeleteMapping("/{id}")
    public ClienteResponse eliminar(@PathVariable Long id, @Valid @RequestBody EliminarClienteRequest body, Authentication authentication) {
        return clienteService.eliminar(id, body.motivo(), actorAutenticado(authentication));
    }

    @PatchMapping("/{id}/restaurar")
    public ClienteResponse restaurar(@PathVariable Long id, Authentication authentication) {
        return clienteService.restaurar(id, actorAutenticado(authentication));
    }

    @GetMapping("/{id}/vehiculos")
    public List<VehiculoClienteHistorialResponse> vehiculosDelCliente(@PathVariable Long id, Authentication authentication) {
        return vehiculoService.listarPorCliente(id, actorAutenticado(authentication));
    }

    /** Registra el vehiculo y lo asocia de una vez (ver maqueta "Asociar vehiculo"). Solo si el cliente esta ACTIVO. */
    @PostMapping("/{id}/vehiculos")
    public ResponseEntity<VehiculoResponse> asociarVehiculo(@PathVariable Long id, @Valid @RequestBody VehiculoRequest datos, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.crear(id, datos, actorAutenticado(authentication)));
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> obtenerFoto(@PathVariable Long id) {
        Path ruta = clienteService.obtenerRutaFoto(id);
        MediaType tipo = ruta.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).body(new FileSystemResource(ruta));
    }

    /** Clientes dados de alta por personal que todavia no pueden iniciar sesion - ver SecurityConfig: USUARIOS_CREAR. */
    @GetMapping("/sin-acceso")
    public List<ClienteSinAccesoResponse> listarSinAcceso() {
        return clienteService.listarSinAcceso();
    }

    /** Crea la cuenta que le faltaba a ese cliente, con la contrasena dada - ver SecurityConfig: USUARIOS_CREAR. */
    @PostMapping("/{id}/acceso")
    public ResponseEntity<Void> asignarAcceso(@PathVariable Long id, @Valid @RequestBody AsignarAccesoRequest datos) {
        clienteService.asignarAcceso(id, datos);
        return ResponseEntity.noContent().build();
    }

    private Usuario actorAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
    }
}
