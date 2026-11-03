package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Texto libre (ya no atado a un enum Java): un admin puede crear roles
    // nuevos con el nombre que quiera desde la pantalla de Roles.
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String descripcion;

    // Solo ADMIN. Marca el rol como "acceso total siempre": PermisoService
    // hace bypass de rol_permisos para estos roles, y RolService rechaza
    // cualquier intento de editar sus permisos (ver registro_Fase02.md).
    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema;

    // Un rol suspendido deja de otorgar permisos (ver PermisoService) sin
    // perder su configuracion; se puede reactivar despues. ADMIN nunca se
    // puede suspender (ver RolService.cambiarEstado).
    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permisos",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    private Set<Permiso> permisos = new HashSet<>();
}
