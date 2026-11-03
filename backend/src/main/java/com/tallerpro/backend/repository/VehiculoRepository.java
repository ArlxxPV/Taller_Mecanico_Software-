package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.Vehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByCliente_Id(Long clienteId);

    boolean existsByPlacaIgnoreCase(String placa);

    boolean existsByVinIgnoreCase(String vin);

    // Para editar: la misma placa/VIN que el vehiculo ya tenia no cuenta como duplicado.
    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);

    boolean existsByVinIgnoreCaseAndIdNot(String vin, Long id);

    long countByEstatus_Id(Long estatusId);

    // "Vehiculos registrados" del panel (fase 4): no cuenta los eliminados.
    long countByEliminadoEnIsNull();

    long countByCliente_Taller_IdAndEliminadoEnIsNull(Long tallerId);

    // Un solo SELECT con JOIN a cliente y estatus (paginado, filtros
    // opcionales parametrizados - ver maqueta "Vehiculos": solo consulta).
    @Query(
            value = """
                    SELECT v FROM Vehiculo v
                    JOIN FETCH v.cliente c
                    JOIN FETCH v.estatus
                    WHERE (:tallerId IS NULL OR c.taller.id = :tallerId)
                      AND (:estatusId IS NULL OR v.estatus.id = :estatusId)
                      AND (:busqueda IS NULL OR :busqueda = ''
                           OR UPPER(v.placa) LIKE UPPER(CONCAT(:busqueda, '%'))
                           OR UPPER(v.vin) LIKE UPPER(CONCAT('%', :busqueda, '%')))
                      AND v.eliminadoEn IS NULL
                    """,
            countQuery = """
                    SELECT COUNT(v) FROM Vehiculo v JOIN v.cliente c
                    WHERE (:tallerId IS NULL OR c.taller.id = :tallerId)
                      AND (:estatusId IS NULL OR v.estatus.id = :estatusId)
                      AND (:busqueda IS NULL OR :busqueda = ''
                           OR UPPER(v.placa) LIKE UPPER(CONCAT(:busqueda, '%'))
                           OR UPPER(v.vin) LIKE UPPER(CONCAT('%', :busqueda, '%')))
                      AND v.eliminadoEn IS NULL
                    """
    )
    Page<Vehiculo> buscar(@Param("tallerId") Long tallerId, @Param("estatusId") Long estatusId,
                           @Param("busqueda") String busqueda, Pageable pageable);
}
