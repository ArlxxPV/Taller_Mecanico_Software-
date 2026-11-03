package com.tallerpro.backend.service;

import com.tallerpro.backend.dto.ActualizarPermisosRolRequest;
import com.tallerpro.backend.dto.PermisoResponse;
import com.tallerpro.backend.dto.RolRequest;
import com.tallerpro.backend.dto.RolResponse;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.Permiso;
import com.tallerpro.backend.model.Rol;
import com.tallerpro.backend.repository.PermisoRepository;
import com.tallerpro.backend.repository.RolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        return rolRepository.findAll().stream().map(RolResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoResponse> permisosDisponibles() {
        return permisoRepository.findAll().stream().map(PermisoResponse::desde).toList();
    }

    @Transactional
    public RolResponse crear(RolRequest datos) {
        if (rolRepository.existsByNombreIgnoreCase(datos.nombre())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre.");
        }

        Rol rol = new Rol();
        rol.setNombre(datos.nombre());
        rol.setDescripcion(datos.descripcion());
        rol.setEsSistema(false);
        rol.setPermisos(resolverPermisos(datos.permisos()));

        rol = rolRepository.save(rol);
        return RolResponse.desde(rol);
    }

    @Transactional
    public RolResponse actualizarPermisos(Long id, ActualizarPermisosRolRequest datos) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el rol solicitado."));

        if (rol.isEsSistema()) {
            throw new ApiException(HttpStatus.CONFLICT, "El rol ADMIN siempre tiene acceso total y no puede editarse.");
        }

        rol.setPermisos(resolverPermisos(datos.permisos()));
        rol = rolRepository.save(rol);
        return RolResponse.desde(rol);
    }

    @Transactional
    public RolResponse cambiarEstado(Long id, boolean activo) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el rol solicitado."));

        if (rol.isEsSistema()) {
            throw new ApiException(HttpStatus.CONFLICT, "El rol ADMIN siempre tiene acceso total y no puede suspenderse.");
        }

        rol.setActivo(activo);
        rol = rolRepository.save(rol);
        return RolResponse.desde(rol);
    }

    private Set<Permiso> resolverPermisos(Set<String> claves) {
        Set<Permiso> permisos = new HashSet<>();
        for (String clave : claves) {
            Permiso permiso = permisoRepository.findByClave(clave)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Hay permisos no validos en la seleccion."));
            permisos.add(permiso);
        }
        return permisos;
    }
}
