package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AsignarAccesoRequest(
        @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contrasena debe tener minimo 8 caracteres, una mayuscula, una minuscula y un numero"
        )
        String password
) {
}
