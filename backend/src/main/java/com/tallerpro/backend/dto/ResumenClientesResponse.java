package com.tallerpro.backend.dto;

// total/activos/suspendidos excluyen eliminados (ver maqueta: la tarjeta
// "Eliminados" es aparte). El campo se agrega al final para no romper a
// nadie que ya desestructure este record por posicion.
public record ResumenClientesResponse(long total, long activos, long suspendidos, long eliminados) {
}
