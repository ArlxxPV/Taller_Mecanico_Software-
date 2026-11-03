package com.tallerpro.backend.dto;

import jakarta.validation.constraints.Size;

public record ReactivarClienteRequest(
        @Size(max = 255, message = "El comentario no debe superar 255 caracteres")
        String comentario
) {
}
