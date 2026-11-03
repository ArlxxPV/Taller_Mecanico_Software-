-- Fase 2 (nueva, administracion de clientes/vehiculos con catalogo de
-- estatus editable): catalogo de estatus que reemplaza por dentro al enum
-- fijo clientes.estado_cliente, sin tocar ese enum (se deja intacto, solo
-- deja de ser la fuente de verdad) ni el contrato JSON que ya usa el
-- frontend (ClienteResponse.estadoCliente sigue siendo el mismo texto).
CREATE TABLE estatus (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    valor         VARCHAR(30)  NOT NULL,
    descripcion   VARCHAR(255) NOT NULL,
    es_sistema    BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME    NULL,
    eliminado_en  DATETIME     NULL,
    CONSTRAINT uq_estatus_valor UNIQUE (valor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Los 4 estatus del sistema. IDs fijos (1-4): el backend los referencia por
-- constante (ver EstatusService), nunca comparando texto.
INSERT INTO estatus (id, valor, descripcion, es_sistema) VALUES
    (1, 'ACTIVO', 'Registro vigente. Puede recibir servicios y asociar vehiculos.', TRUE),
    (2, 'SUSPENDIDO', 'Bloqueado temporalmente. Conserva sus datos pero no puede asociar vehiculos ni abrir ordenes hasta ser reactivado.', TRUE),
    (3, 'ACTUALIZADO', 'Movimiento que indica que los datos del registro fueron modificados. Se usa en el historial.', TRUE),
    (4, 'ELIMINADO', 'Baja logica (soft delete). Se oculta de los listados, pero nunca se borra de la base de datos.', TRUE);

-- clientes.estatus_id: nueva columna, FK al catalogo. Convive con la
-- columna estado_cliente (ENUM) que ya existia; no se elimina esa columna.
ALTER TABLE clientes
    ADD COLUMN estatus_id BIGINT NULL AFTER estado_cliente,
    ADD CONSTRAINT fk_clientes_estatus FOREIGN KEY (estatus_id) REFERENCES estatus(id),
    ADD INDEX idx_clientes_estatus (estatus_id);

UPDATE clientes SET estatus_id = 1 WHERE estado_cliente = 'ACTIVO';
UPDATE clientes SET estatus_id = 2 WHERE estado_cliente = 'SUSPENDIDO';

ALTER TABLE clientes MODIFY estatus_id BIGINT NOT NULL;
