package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Usuario;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UsuarioResponse(
        Long id,
        String nombreCompleto,
        String email,
        Set<String> roles,
        LocalDateTime ultimoLoginEn,
        String ultimoLoginIp
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getRoles().stream().map(r -> r.getNombre().name()).collect(Collectors.toSet()),
                usuario.getUltimoLoginEn(),
                usuario.getUltimoLoginIp()
        );
    }
}
