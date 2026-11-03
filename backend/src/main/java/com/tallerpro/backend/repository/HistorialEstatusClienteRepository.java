package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.HistorialEstatusCliente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialEstatusClienteRepository extends JpaRepository<HistorialEstatusCliente, Long> {

    @Query("""
            SELECT h FROM HistorialEstatusCliente h
            JOIN FETCH h.estatusNuevo
            LEFT JOIN FETCH h.estatusAnterior
            LEFT JOIN FETCH h.usuario
            WHERE h.cliente.id = :clienteId
            ORDER BY h.creadoEn DESC, h.id DESC
            """)
    List<HistorialEstatusCliente> buscarPorCliente(@Param("clienteId") Long clienteId);

    // Para la tabla "Ultimos movimientos" del panel (fase 4): los N mas
    // recientes, acotados al taller del actor (null = todos, solo quien
    // tiene CLIENTES_TODOS_TALLERES).
    @Query("""
            SELECT h FROM HistorialEstatusCliente h
            JOIN FETCH h.estatusNuevo
            LEFT JOIN FETCH h.estatusAnterior
            LEFT JOIN FETCH h.usuario
            JOIN FETCH h.cliente c
            WHERE (:tallerId IS NULL OR c.taller.id = :tallerId)
            ORDER BY h.creadoEn DESC, h.id DESC
            """)
    List<HistorialEstatusCliente> ultimosMovimientos(@Param("tallerId") Long tallerId, Pageable pageable);
}
