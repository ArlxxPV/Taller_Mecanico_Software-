package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** UC-CV-12: cambiar el cliente al que esta asociado un vehiculo. */
public record CambiarClienteVehiculoRequest(
        @NotNull(message = "El nuevo cliente es obligatorio")
        Long nuevoClienteId,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 255, message = "El motivo debe tener entre 10 y 255 caracteres")
        String motivo
) {
}
