package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una fila por cada movimiento de estatus de un cliente (alta, suspension,
 * reactivacion, edicion, eliminacion, restauracion). "cambios" solo se usa
 * en movimientos ACTUALIZADO: JSON con {"campo": {"anterior":..,"nuevo":..}}
 * de los campos que de verdad cambiaron (ver ClienteService.diferencias).
 */
@Entity
@Table(name = "historial_estatus_cliente")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstatusCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estatus_anterior_id")
    private Estatus estatusAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estatus_nuevo_id", nullable = false)
    private Estatus estatusNuevo;

    @Column(length = 255)
    private String motivo;

    @Column(columnDefinition = "json")
    private String cambios;

    // Nulo en los movimientos de "Alta" migrados de clientes anteriores a
    // esta fase (ver backfill en V12): no hay forma confiable de saber quien
    // los registro. El frontend usa Cliente.origenRegistro como respaldo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
