-- Rol nuevo para el registro de clientes (fase 2). No modifica los roles
-- existentes ni el comportamiento de ADMIN/EMPLEADO/CLIENTE.
INSERT INTO roles (nombre, descripcion) VALUES
    ('RECEPCIONISTA', 'Registra y da mantenimiento a los datos de los clientes del taller');
