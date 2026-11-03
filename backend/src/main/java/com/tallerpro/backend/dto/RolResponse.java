package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Permiso;
import com.tallerpro.backend.model.Rol;

import java.util.Set;
import java.util.stream.Collectors;

public record RolResponse(Long id, String nombre, String descripcion, boolean esSistema, boolean activo, Set<String> permisos) {
    public static RolResponse desde(Rol rol) {
        return new RolResponse(
                rol.getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.isEsSistema(),
                rol.isActivo(),
                rol.getPermisos().stream().map(Permiso::getClave).collect(Collectors.toSet())
        );
    }
}
