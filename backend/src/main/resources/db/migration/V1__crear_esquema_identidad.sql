-- =====================================================================
-- Modulo 1: identidad, usuarios, roles, clientes y vehiculos
-- Se ejecuta automaticamente por Flyway al levantar el backend, dentro
-- de la base de datos `tallerpro` del contenedor tallerpro-mysql.
-- =====================================================================

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuarios (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo      VARCHAR(120) NOT NULL,
    email                VARCHAR(150) NOT NULL UNIQUE,
    telefono             VARCHAR(20)  NULL,
    password_hash        VARCHAR(255) NOT NULL,
    estado               ENUM('ACTIVO', 'BLOQUEADO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos     INT NOT NULL DEFAULT 0,
    bloqueado_hasta       DATETIME NULL,
    ultimo_login_en       DATETIME NULL,
    ultimo_login_ip       VARCHAR(64) NULL,
    creado_en             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usuarios_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Relacion muchos-a-muchos: un usuario puede tener uno o mas roles
-- (deja abierta la puerta a roles mas finos como MECANICO o RECEPCIONISTA
-- en fases futuras, sin tocar el esquema).
CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tokens de un solo uso para el flujo de "olvide mi contrasena".
-- Se guarda el HASH del token, nunca el token en claro.
CREATE TABLE password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id   BIGINT NOT NULL,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    expira_en    DATETIME NOT NULL,
    usado        BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_reset_tokens_usuario (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bitacora de inicios de sesion (exitosos y fallidos): responde a
-- "quien inicia sesion y en que momento" sin depender de sesiones de servidor,
-- ya que el backend es una API sin estado (JWT).
CREATE TABLE login_audit (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT NULL,
    email_intento   VARCHAR(150) NOT NULL,
    exitoso         BOOLEAN NOT NULL,
    motivo_fallo    VARCHAR(100) NULL,
    ip_origen       VARCHAR(64) NULL,
    user_agent      VARCHAR(255) NULL,
    creado_en       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_audit_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_login_audit_usuario (usuario_id),
    INDEX idx_login_audit_fecha (creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Datos de negocio del cliente del taller (1 a 1 con un usuario que tiene rol CLIENTE).
-- Se separa de `usuarios` para que la identidad/autenticacion quede aislada
-- de los datos propios del negocio (direccion, identificacion, etc.).
CREATE TABLE clientes (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id           BIGINT NOT NULL UNIQUE,
    direccion            VARCHAR(255) NULL,
    documento_identidad   VARCHAR(50) NULL,
    notas                VARCHAR(500) NULL,
    creado_en            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vehiculos (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id     BIGINT NOT NULL,
    marca          VARCHAR(60) NOT NULL,
    modelo         VARCHAR(60) NOT NULL,
    anio           SMALLINT NOT NULL,
    placa          VARCHAR(20) NOT NULL UNIQUE,
    color          VARCHAR(30) NULL,
    vin            VARCHAR(32) NULL UNIQUE,
    kilometraje     INT NULL,
    creado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehiculos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    INDEX idx_vehiculos_cliente (cliente_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
