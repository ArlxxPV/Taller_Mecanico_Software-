package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Permiso;

public record PermisoResponse(String clave, String descripcion) {
    public static PermisoResponse desde(Permiso permiso) {
        return new PermisoResponse(permiso.getClave(), permiso.getDescripcion());
    }
}
