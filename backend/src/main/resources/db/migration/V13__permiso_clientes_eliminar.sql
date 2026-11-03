-- Nuevo permiso para eliminar/restaurar clientes y ver los eliminados
-- (ver ClavePermiso.CLIENTES_ELIMINAR). Solo ADMIN, igual que roles/estatus.
INSERT INTO permisos (clave, descripcion) VALUES
    ('CLIENTES_ELIMINAR', 'Eliminar (baja logica), restaurar y ver clientes eliminados');

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p ON p.clave = 'CLIENTES_ELIMINAR'
WHERE r.nombre = 'ADMIN';

-- La maqueta de administracion de clientes pide explicitamente que
-- RECEPCIONISTA tambien pueda suspender/reactivar (antes solo lo tenia
-- ADMIN, vease el bypass es_sistema). No se quita nada, solo se agrega.
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p ON p.clave = 'CLIENTES_SUSPENDER'
WHERE r.nombre = 'RECEPCIONISTA'
  AND NOT EXISTS (
      SELECT 1 FROM rol_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id
  );
