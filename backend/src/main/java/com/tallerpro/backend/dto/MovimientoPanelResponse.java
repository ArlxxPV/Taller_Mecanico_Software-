package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.HistorialEstatusCliente;

import java.time.LocalDateTime;

public record MovimientoPanelResponse(
        Long clienteId,
        String clienteNombre,
        String movimiento,
        String detalle,
        String cambios,
        String usuarioNombre,
        String origenRegistro,
        LocalDateTime fecha
) {
    public static MovimientoPanelResponse desde(HistorialEstatusCliente h) {
        return new MovimientoPanelResponse(
                h.getCliente().getId(),
                h.getCliente().getNombreCompleto(),
                h.getEstatusNuevo().getValor(),
                h.getMotivo(),
                h.getCambios(),
                // Nulo (alta migrada/autorregistro, ver V12/AuthService): el
                // frontend cae de regreso a origenRegistro.
                h.getUsuario() != null ? h.getUsuario().getNombreCompleto() : null,
                h.getCliente().getOrigenRegistro(),
                h.getCreadoEn()
        );
    }
}
