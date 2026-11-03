package com.tallerpro.backend.dto;

public record PanelResumenResponse(
        long clientesActivos,
        long clientesSuspendidos,
        long vehiculosRegistrados,
        long clientesNuevosEsteMes
) {
}
