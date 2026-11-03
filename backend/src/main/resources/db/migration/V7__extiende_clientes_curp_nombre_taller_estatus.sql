-- Fase 2 (modulo use-cv-02 + ajustes de Fase 01 al registro de clientes):
-- nombre dividido, CURP, origen de registro, estatus y taller del cliente.
-- Nullable en los campos que no se pueden rellenar con certeza, porque ya
-- existen clientes sin estos datos; el formulario los exigira a partir de
-- ahora, asi que quedaran completos la proxima vez que se editen.
ALTER TABLE clientes
    MODIFY nombre_completo VARCHAR(180) NULL,
    ADD COLUMN nombres           VARCHAR(60)  NULL AFTER nombre_completo,
    ADD COLUMN apellido_paterno  VARCHAR(60)  NULL AFTER nombres,
    ADD COLUMN apellido_materno  VARCHAR(60)  NULL AFTER apellido_paterno,
    ADD COLUMN curp              VARCHAR(18)  NULL AFTER documento_identidad,
    ADD COLUMN origen_registro   ENUM('ADMIN','RECEPCIONISTA','AUTORREGISTRO') NULL,
    ADD COLUMN estado_cliente    ENUM('ACTIVO','SUSPENDIDO') NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN taller_id         BIGINT NULL,
    ADD CONSTRAINT fk_clientes_taller FOREIGN KEY (taller_id) REFERENCES talleres(id) ON DELETE SET NULL,
    ADD INDEX idx_clientes_taller (taller_id),
    ADD INDEX idx_clientes_estado_cliente (estado_cliente);

CREATE UNIQUE INDEX uq_clientes_curp ON clientes (curp);

-- Backfill de origen_registro para los clientes que ya existian: los que
-- tienen cuenta propia (usuario_id) solo pudieron crearse via autorregistro
-- (AuthService.registrar); los que no tienen cuenta se dieron de alta a mano
-- por mostrador. El codigo anterior no distinguia ADMIN de RECEPCIONISTA en
-- ese flujo, asi que se aproxima a 'ADMIN' (campo solo informativo/de
-- auditoria, no condiciona permisos de ningun cliente ya creado).
UPDATE clientes
SET origen_registro = CASE WHEN usuario_id IS NOT NULL THEN 'AUTORREGISTRO' ELSE 'ADMIN' END
WHERE origen_registro IS NULL;

ALTER TABLE clientes
    MODIFY origen_registro ENUM('ADMIN','RECEPCIONISTA','AUTORREGISTRO') NOT NULL;
