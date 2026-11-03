package com.tallerpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El valor se normaliza en el servicio (mayusculas, espacios -> "_"), no
 * aqui: la validacion de formato (regex ^[A-Z_]{3,30}$) se aplica ya
 * normalizado, porque el usuario puede escribir en minusculas o con espacios
 * y eso es valido en la UI (ver maqueta "Agregar estatus").
 */
public record EstatusRequest(
        @NotBlank(message = "El valor es obligatorio")
        @Size(max = 30, message = "El valor no debe superar 30 caracteres")
        String valor,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(min = 10, max = 255, message = "La descripcion debe tener entre 10 y 255 caracteres")
        String descripcion
) {
}
