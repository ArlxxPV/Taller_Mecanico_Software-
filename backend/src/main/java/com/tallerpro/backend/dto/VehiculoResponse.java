package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Vehiculo;

import java.time.LocalDateTime;

public record VehiculoResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        String vin,
        String numeroSerie,
        String placa,
        String marca,
        String modelo,
        String version,
        Integer anio,
        String color,
        Integer kilometraje,
        String estatus,
        LocalDateTime creadoEn
) {
    public static VehiculoResponse desde(Vehiculo v) {
        return new VehiculoResponse(
                v.getId(),
                v.getCliente().getId(),
                v.getCliente().getNombreCompleto(),
                v.getVin(),
                v.getNumeroSerie(),
                v.getPlaca(),
                v.getMarca(),
                v.getModelo(),
                v.getVersion(),
                (int) v.getAnio(),
                v.getColor(),
                v.getKilometraje(),
                v.getEstatus().getValor(),
                v.getCreadoEn()
        );
    }
}
