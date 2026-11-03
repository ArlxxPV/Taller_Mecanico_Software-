package com.tallerpro.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Datos del formulario de registro manual de clientes (fase 2), llenado por
 * un ADMIN o RECEPCIONISTA. Viaja como la parte "datos" (JSON) de una
 * peticion multipart/form-data; la foto viaja aparte (ver ClienteController).
 * La edad NO viaja aqui: el backend la calcula siempre desde fechaNacimiento.
 */
public record ClienteRequest(

        @NotBlank(message = "El nombre(s) es obligatorio")
        @Size(max = 60, message = "El nombre no debe superar 60 caracteres")
        String nombres,

        @NotBlank(message = "El apellido paterno es obligatorio")
        @Size(max = 60, message = "El apellido paterno no debe superar 60 caracteres")
        String apellidoPaterno,

        @NotBlank(message = "El apellido materno es obligatorio")
        @Size(max = 60, message = "El apellido materno no debe superar 60 caracteres")
        String apellidoMaterno,

        @NotBlank(message = "La CURP es obligatoria")
        @Pattern(
                regexp = "^[A-Z][AEIOU][A-Z]{2}\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])[HM](?:AS|BC|BS|CC|CS|CH|CL|CM|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QO|QR|SP|SL|SR|TC|TL|TS|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$",
                message = "La CURP no tiene un formato valido"
        )
        String curp,

        @NotBlank(message = "El contacto alternativo es obligatorio")
        @Size(max = 150, message = "El contacto alternativo no debe superar 150 caracteres")
        String contactoAlternativo,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El telefono personal es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El telefono personal debe tener exactamente 10 digitos")
        String telefonoPersonal,

        @NotBlank(message = "El telefono de trabajo es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El telefono de trabajo debe tener exactamente 10 digitos")
        String telefonoTrabajo,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String email,

        @NotBlank(message = "El correo de trabajo es obligatorio")
        @Email(message = "El correo de trabajo no tiene un formato valido")
        String emailTrabajo,

        @NotNull(message = "La direccion es obligatoria")
        @Valid
        DireccionRequest direccion,

        @NotNull(message = "El taller es obligatorio")
        Long tallerId
) {
}
