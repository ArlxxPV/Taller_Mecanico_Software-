package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Taller;

import java.time.LocalDateTime;

public record TallerResponse(
        Long id,
        String nombre,
        String razonSocial,
        String telefono,
        String rfc,
        String emailContacto,
        boolean tieneFoto,
        String calle,
        String colonia,
        String municipio,
        String estado,
        String codigoPostal,
        LocalDateTime creadoEn
) {
    public static TallerResponse desde(Taller taller) {
        return new TallerResponse(
                taller.getId(),
                taller.getNombre(),
                taller.getRazonSocial(),
                taller.getTelefono(),
                taller.getRfc(),
                taller.getEmailContacto(),
                taller.getFotoUrl() != null,
                taller.getCalle(),
                taller.getColonia(),
                taller.getMunicipio(),
                taller.getEstadoDireccion(),
                taller.getCodigoPostal(),
                taller.getCreadoEn()
        );
    }
}
