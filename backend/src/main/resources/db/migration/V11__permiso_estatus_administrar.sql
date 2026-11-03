-- Nuevo permiso para el catalogo de estatus (ver ClavePermiso.ESTATUS_ADMINISTRAR).
-- Se le da el mismo trato que los permisos de V8: ADMIN lo recibe de forma
-- explicita en rol_permisos (ademas del bypass por es_sistema), para que la
-- pantalla de roles lo muestre marcado igual que el resto de sus permisos.
INSERT INTO permisos (clave, descripcion) VALUES
    ('ESTATUS_ADMINISTRAR', 'Crear, editar, dar de baja y restaurar estatus del catalogo');

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p ON p.clave = 'ESTATUS_ADMINISTRAR'
WHERE r.nombre = 'ADMIN';
