-- Caso de uso "cambiar propietario de un vehiculo": hasta ahora
-- vehiculos.cliente_id era una relacion directa sin historial. Esta tabla
-- registra cada relacion cliente-vehiculo (con vigencia y motivo), sin
-- borrar ni tocar vehiculos.cliente_id: esa columna sigue existiendo y
-- sigue siendo el puntero rapido al cliente ACTUAL (se actualiza al cambiar
-- de propietario), esta tabla es el historial/auditoria completo.
CREATE TABLE historial_relacion_cliente_vehiculo (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehiculo_id  BIGINT NOT NULL,
    cliente_id   BIGINT NOT NULL,
    tipo         VARCHAR(30) NOT NULL DEFAULT 'PROPIETARIO',
    vigente      BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin    DATETIME NULL,
    motivo       VARCHAR(255) NULL,
    usuario_id   BIGINT NULL,
    creado_en    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hrcv_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id),
    CONSTRAINT fk_hrcv_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_hrcv_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_hrcv_vehiculo_vigente (vehiculo_id, vigente),
    INDEX idx_hrcv_cliente (cliente_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Backfill: una relacion vigente por cada vehiculo que ya existia, con el
-- cliente que ya tenia asignado (no se pierde ningun dato existente).
INSERT INTO historial_relacion_cliente_vehiculo (vehiculo_id, cliente_id, tipo, vigente, fecha_inicio, usuario_id, creado_en)
SELECT v.id, v.cliente_id, 'PROPIETARIO', TRUE, v.creado_en, v.creado_por_id, NOW()
FROM vehiculos v;
