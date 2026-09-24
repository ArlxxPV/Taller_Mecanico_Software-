package com.tallerpro.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que llegan desde el formulario de registro del frontend.
 * El registro publico siempre crea un usuario con rol CLIENTE; los roles
 * ADMIN/EMPLEADO se asignan desde una pantalla administrativa (fase futura),
 * nunca desde este endpoint abierto.
 */
public record RegistroRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String email,

        String telefono,

        @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contrasena debe tener minimo 8 caracteres, una mayuscula, una minuscula y un numero"
        )
        String password
) {
}
