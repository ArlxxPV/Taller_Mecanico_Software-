package com.tallerpro.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime momento,
        int status,
        String mensaje,
        List<String> detalles
) {
    public static ApiErrorResponse de(int status, String mensaje, List<String> detalles) {
        return new ApiErrorResponse(LocalDateTime.now(), status, mensaje, detalles);
    }
}
