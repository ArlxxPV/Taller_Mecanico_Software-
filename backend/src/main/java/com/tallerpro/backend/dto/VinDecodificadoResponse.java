package com.tallerpro.backend.dto;

public record VinDecodificadoResponse(String marca, String modelo, String anio, String version) {
    public static VinDecodificadoResponse vacio() {
        return new VinDecodificadoResponse(null, null, null, null);
    }
}
