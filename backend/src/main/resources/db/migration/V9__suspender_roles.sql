-- Permite suspender un rol (deja de otorgar permisos sin borrarlo). ADMIN
-- (es_sistema=true) nunca se puede suspender, igual que no se pueden editar
-- sus permisos (ver RolService.cambiarEstado).
ALTER TABLE roles
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
