package com.tallerpro.backend.security;

import com.tallerpro.backend.model.ClavePermiso;
import com.tallerpro.backend.model.Rol;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public AppUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese correo"));

        List<GrantedAuthority> authorities = new ArrayList<>();
        usuario.getRoles().forEach(rol -> authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre())));

        // Un rol "de sistema" (solo ADMIN) siempre tiene acceso total, sin
        // importar lo que haya guardado en rol_permisos (ver PermisoService).
        boolean esSistema = usuario.getRoles().stream().anyMatch(Rol::isEsSistema);
        if (esSistema) {
            Arrays.stream(ClavePermiso.values())
                    .forEach(clave -> authorities.add(new SimpleGrantedAuthority("PERM_" + clave.name())));
        } else {
            usuario.getRoles().stream()
                    .filter(Rol::isActivo)
                    .flatMap(rol -> rol.getPermisos().stream())
                    .map(permiso -> "PERM_" + permiso.getClave())
                    .distinct()
                    .forEach(permiso -> authorities.add(new SimpleGrantedAuthority(permiso)));
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(usuario.getEstado() == com.tallerpro.backend.model.EstadoUsuario.INACTIVO)
                .accountLocked(usuario.estaBloqueado())
                .build();
    }
}
