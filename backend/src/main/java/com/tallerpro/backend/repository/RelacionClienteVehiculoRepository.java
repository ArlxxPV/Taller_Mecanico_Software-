package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.RelacionClienteVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelacionClienteVehiculoRepository extends JpaRepository<RelacionClienteVehiculo, Long> {
    Optional<RelacionClienteVehiculo> findByVehiculo_IdAndVigenteTrue(Long vehiculoId);

    // Vigentes e historicas de un cliente (UC-CV-13), mas recientes primero.
    List<RelacionClienteVehiculo> findByCliente_IdOrderByFechaInicioDesc(Long clienteId);
}
