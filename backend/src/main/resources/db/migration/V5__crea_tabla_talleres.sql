-- Fase 2 (subproceso 1): alta de talleres. Primera tabla de "organizacion"
-- del sistema; usuarios y clientes se asociaran a un taller en las
-- migraciones siguientes (V6, V7). Nace vacia a proposito: el usuario
-- registrara los 3 talleres reales desde la pantalla de Alta de talleres.
CREATE TABLE talleres (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre             VARCHAR(150) NOT NULL,
    razon_social       VARCHAR(150) NOT NULL,
    telefono           VARCHAR(10)  NOT NULL,
    rfc                VARCHAR(13)  NOT NULL,
    email_contacto     VARCHAR(150) NOT NULL,
    calle              VARCHAR(150) NOT NULL,
    colonia            VARCHAR(100) NOT NULL,
    municipio          VARCHAR(100) NOT NULL,
    estado_direccion   VARCHAR(100) NOT NULL,
    codigo_postal      VARCHAR(10)  NOT NULL,
    foto_url           VARCHAR(255) NULL,
    creado_en          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uq_talleres_nombre ON talleres (nombre);
CREATE UNIQUE INDEX uq_talleres_rfc ON talleres (rfc);
