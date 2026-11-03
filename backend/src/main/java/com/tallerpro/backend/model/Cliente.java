package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nulo cuando el cliente lo dio de alta un ADMIN/RECEPCIONISTA "de
    // mostrador" y todavia no tiene una cuenta propia con la que iniciar
    // sesion (ver AuthService.registrar() para el caso de auto-registro).
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "usuario_id", nullable = true, unique = true)
    private Usuario usuario;

    // Si el cliente tiene cuenta (usuario != null), este valor se mantiene
    // igual al de `usuario.nombreCompleto`. Si no tiene cuenta (alta manual
    // por ADMIN/RECEPCIONISTA), este es el unico lugar donde vive su nombre.
    @Column(name = "nombre_completo", length = 180)
    private String nombreCompleto;

    @Column(length = 60)
    private String nombres;

    @Column(name = "apellido_paterno", length = 60)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 60)
    private String apellidoMaterno;

    @Column(length = 255)
    private String direccion;

    @Column(name = "documento_identidad", length = 50)
    private String documentoIdentidad;

    @Column(length = 18)
    private String curp;

    // Texto libre (ya no enum): 'AUTORREGISTRO' o el/los nombre(s) de rol del
    // actor que lo registro (ver ClienteService) - con roles dinamicos ya no
    // hay un conjunto fijo de "quien pudo haber registrado".
    @Column(name = "origen_registro", length = 100)
    private String origenRegistro;

    // Congelado desde la fase 2 nueva (catalogo de estatus, ver EstatusId):
    // ya no se vuelve a escribir, se conserva solo porque ya existia (regla
    // del proyecto de no borrar columnas que funcionan). La fuente de verdad
    // actual del estatus del cliente es `estatus` (abajo).
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cliente", nullable = false, length = 20)
    private EstadoCliente estadoCliente = EstadoCliente.ACTIVO;

    // Fuente de verdad del estatus del cliente (ACTIVO/SUSPENDIDO/ELIMINADO,
    // nunca ACTUALIZADO: ese valor es solo para filas de historial). Ver
    // ClienteService.suspender/reactivar/eliminar/restaurar.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estatus_id", nullable = false)
    private Estatus estatus;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por_id")
    private Usuario actualizadoPor;

    @Column(name = "eliminado_en")
    private LocalDateTime eliminadoEn;

    // Taller al que pertenece el cliente. Nulo para clientes antiguos (antes
    // de esta fase) o autorregistrados, hasta que se les asigne uno al editar.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id")
    private Taller taller;

    @Column(length = 500)
    private String notas;

    @Column(name = "contacto_alternativo", length = 150)
    private String contactoAlternativo;

    private Integer edad;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono_personal", length = 20)
    private String telefonoPersonal;

    @Column(name = "telefono_trabajo", length = 20)
    private String telefonoTrabajo;

    // Correo propio del registro de cliente (distinto del correo de acceso en
    // `usuarios`, que solo existe si este cliente tiene cuenta).
    @Column(length = 150)
    private String email;

    @Column(name = "email_trabajo", length = 150)
    private String emailTrabajo;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

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

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehiculo> vehiculos = new ArrayList<>();
}
