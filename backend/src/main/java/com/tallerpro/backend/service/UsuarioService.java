package com.tallerpro.backend.service;

import com.tallerpro.backend.dto.CambiarRolUsuarioRequest;
import com.tallerpro.backend.dto.UsuarioRequest;
import com.tallerpro.backend.dto.UsuarioResponse;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.ClavePermiso;
import com.tallerpro.backend.model.Rol;
import com.tallerpro.backend.model.Taller;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.RolRepository;
import com.tallerpro.backend.repository.TallerRepository;
import com.tallerpro.backend.repository.UsuarioRepository;
import com.tallerpro.backend.util.NormalizadorTexto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Alta, consulta y cambio de rol de cuentas de personal (no clientes) - ver registro_Fase02.md. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final TallerRepository tallerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermisoService permisoService;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           TallerRepository tallerRepository, PasswordEncoder passwordEncoder,
                           PermisoService permisoService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.tallerRepository = tallerRepository;
        this.passwordEncoder = passwordEncoder;
        this.permisoService = permisoService;
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest datos, Usuario actor) {
        if (usuarioRepository.existsByEmailIgnoreCase(datos.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }

        Rol rol = resolverRolAsignable(datos.rolId(), actor);
        Taller taller = resolverTallerSiNecesario(rol, datos.tallerId());

        String nombres = NormalizadorTexto.aTitulo(datos.nombres());
        String apellidoPaterno = NormalizadorTexto.aTitulo(datos.apellidoPaterno());
        String apellidoMaterno = NormalizadorTexto.aTitulo(datos.apellidoMaterno());

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombres + " " + apellidoPaterno + " " + apellidoMaterno);
        usuario.setEmail(NormalizadorTexto.aMinusculas(datos.email()));
        usuario.setPasswordHash(passwordEncoder.encode(datos.password()));
        usuario.setRoles(new HashSet<>(Set.of(rol)));
        usuario.setTaller(taller);
        usuario = usuarioRepository.save(usuario);

        return UsuarioResponse.desde(usuario);
    }

    /** Reasigna el rol (y taller si aplica) de una cuenta de personal ya existente. */
    @Transactional
    public UsuarioResponse cambiarRol(Long id, CambiarRolUsuarioRequest datos, Usuario actor) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro la cuenta solicitada."));

        Rol rol = resolverRolAsignable(datos.rolId(), actor);
        Taller taller = resolverTallerSiNecesario(rol, datos.tallerId());

        usuario.setRoles(new HashSet<>(Set.of(rol)));
        usuario.setTaller(taller);
        usuario = usuarioRepository.save(usuario);

        return UsuarioResponse.desde(usuario);
    }

    /**
     * Todas las cuentas del sistema, incluidas las de rol CLIENTE (p. ej. un
     * cliente autorregistrado o uno al que se le asigno acceso): tambien se
     * les puede cambiar el rol desde Personal (ver cambiarRol), no solo a las
     * cuentas de personal originalmente creadas aqui.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    private Rol resolverRolAsignable(Long rolId, Usuario actor) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El rol seleccionado no es valido."));

        if (!rol.isActivo()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Este rol esta suspendido y no se puede asignar.");
        }

        // Evita auto-escalacion: asignar un rol de acceso total (es_sistema)
        // requiere poder administrar roles, no solo crear/editar personal.
        if (rol.isEsSistema() && !permisoService.tiene(actor, ClavePermiso.ROLES_ADMINISTRAR)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes asignar un rol de administrador total.");
        }

        return rol;
    }

    private Taller resolverTallerSiNecesario(Rol rol, Long tallerId) {
        if (!necesitaTaller(rol)) {
            return null;
        }
        if (tallerId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El taller es obligatorio para este rol.");
        }
        return tallerRepository.findById(tallerId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El taller seleccionado no es valido."));
    }

    private boolean necesitaTaller(Rol rol) {
        boolean tieneTodosTalleres = tienePermiso(rol, ClavePermiso.CLIENTES_TODOS_TALLERES);
        boolean operaClientes = tienePermiso(rol, ClavePermiso.CLIENTES_VER)
                || tienePermiso(rol, ClavePermiso.CLIENTES_CREAR)
                || tienePermiso(rol, ClavePermiso.CLIENTES_EDITAR);
        return !tieneTodosTalleres && operaClientes;
    }

    private boolean tienePermiso(Rol rol, ClavePermiso clave) {
        return rol.getPermisos().stream().anyMatch(p -> p.getClave().equals(clave.name()));
    }
}
