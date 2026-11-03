package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Estatus;

import java.time.LocalDateTime;

public record EstatusResponse(
        Long id,
        String valor,
        String descripcion,
        boolean esSistema,
        boolean vigente,
        long clientesEnUso,
        long vehiculosEnUso,
        LocalDateTime actualizadoEn
) {
    public static EstatusResponse desde(Estatus estatus, long clientesEnUso, long vehiculosEnUso) {
        return new EstatusResponse(
                estatus.getId(),
                estatus.getValor(),
                estatus.getDescripcion(),
                estatus.isEsSistema(),
                estatus.getEliminadoEn() == null,
                clientesEnUso,
                vehiculosEnUso,
                estatus.getActualizadoEn()
        );
    }
}
