package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DireccionRequest(

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 150, message = "La calle no debe superar 150 caracteres")
        String calle,

        @NotBlank(message = "La colonia es obligatoria")
        @Size(max = 100, message = "La colonia no debe superar 100 caracteres")
        String colonia,

        @NotBlank(message = "El municipio es obligatorio")
        @Size(max = 100, message = "El municipio no debe superar 100 caracteres")
        String municipio,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 100, message = "El estado no debe superar 100 caracteres")
        String estado,

        @NotBlank(message = "El codigo postal es obligatorio")
        @Pattern(regexp = "^\\d{5}$", message = "El codigo postal debe tener 5 digitos")
        String codigoPostal
) {
}
