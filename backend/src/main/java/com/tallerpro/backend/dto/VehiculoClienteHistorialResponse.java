package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.RelacionClienteVehiculo;

import java.time.LocalDateTime;

/** UC-CV-13: vehiculos de un cliente, vigentes e historicos. */
public record VehiculoClienteHistorialResponse(
        VehiculoResponse vehiculo,
        boolean vigente,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String motivo
) {
    public static VehiculoClienteHistorialResponse desde(RelacionClienteVehiculo relacion) {
        return new VehiculoClienteHistorialResponse(
                VehiculoResponse.desde(relacion.getVehiculo()),
                relacion.isVigente(),
                relacion.getFechaInicio(),
                relacion.getFechaFin(),
                relacion.getMotivo()
        );
    }
}
