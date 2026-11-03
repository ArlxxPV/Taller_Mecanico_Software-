package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Cliente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public record ClienteResponse(
        Long id,
        String nombreCompleto,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String curp,
        String contactoAlternativo,
        Integer edad,
        LocalDate fechaNacimiento,
        String telefonoPersonal,
        String telefonoTrabajo,
        String email,
        String emailTrabajo,
        boolean tieneFoto,
        String calle,
        String colonia,
        String municipio,
        String estado,
        String codigoPostal,
        String origenRegistro,
        String estadoCliente,
        Long tallerId,
        String tallerNombre,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        String actualizadoPorNombre,
        LocalDateTime eliminadoEn
) {
    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombreCompleto(),
                cliente.getNombres(),
                cliente.getApellidoPaterno(),
                cliente.getApellidoMaterno(),
                cliente.getCurp(),
                cliente.getContactoAlternativo(),
                calcularEdad(cliente.getFechaNacimiento()),
                cliente.getFechaNacimiento(),
                cliente.getTelefonoPersonal(),
                cliente.getTelefonoTrabajo(),
                cliente.getEmail(),
                cliente.getEmailTrabajo(),
                cliente.getFotoUrl() != null,
                cliente.getCalle(),
                cliente.getColonia(),
                cliente.getMunicipio(),
                cliente.getEstadoDireccion(),
                cliente.getCodigoPostal(),
                cliente.getOrigenRegistro(),
                // Fuente de verdad desde la fase 2 nueva: el catalogo de estatus
                // (ver Cliente.estatus), no el enum congelado estadoCliente.
                // Mismo campo/formato de siempre (texto: "ACTIVO"/"SUSPENDIDO"),
                // ahora tambien puede ser "ELIMINADO".
                cliente.getEstatus().getValor(),
                cliente.getTaller() != null ? cliente.getTaller().getId() : null,
                cliente.getTaller() != null ? cliente.getTaller().getNombre() : null,
                cliente.getCreadoEn(),
                cliente.getActualizadoEn(),
                cliente.getActualizadoPor() != null ? cliente.getActualizadoPor().getNombreCompleto() : null,
                cliente.getEliminadoEn()
        );
    }

    private static Integer calcularEdad(LocalDate fechaNacimiento) {
        return fechaNacimiento != null ? Period.between(fechaNacimiento, LocalDate.now()).getYears() : null;
    }
}
