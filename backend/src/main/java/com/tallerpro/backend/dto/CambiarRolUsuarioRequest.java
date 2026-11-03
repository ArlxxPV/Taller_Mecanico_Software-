package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarRolUsuarioRequest(
        @NotNull(message = "El rol es obligatorio")
        Long rolId,
        Long tallerId
) {
}
