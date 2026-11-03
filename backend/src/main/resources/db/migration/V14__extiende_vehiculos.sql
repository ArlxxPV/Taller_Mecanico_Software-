-- Fase 3 (registro de vehiculos + catalogo NHTSA vPIC). La tabla vehiculos
-- ya existia desde V1 (minima, sin usar por ningun endpoint todavia: 0
-- filas), se completa aqui con lo que pide esta fase. vin pasa a NOT NULL
-- (ya era UNIQUE) porque ahora es un dato obligatorio del formulario.
ALTER TABLE vehiculos
    MODIFY vin                VARCHAR(17)  NOT NULL,
    ADD COLUMN numero_serie   VARCHAR(17)  NOT NULL AFTER vin,
    ADD COLUMN version        VARCHAR(80)  NOT NULL AFTER modelo,
    ADD COLUMN estatus_id     BIGINT       NOT NULL DEFAULT 1 AFTER color,
    ADD COLUMN marca_api_id   INT          NULL AFTER estatus_id,
    ADD COLUMN modelo_api_id  INT          NULL AFTER marca_api_id,
    ADD COLUMN creado_por_id  BIGINT       NULL AFTER creado_en,
    ADD COLUMN actualizado_en DATETIME     NULL AFTER creado_por_id,
    ADD COLUMN eliminado_en   DATETIME     NULL AFTER actualizado_en,
    ADD CONSTRAINT fk_vehiculos_estatus FOREIGN KEY (estatus_id) REFERENCES estatus(id),
    ADD CONSTRAINT fk_vehiculos_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios(id),
    ADD INDEX idx_vehiculos_estatus (estatus_id);
