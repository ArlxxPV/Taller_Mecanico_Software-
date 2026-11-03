package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Catalogo editable de estatus (fase 2). Los 4 registros de sistema (ids
 * 1-4, ver V10) los referencia el backend por constante (EstatusService) y
 * nunca se pueden dar de baja ni cambiar de valor. Un admin puede agregar
 * mas (ej. "EN_REVISION") y darlos de baja (soft delete via eliminadoEn)
 * si ningun cliente/vehiculo los usa.
 */
@Entity
@Table(name = "estatus")
@Getter
@Setter
@NoArgsConstructor
public class Estatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String valor;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(name = "eliminado_en")
    private LocalDateTime eliminadoEn;
}
