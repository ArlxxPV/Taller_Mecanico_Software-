package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.CambiarRolUsuarioRequest;
import com.tallerpro.backend.dto.UsuarioRequest;
import com.tallerpro.backend.dto.UsuarioResponse;
import com.tallerpro.backend.model.Cliente;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.ClienteRepository;
import com.tallerpro.backend.repository.UsuarioRepository;
import com.tallerpro.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioService = usuarioService;
    }

    /**
     * Devuelve el perfil del usuario autenticado (segun el JWT). El frontend
     * la usa justo despues del login y al recargar el dashboard para saber
     * quien esta conectado, con que rol, sus permisos, su taller
     * (RECEPCIONISTA) y su estatus (CLIENTE).
     */
    @GetMapping("/yo")
    public UsuarioResponse yo(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow();
        Cliente clienteVinculado = clienteRepository.findByUsuario_Id(usuario.getId()).orElse(null);
        return UsuarioResponse.desde(usuario, clienteVinculado);
    }

    /** Todas las cuentas del sistema (incluidas las de rol CLIENTE) - ver SecurityConfig: USUARIOS_VER o USUARIOS_CREAR. */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /** Alta de una cuenta de personal con un rol ya creado - ver SecurityConfig: USUARIOS_CREAR. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest datos, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(datos, actor));
    }

    /** Reasigna el rol (y taller si aplica) de una cuenta de personal ya existente. */
    @PutMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolUsuarioRequest datos, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return usuarioService.cambiarRol(id, datos, actor);
    }
}
