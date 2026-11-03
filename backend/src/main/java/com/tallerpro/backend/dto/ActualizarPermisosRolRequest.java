package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record ActualizarPermisosRolRequest(
        @NotEmpty(message = "Selecciona al menos un permiso")
        Set<String> permisos
) {
}
