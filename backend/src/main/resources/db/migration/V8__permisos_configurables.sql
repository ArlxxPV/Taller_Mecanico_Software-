-- Reemplaza el enum Java NombreRol por un catalogo de permisos configurable.
-- La tabla roles ya era VARCHAR(30) libre desde V1 (comentario original:
-- "deja abierta la puerta a roles mas finos... sin tocar el esquema"); esta
-- migracion explota esa puerta: agrega el catalogo fijo de permisos, la
-- relacion N:M rol<->permiso, y el flag de "rol de sistema" que protege a
-- ADMIN de perder acceso por error humano o por diseño.

CREATE TABLE permisos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    clave       VARCHAR(50)  NOT NULL UNIQUE,
    descripcion VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permisos (clave, descripcion) VALUES
    ('CLIENTES_VER',            'Ver el listado y el detalle de clientes'),
    ('CLIENTES_CREAR',          'Registrar clientes nuevos'),
    ('CLIENTES_EDITAR',         'Editar los datos de un cliente existente'),
    ('CLIENTES_SUSPENDER',      'Suspender o reactivar el estatus de un cliente'),
    ('CLIENTES_TODOS_TALLERES', 'Ver y administrar clientes de cualquier taller, no solo el propio'),
    ('TALLERES_VER',            'Ver el listado y el detalle de talleres'),
    ('TALLERES_CREAR',          'Registrar talleres nuevos'),
    ('TALLERES_EDITAR',         'Editar los datos de un taller existente'),
    ('USUARIOS_VER',            'Ver el listado de cuentas de personal'),
    ('USUARIOS_CREAR',          'Crear cuentas de personal (no clientes)'),
    ('ROLES_ADMINISTRAR',       'Crear roles y configurar sus permisos');

CREATE TABLE rol_permisos (
    rol_id     BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permisos_rol FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permisos_permiso FOREIGN KEY (permiso_id) REFERENCES permisos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- es_sistema: solo ADMIN. Marca el rol como "acceso total siempre", protegido
-- de perder permisos por accidente o por diseño (ver RolService.actualizarPermisos,
-- rechaza con 409 cualquier intento de editar un rol con es_sistema=true).
ALTER TABLE roles
    ADD COLUMN es_sistema BOOLEAN NOT NULL DEFAULT FALSE;

-- nombre ya era VARCHAR(30) libre; se ensancha a 50 para nombres de rol mas
-- descriptivos que los 4 originales (ej. "Supervisor de taller").
ALTER TABLE roles
    MODIFY nombre VARCHAR(50) NOT NULL;

UPDATE roles SET es_sistema = TRUE WHERE nombre = 'ADMIN';

-- Backfill: cada rol recibe exactamente los permisos que ya tiene HOY segun
-- las reglas vigentes de SecurityConfig/ClienteService, para que la pantalla
-- nueva los muestre desde el primer momento sin reconfigurar nada a mano.
-- RECEPCIONISTA: alta/edicion/consulta de clientes de su taller + consulta de
-- talleres (para su propio select). No incluye CLIENTES_SUSPENDER (hoy es
-- ADMIN-only) ni CLIENTES_TODOS_TALLERES (hoy esta aislado por taller).
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r
JOIN permisos p ON p.clave IN ('CLIENTES_VER','CLIENTES_CREAR','CLIENTES_EDITAR','TALLERES_VER')
WHERE r.nombre = 'RECEPCIONISTA';

-- ADMIN: todos los permisos explicitos (ademas del bypass por es_sistema),
-- para que la pantalla de roles lo muestre con todas las casillas marcadas.
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p WHERE r.nombre = 'ADMIN';

-- EMPLEADO y CLIENTE quedan sin filas (su comportamiento real hoy: EMPLEADO
-- no tiene ningun endpoint exclusivo, CLIENTE es autoservicio puro).

-- origen_registro de clientes: con roles dinamicos ya no hay un conjunto fijo
-- de "quien pudo haber registrado". Se guarda el nombre real del rol del
-- actor (o 'AUTORREGISTRO'). Cambio no-destructivo: los valores actuales
-- ('ADMIN','RECEPCIONISTA','AUTORREGISTRO') ya son strings validos.
ALTER TABLE clientes
    MODIFY origen_registro VARCHAR(100) NOT NULL;
