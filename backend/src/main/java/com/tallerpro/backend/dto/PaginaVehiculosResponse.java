package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Vehiculo;
import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaVehiculosResponse(
        List<VehiculoResponse> vehiculos,
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        int tamanioPagina
) {
    public static PaginaVehiculosResponse desde(Page<Vehiculo> pagina) {
        return new PaginaVehiculosResponse(
                pagina.getContent().stream().map(VehiculoResponse::desde).toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.getSize()
        );
    }
}
