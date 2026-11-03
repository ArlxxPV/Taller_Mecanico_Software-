package com.tallerpro.backend.model;

/**
 * IDs fijos de los 4 estatus de sistema sembrados en V10. El codigo nunca
 * debe comparar por texto ("ACTIVO".equals(...)) para decidir logica de
 * negocio; usa estas constantes contra Cliente.getEstatusId()/Vehiculo
 * (fases siguientes). El catalogo en si (tabla estatus) es editable, pero
 * estos 4 ids especificos no se pueden dar de baja (ver EstatusService).
 */
public final class EstatusId {
    public static final Long ACTIVO = 1L;
    public static final Long SUSPENDIDO = 2L;
    public static final Long ACTUALIZADO = 3L;
    public static final Long ELIMINADO = 4L;

    private EstatusId() {
    }
}
