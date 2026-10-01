-- Fase 2: registro manual de clientes por ADMIN/RECEPCIONISTA.
-- Hasta ahora `clientes` solo se llenaba automaticamente cuando alguien se
-- auto-registraba (siempre con un `usuario_id`). A partir de aqui tambien se
-- puede dar de alta un cliente "de mostrador" sin que tenga cuenta propia,
-- por eso `usuario_id` deja de ser obligatorio.
ALTER TABLE clientes
    MODIFY usuario_id BIGINT NULL,
    -- Antes el nombre se leia siempre de `usuarios.nombre_completo`. Ahora que
    -- un cliente puede no tener cuenta, su nombre vive tambien aqui.
    ADD COLUMN nombre_completo VARCHAR(120) NULL AFTER usuario_id,
    ADD COLUMN contacto_alternativo VARCHAR(150) NULL AFTER nombre_completo,
    ADD COLUMN edad INT NULL,
    ADD COLUMN fecha_nacimiento DATE NULL,
    ADD COLUMN telefono_personal VARCHAR(20) NULL,
    ADD COLUMN telefono_trabajo VARCHAR(20) NULL,
    ADD COLUMN email VARCHAR(150) NULL,
    ADD COLUMN email_trabajo VARCHAR(150) NULL,
    ADD COLUMN foto_url VARCHAR(255) NULL,
    ADD COLUMN calle VARCHAR(150) NULL,
    ADD COLUMN colonia VARCHAR(100) NULL,
    ADD COLUMN municipio VARCHAR(100) NULL,
    ADD COLUMN estado_direccion VARCHAR(100) NULL,
    ADD COLUMN codigo_postal VARCHAR(10) NULL;

-- Evita registrar dos veces al mismo cliente por correo (MySQL permite
-- muchos NULL en una columna UNIQUE, asi que no afecta a los clientes viejos
-- que todavia no tienen este campo lleno).
CREATE UNIQUE INDEX uq_clientes_email ON clientes (email);

-- A los clientes que ya existian (auto-registrados) se les copia el nombre
-- y correo desde su cuenta, para que no queden en blanco.
UPDATE clientes c
JOIN usuarios u ON u.id = c.usuario_id
SET c.nombre_completo = u.nombre_completo,
    c.email = u.email
WHERE c.usuario_id IS NOT NULL;
