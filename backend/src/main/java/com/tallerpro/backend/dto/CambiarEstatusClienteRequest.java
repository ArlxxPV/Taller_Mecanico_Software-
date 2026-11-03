package com.tallerpro.backend.dto;

import com.tallerpro.backend.model.EstadoCliente;
import jakarta.validation.constraints.NotNull;

public record CambiarEstatusClienteRequest(
        @NotNull(message = "El estatus es obligatorio")
        EstadoCliente estadoCliente
) {
}
