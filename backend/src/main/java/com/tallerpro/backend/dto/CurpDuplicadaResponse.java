package com.tallerpro.backend.dto;

public record CurpDuplicadaResponse(boolean existe, Long clienteId, String nombreCompleto) {
    public static CurpDuplicadaResponse ninguno() {
        return new CurpDuplicadaResponse(false, null, null);
    }
}
