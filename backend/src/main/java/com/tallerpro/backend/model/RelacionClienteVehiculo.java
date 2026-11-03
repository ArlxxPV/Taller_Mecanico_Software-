package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Historial de relaciones cliente-vehiculo (UC-CV-11/12/13/14). A lo sumo
 * una fila por vehiculo tiene vigente=true en un momento dado (lo garantiza
 * VehiculoService, no una constraint de BD - mismo estilo que el resto del
 * proyecto). vehiculos.cliente_id sigue existiendo y se mantiene sincronizado
 * con la relacion vigente (puntero rapido al cliente actual); esta tabla es
 * el historial/auditoria completo de cada cambio.
 */
@Entity
@Table(name = "historial_relacion_cliente_vehiculo")
@Getter
@Setter
@NoArgsConstructor
public class RelacionClienteVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 30)
    private String tipo = "PROPIETARIO";

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
