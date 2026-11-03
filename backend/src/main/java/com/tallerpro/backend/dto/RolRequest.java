package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RolRequest(

        @NotBlank(message = "El nombre del rol es obligatorio")
        @Size(max = 50, message = "El nombre del rol no debe superar 50 caracteres")
        String nombre,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 150, message = "La descripcion no debe superar 150 caracteres")
        String descripcion,

        @NotEmpty(message = "Selecciona al menos un permiso")
        Set<String> permisos
) {
}
