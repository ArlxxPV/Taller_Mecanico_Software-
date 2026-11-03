package com.tallerpro.backend.dto;

// Usado por los chequeos "al salir del campo" de VIN y placa (ver maqueta
// "Modal Asociar vehiculo": "Formato valido * digito verificador correcto *
// no registrado"). disponible=false quiere decir que ya hay un vehiculo con
// ese VIN/placa.
public record DisponibilidadResponse(boolean disponible) {
}
