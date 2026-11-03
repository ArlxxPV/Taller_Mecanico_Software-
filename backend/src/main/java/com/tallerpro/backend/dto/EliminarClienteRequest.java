package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EliminarClienteRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 255, message = "El motivo debe tener entre 10 y 255 caracteres")
        String motivo
) {
}
