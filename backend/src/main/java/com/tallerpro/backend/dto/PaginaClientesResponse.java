package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.Cliente;
import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaClientesResponse(
        List<ClienteResponse> clientes,
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        int tamanioPagina
) {
    public static PaginaClientesResponse desde(Page<Cliente> pagina) {
        return new PaginaClientesResponse(
                pagina.getContent().stream().map(ClienteResponse::desde).toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.getSize()
        );
    }
}
