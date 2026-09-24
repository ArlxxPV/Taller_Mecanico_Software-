package com.tallerpro.backend.model;

/**
 * Nombres de rol soportados. Vive como enum en Java para tener autocompletado
 * y seguridad de tipos en el codigo; la tabla `roles` en MySQL es la fuente
 * de verdad real (puedes agregar filas ahi sin redeployar si usas el nombre
 * como texto libre en requestMatchers, pero para los 3 roles base esto basta).
 */
public enum NombreRol {
    ADMIN,
    EMPLEADO,
    CLIENTE
}
