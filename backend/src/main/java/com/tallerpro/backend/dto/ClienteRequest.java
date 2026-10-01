package com.tallerpro.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Datos del formulario de registro manual de clientes (fase 2), llenado por
 * un ADMIN o RECEPCIONISTA. Viaja como la parte "datos" (JSON) de una
 * peticion multipart/form-data; la foto viaja aparte (ver ClienteController).
 */
public record ClienteRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombreCompleto,

        @Size(max = 150, message = "El contacto alternativo no debe superar 150 caracteres")
        String contactoAlternativo,

        @NotNull(message = "La edad es obligatoria")
        @Min(value = 0, message = "La edad no puede ser negativa")
        @Max(value = 120, message = "La edad no es valida")
        Integer edad,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El telefono personal es obligatorio")
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "El telefono personal no tiene un formato valido")
        String telefonoPersonal,

        @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "El telefono de trabajo no tiene un formato valido")
        String telefonoTrabajo,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String email,

        @Email(message = "El correo de trabajo no tiene un formato valido")
        String emailTrabajo,

        @NotNull(message = "La direccion es obligatoria")
        @Valid
        DireccionRequest direccion
) {
}
