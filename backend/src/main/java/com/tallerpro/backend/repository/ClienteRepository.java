package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.Cliente;
import com.tallerpro.backend.model.EstadoCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuario_Id(Long usuarioId);

    // Clientes sin cuenta propia (dados de alta por ADMIN/RECEPCIONISTA "de
    // mostrador"): no pueden iniciar sesion hasta que se les asigne una
    // contrasena (ver ClienteService.asignarAcceso).
    List<Cliente> findByUsuarioIsNull();

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByCurpIgnoreCase(String curp);

    Optional<Cliente> findByCurpIgnoreCase(String curp);

    boolean existsByCurpIgnoreCaseAndIdNot(String curp, Long id);

    // findAll(Pageable) (heredado) cubre el caso "sin filtros".
    Page<Cliente> findByTaller_Id(Long tallerId, Pageable pageable);

    Page<Cliente> findByEstadoCliente(EstadoCliente estadoCliente, Pageable pageable);

    Page<Cliente> findByTaller_IdAndEstadoCliente(Long tallerId, EstadoCliente estadoCliente, Pageable pageable);

    // count() (heredado) cubre el total sin filtro de taller.
    long countByTaller_Id(Long tallerId);

    long countByEstadoCliente(EstadoCliente estadoCliente);

    long countByTaller_IdAndEstadoCliente(Long tallerId, EstadoCliente estadoCliente);

    // Cuenta directo contra la columna estatus_id (catalogo de estatus, fase
    // 2 nueva) via SQL nativo: Cliente todavia no mapea esa columna como
    // entidad/relacion (eso llega cuando el resto de esa fase la empiece a
    // usar); esto solo sirve para el "en uso" del catalogo de estatus.
    @Query(value = "SELECT COUNT(*) FROM clientes WHERE estatus_id = :estatusId", nativeQuery = true)
    long contarPorEstatusId(@Param("estatusId") Long estatusId);

    // === Administracion de clientes (fase 2 nueva) ===============

    // Un solo SELECT con JOIN a estatus y taller (taller es nullable en
    // clientes antiguos, por eso LEFT JOIN FETCH; estatus nunca lo es desde
    // V10). Todos los filtros son opcionales (:param IS NULL se salta la
    // condicion) y van parametrizados, nunca concatenados. countQuery
    // explicito con el mismo WHERE, sin los FETCH (no hacen falta para contar).
    @Query(
            value = """
                    SELECT c FROM Cliente c
                    LEFT JOIN FETCH c.taller
                    JOIN FETCH c.estatus
                    WHERE (:tallerId IS NULL OR c.taller.id = :tallerId)
                      AND (:estatusId IS NULL OR c.estatus.id = :estatusId)
                      AND (:incluirEliminados = true OR c.eliminadoEn IS NULL)
                      AND (:busqueda IS NULL OR :busqueda = ''
                           OR LOWER(c.nombreCompleto) LIKE LOWER(CONCAT(:busqueda, '%'))
                           OR LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT(:busqueda, '%'))
                           OR c.telefonoPersonal LIKE CONCAT(:busqueda, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(c) FROM Cliente c
                    WHERE (:tallerId IS NULL OR c.taller.id = :tallerId)
                      AND (:estatusId IS NULL OR c.estatus.id = :estatusId)
                      AND (:incluirEliminados = true OR c.eliminadoEn IS NULL)
                      AND (:busqueda IS NULL OR :busqueda = ''
                           OR LOWER(c.nombreCompleto) LIKE LOWER(CONCAT(:busqueda, '%'))
                           OR LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT(:busqueda, '%'))
                           OR c.telefonoPersonal LIKE CONCAT(:busqueda, '%'))
                    """
    )
    Page<Cliente> buscar(@Param("tallerId") Long tallerId, @Param("estatusId") Long estatusId,
                          @Param("incluirEliminados") boolean incluirEliminados, @Param("busqueda") String busqueda,
                          Pageable pageable);

    // Resumen (tarjetas clicables): total/activos/suspendidos excluyen
    // eliminados; eliminados se cuenta aparte. tallerId null = todos los talleres.
    long countByTaller_IdAndEliminadoEnIsNull(Long tallerId);

    long countByEliminadoEnIsNull();

    long countByTaller_IdAndEstatus_IdAndEliminadoEnIsNull(Long tallerId, Long estatusId);

    long countByEstatus_IdAndEliminadoEnIsNull(Long estatusId);

    long countByTaller_IdAndEliminadoEnIsNotNull(Long tallerId);

    long countByEliminadoEnIsNotNull();

    // "Clientes nuevos este mes" del panel (fase 4).
    long countByCreadoEnGreaterThanEqualAndEliminadoEnIsNull(LocalDateTime desde);

    long countByTaller_IdAndCreadoEnGreaterThanEqualAndEliminadoEnIsNull(Long tallerId, LocalDateTime desde);

    // UPDATE condicionado (atomico: evita una condicion de carrera entre leer
    // el estatus y escribirlo). 0 filas afectadas = o no existe, o no estaba
    // en el estatus requerido; ClienteService decide cual de los dos fue.
    // clearAutomatically: limpia el contexto de persistencia tras el UPDATE
    // nativo, para que una lectura posterior en la MISMA transaccion (ej.
    // ClienteService.suspender() llamando a obtener() justo despues) no
    // devuelva la entidad vieja cacheada en el primer nivel de Hibernate.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE clientes SET estatus_id = :nuevoEstatusId, actualizado_en = NOW(), actualizado_por_id = :actorId
            WHERE id = :id AND estatus_id = :estatusRequeridoId AND eliminado_en IS NULL
            """, nativeQuery = true)
    int cambiarEstatusSiEsta(@Param("id") Long id, @Param("nuevoEstatusId") Long nuevoEstatusId,
                              @Param("estatusRequeridoId") Long estatusRequeridoId, @Param("actorId") Long actorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE clientes SET estatus_id = :eliminadoId, eliminado_en = NOW(), actualizado_en = NOW(), actualizado_por_id = :actorId
            WHERE id = :id AND eliminado_en IS NULL
            """, nativeQuery = true)
    int eliminarSiNoEliminado(@Param("id") Long id, @Param("eliminadoId") Long eliminadoId, @Param("actorId") Long actorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE clientes SET estatus_id = :activoId, eliminado_en = NULL, actualizado_en = NOW(), actualizado_por_id = :actorId
            WHERE id = :id AND eliminado_en IS NOT NULL
            """, nativeQuery = true)
    int restaurarSiEliminado(@Param("id") Long id, @Param("activoId") Long activoId, @Param("actorId") Long actorId);
}
