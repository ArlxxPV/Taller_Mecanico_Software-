package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Cliente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombreCompleto,
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
        LocalDateTime creadoEn
) {
    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombreCompleto(),
                cliente.getContactoAlternativo(),
                cliente.getEdad(),
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
                cliente.getCreadoEn()
        );
    }
}
