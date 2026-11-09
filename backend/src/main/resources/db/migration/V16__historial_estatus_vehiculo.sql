-- Auditoria de vehiculo: una fila por cada edicion (con el diff en "cambios")
-- y por cada activar/desactivar. Mismo patron exacto que historial_estatus_cliente
-- (V12): estatus_anterior_id nulo en las filas de "Alta"/edicion, estatus_nuevo_id
-- reusa el estatus de sistema ACTUALIZADO (id 3) para marcar una edicion sin
-- transicion real de estatus. Nunca se borra una fila.
CREATE TABLE historial_estatus_vehiculo (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehiculo_id         BIGINT NOT NULL,
    estatus_anterior_id BIGINT NULL,
    estatus_nuevo_id    BIGINT NOT NULL,
    motivo              VARCHAR(255) NULL,
    cambios             JSON NULL,
    usuario_id          BIGINT NULL,
    creado_en           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hev_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id),
    CONSTRAINT fk_hev_estatus_anterior FOREIGN KEY (estatus_anterior_id) REFERENCES estatus(id),
    CONSTRAINT fk_hev_estatus_nuevo FOREIGN KEY (estatus_nuevo_id) REFERENCES estatus(id),
    CONSTRAINT fk_hev_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_hev_vehiculo (vehiculo_id, creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Backfill: una fila "Alta" por cada vehiculo que ya existia (no se pierde
-- historial de lo que ya habia), mismo criterio que V12 para clientes.
INSERT INTO historial_estatus_vehiculo (vehiculo_id, estatus_nuevo_id, motivo, usuario_id, creado_en)
SELECT v.id, v.estatus_id, 'Alta', v.creado_por_id, v.creado_en
FROM vehiculos v;
