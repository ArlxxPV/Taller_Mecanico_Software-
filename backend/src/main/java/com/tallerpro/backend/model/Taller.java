package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "talleres")
@Getter
@Setter
@NoArgsConstructor
public class Taller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String nombre;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(length = 10)
    private String telefono;

    @Column(length = 13)
    private String rfc;

    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    @Column(length = 150)
    private String calle;

    @Column(length = 100)
    private String colonia;

    @Column(length = 100)
    private String municipio;

    @Column(name = "estado_direccion", length = 100)
    private String estadoDireccion;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
