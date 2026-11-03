package com.tallerpro.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de una cuenta de personal (no clientes). tallerId no lleva @NotNull:
 * su obligatoriedad depende del rol elegido (ver UsuarioService), una regla
 * condicional que Jakarta Validation no puede expresar declarativamente.
 */
public record UsuarioRequest(

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 60, message = "Los nombres no deben superar 60 caracteres")
        String nombres,

        @NotBlank(message = "El apellido paterno es obligatorio")
        @Size(max = 60, message = "El apellido paterno no debe superar 60 caracteres")
        String apellidoPaterno,

        @NotBlank(message = "El apellido materno es obligatorio")
        @Size(max = 60, message = "El apellido materno no debe superar 60 caracteres")
        String apellidoMaterno,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contrasena debe tener minimo 8 caracteres, una mayuscula, una minuscula y un numero"
        )
        String password,

        @NotNull(message = "El rol es obligatorio")
        Long rolId,

        Long tallerId
) {
}
