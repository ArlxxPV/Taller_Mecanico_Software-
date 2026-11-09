package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.HistorialEstatusVehiculo;

import java.time.LocalDateTime;

/** Mismo criterio que HistorialEstatusClienteResponse: "cambios" solo viene lleno en movimientos ACTUALIZADO. */
public record HistorialEstatusVehiculoResponse(
        Long id,
        String estatusAnterior,
        String estatusNuevo,
        String motivo,
        String cambios,
        String usuarioNombre,
        LocalDateTime creadoEn
) {
    public static HistorialEstatusVehiculoResponse desde(HistorialEstatusVehiculo h) {
        return new HistorialEstatusVehiculoResponse(
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
