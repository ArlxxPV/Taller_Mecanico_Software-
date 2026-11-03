package com.tallerpro.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El frontend normaliza VIN/placa a mayusculas y sin espacios/guiones ANTES
 * de enviar (igual que ya hace con la CURP en ClienteFormularioCampos): el
 * @Pattern de aqui es la validacion que de verdad cuenta, no confia en eso.
 * El anio se valida en el servicio (1950 a anio actual + 1: el limite
 * superior no es una constante de compilacion, @Max no sirve para eso).
 */
public record VehiculoRequest(

        @NotBlank(message = "El VIN es obligatorio")
        @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "El VIN debe tener 17 caracteres validos (sin I, O ni Q)")
        String vin,

        @NotBlank(message = "El numero de serie es obligatorio")
        @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{8,17}$", message = "El numero de serie debe tener de 8 a 17 caracteres validos")
        String numeroSerie,

        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^[A-HJ-NPR-Z]{3}(\\d{4}|\\d{3}[A-HJ-NPR-Z]?)$", message = "La placa no tiene un formato valido")
        String placa,

        @NotNull(message = "El anio es obligatorio")
        Integer anio,

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 60, message = "La marca no debe superar 60 caracteres")
        String marca,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 60, message = "El modelo no debe superar 60 caracteres")
        String modelo,

        @NotBlank(message = "La version es obligatoria")
        @Size(max = 80, message = "La version no debe superar 80 caracteres")
        String version,

        @NotBlank(message = "El color es obligatorio")
        @Pattern(regexp = "^[A-Za-zÀ-ÿ ]{3,40}$", message = "El color debe tener de 3 a 40 letras")
        String color,

        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = 2_000_000, message = "El kilometraje no debe superar 2,000,000")
        Integer kilometraje,

        Integer marcaApiId,
        Integer modeloApiId
) {
}
