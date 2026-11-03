package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Cliente;

import java.time.LocalDateTime;

/** Cliente dado de alta por personal (no autorregistrado) que todavia no puede iniciar sesion. */
public record ClienteSinAccesoResponse(Long id, String nombreCompleto, String email, LocalDateTime creadoEn) {
    public static ClienteSinAccesoResponse desde(Cliente cliente) {
        return new ClienteSinAccesoResponse(cliente.getId(), cliente.getNombreCompleto(), cliente.getEmail(), cliente.getCreadoEn());
    }
}
