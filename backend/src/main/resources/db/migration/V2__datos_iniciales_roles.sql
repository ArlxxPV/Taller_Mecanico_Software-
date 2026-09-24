-- Roles base del sistema. Puedes agregar mas (MECANICO, RECEPCIONISTA, etc.)
-- con un INSERT nuevo en una migracion futura V3__..., sin tocar codigo Java.
INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMIN', 'Acceso total: gestiona usuarios, roles y configuracion del taller'),
    ('EMPLEADO', 'Personal del taller: atendera ordenes, clientes y vehiculos en proximos modulos'),
    ('CLIENTE', 'Dueno de uno o mas vehiculos atendidos por el taller');
