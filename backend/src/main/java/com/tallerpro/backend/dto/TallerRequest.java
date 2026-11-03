package com.tallerpro.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TallerRequest(

        @NotBlank(message = "El nombre del taller es obligatorio")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
        String nombre,

        @NotBlank(message = "La razon social es obligatoria")
        @Size(max = 150, message = "La razon social no debe superar 150 caracteres")
        String razonSocial,

        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El telefono debe tener exactamente 10 digitos")
        String telefono,

        @NotBlank(message = "El RFC es obligatorio")
        @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El RFC no tiene un formato valido")
        String rfc,

        @NotBlank(message = "El correo de contacto es obligatorio")
        @Email(message = "El correo de contacto no tiene un formato valido")
        String emailContacto,

        @NotNull(message = "La direccion es obligatoria")
        @Valid
        DireccionRequest direccion
) {
}
