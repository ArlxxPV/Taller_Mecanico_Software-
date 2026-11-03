package com.tallerpro.backend.service;

import com.tallerpro.backend.model.ClavePermiso;
import com.tallerpro.backend.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class PermisoService {

    /**
     * Un rol con esSistema (solo ADMIN) siempre tiene acceso total, sin
     * importar lo que haya guardado en rol_permisos - es el bypass que
     * garantiza que ADMIN nunca pierda permisos por accidente ni por diseño.
     * Un rol suspendido (activo=false) no otorga ningun permiso.
     */
    public boolean tiene(Usuario actor, ClavePermiso clave) {
        return actor.getRoles().stream().anyMatch(rol ->
                rol.isEsSistema() ||
                        (rol.isActivo() && rol.getPermisos().stream().anyMatch(p -> p.getClave().equals(clave.name()))));
    }
}
