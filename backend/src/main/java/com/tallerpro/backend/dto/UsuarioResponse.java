package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.ClavePermiso;
import com.tallerpro.backend.model.Cliente;
import com.tallerpro.backend.model.Permiso;
import com.tallerpro.backend.model.Rol;
import com.tallerpro.backend.model.Usuario;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record UsuarioResponse(
        Long id,
        String nombreCompleto,
        String email,
        Set<String> roles,
        Set<String> permisos,
        LocalDateTime ultimoLoginEn,
        String ultimoLoginIp,
        Long tallerId,
        String tallerNombre,
        String estadoCliente
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return desde(usuario, null);
    }

    /**
     * clienteVinculado: el Cliente asociado a este Usuario, si tiene rol
     * CLIENTE (ver UsuarioController.yo). estadoCliente queda null si no
     * aplica (ADMIN, EMPLEADO, RECEPCIONISTA). tallerId es "mi taller": el
     * propio para RECEPCIONISTA (usuario.tallerId) o el de su cliente
     * vinculado si inicio sesion como CLIENTE.
     */
    public static UsuarioResponse desde(Usuario usuario, Cliente clienteVinculado) {
        Long tallerId = usuario.getTaller() != null
                ? usuario.getTaller().getId()
                : (clienteVinculado != null && clienteVinculado.getTaller() != null ? clienteVinculado.getTaller().getId() : null);
        String tallerNombre = usuario.getTaller() != null
                ? usuario.getTaller().getNombre()
                : (clienteVinculado != null && clienteVinculado.getTaller() != null ? clienteVinculado.getTaller().getNombre() : null);

        boolean esSistema = usuario.getRoles().stream().anyMatch(Rol::isEsSistema);
        Set<String> permisos = esSistema
                ? Arrays.stream(ClavePermiso.values()).map(Enum::name).collect(Collectors.toSet())
                : usuario.getRoles().stream()
                        .filter(Rol::isActivo)
                        .flatMap(r -> r.getPermisos().stream())
                        .map(Permiso::getClave)
                        .collect(Collectors.toSet());

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet()),
                permisos,
                usuario.getUltimoLoginEn(),
                usuario.getUltimoLoginIp(),
                tallerId,
                tallerNombre,
                clienteVinculado != null ? clienteVinculado.getEstadoCliente().name() : null
        );
    }
}
