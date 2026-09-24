package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.UsuarioResponse;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Devuelve el perfil del usuario autenticado (segun el JWT). El frontend
     * la usa justo despues del login y al recargar el dashboard para saber
     * quien esta conectado y con que rol.
     */
    @GetMapping("/yo")
    public UsuarioResponse yo(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow();
        return UsuarioResponse.desde(usuario);
    }
}
