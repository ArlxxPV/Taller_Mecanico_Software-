package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.MovimientoPanelResponse;
import com.tallerpro.backend.dto.PanelResumenResponse;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.UsuarioRepository;
import com.tallerpro.backend.service.PanelService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo para quien ve clientes (ver SecurityConfig): el panel simple de CLIENTE no llama esto. */
@RestController
@RequestMapping("/api/panel")
public class PanelController {

    private final PanelService panelService;
    private final UsuarioRepository usuarioRepository;

    public PanelController(PanelService panelService, UsuarioRepository usuarioRepository) {
        this.panelService = panelService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/resumen")
    public PanelResumenResponse resumen(Authentication authentication) {
        return panelService.resumen(actorAutenticado(authentication));
    }

    @GetMapping("/ultimos-movimientos")
    public List<MovimientoPanelResponse> ultimosMovimientos(Authentication authentication) {
        return panelService.ultimosMovimientos(actorAutenticado(authentication));
    }

    private Usuario actorAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
    }
}
