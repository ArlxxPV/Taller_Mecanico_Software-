-- Fase 2 (administracion de clientes): auditoria de edicion y tabla de
-- historial de movimientos de estatus. estatus_id ya existia desde V10;
-- aqui se agregan las columnas de auditoria que le faltaban (ver maqueta
-- "Detalle de cliente", pie: "Alta: ... Ultima edicion: ... por ...").
ALTER TABLE clientes
    ADD COLUMN actualizado_en     DATETIME NULL AFTER creado_en,
    ADD COLUMN actualizado_por_id BIGINT   NULL AFTER actualizado_en,
    ADD COLUMN eliminado_en       DATETIME NULL AFTER actualizado_por_id,
    ADD CONSTRAINT fk_clientes_actualizado_por FOREIGN KEY (actualizado_por_id) REFERENCES usuarios(id);

CREATE TABLE historial_estatus_cliente (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id          BIGINT NOT NULL,
    estatus_anterior_id BIGINT NULL,
    estatus_nuevo_id    BIGINT NOT NULL,
    motivo              VARCHAR(255) NULL,
    cambios             JSON NULL,
    usuario_id          BIGINT NULL,
    creado_en           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_historial_estatus_anterior FOREIGN KEY (estatus_anterior_id) REFERENCES estatus(id),
    CONSTRAINT fk_historial_estatus_nuevo FOREIGN KEY (estatus_nuevo_id) REFERENCES estatus(id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_historial_cliente (cliente_id, creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Para que la pestana Historial de un cliente ya existente no aparezca
-- vacia: una fila de "Alta" con el estatus que ya tiene hoy. usuario_id
-- queda NULL (no hay forma confiable de saber quien de los registros viejos
-- lo dio de alta; el frontend usa origen_registro como respaldo en ese caso).
INSERT INTO historial_estatus_cliente (cliente_id, estatus_anterior_id, estatus_nuevo_id, motivo, usuario_id, creado_en)
SELECT id, NULL, estatus_id, 'Alta', NULL, creado_en FROM clientes;
