package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.HistorialEstatusCliente;

import java.time.LocalDateTime;

/**
 * "cambios" viaja como el JSON crudo guardado en la fila (o null si no es un
 * movimiento ACTUALIZADO): el frontend lo parsea para mostrar "campo:
 * anterior -> nuevo" (ver maqueta, pestana Historial). "usuarioNombre" nulo
 * = fila de alta migrada o autorregistro (ver V12/AuthService); el frontend
 * cae de regreso a Cliente.origenRegistro en ese caso.
 */
public record HistorialEstatusClienteResponse(
        Long id,
        String estatusAnterior,
        String estatusNuevo,
        String motivo,
        String cambios,
        String usuarioNombre,
        LocalDateTime creadoEn
) {
    public static HistorialEstatusClienteResponse desde(HistorialEstatusCliente h) {
        return new HistorialEstatusClienteResponse(
                h.getId(),
                h.getEstatusAnterior() != null ? h.getEstatusAnterior().getValor() : null,
                h.getEstatusNuevo().getValor(),
                h.getMotivo(),
                h.getCambios(),
                h.getUsuario() != null ? h.getUsuario().getNombreCompleto() : null,
                h.getCreadoEn()
        );
    }
}
