-- Fase 2: aislamiento de datos por taller. Una cuenta RECEPCIONISTA queda
-- atada a un taller (asignacion manual por SQL, ver documentacion); ADMIN,
-- EMPLEADO y CLIENTE no lo necesitan y quedan con taller_id NULL. Cambio de
-- esquema no pedido literalmente por el enunciado pero indispensable: sin
-- esto no hay forma de resolver "el taller de la recepcionista actual".
ALTER TABLE usuarios
    ADD COLUMN taller_id BIGINT NULL AFTER telefono,
    ADD CONSTRAINT fk_usuarios_taller FOREIGN KEY (taller_id) REFERENCES talleres(id) ON DELETE SET NULL,
    ADD INDEX idx_usuarios_taller (taller_id);
