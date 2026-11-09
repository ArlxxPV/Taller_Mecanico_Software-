package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una fila por cada movimiento de estatus de un vehiculo (alta, edicion,
 * activacion, desactivacion). "cambios" solo se usa en movimientos
 * ACTUALIZADO: JSON con {"campo": ["anterior","nuevo"]} de los campos que de
 * verdad cambiaron (ver VehiculoService.diferencias). Mismo patron exacto
 * que HistorialEstatusCliente.
 */
@Entity
@Table(name = "historial_estatus_vehiculo")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstatusVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
