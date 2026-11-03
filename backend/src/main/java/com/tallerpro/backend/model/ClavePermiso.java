package com.tallerpro.backend.model;

/**
 * Catalogo fijo de capacidades que el sistema sabe proteger. Lo configurable
 * es que ROLES tienen cada permiso (tabla rol_permisos), no este catalogo en
 * si: el codigo necesita poder preguntar "¿tiene tal permiso?" en sitios
 * concretos (SecurityConfig, ClienteService, etc.), asi que el conjunto de
 * capacidades posibles no puede ser arbitrario/inventado por un admin.
 */
public enum ClavePermiso {
    CLIENTES_VER,
    CLIENTES_CREAR,
    CLIENTES_EDITAR,
    CLIENTES_SUSPENDER,
    CLIENTES_TODOS_TALLERES,
    TALLERES_VER,
    TALLERES_CREAR,
    TALLERES_EDITAR,
    USUARIOS_VER,
    USUARIOS_CREAR,
    ROLES_ADMINISTRAR,
    ESTATUS_ADMINISTRAR,
    CLIENTES_ELIMINAR
}
