package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.HistorialEstatusVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstatusVehiculoRepository extends JpaRepository<HistorialEstatusVehiculo, Long> {
    List<HistorialEstatusVehiculo> findByVehiculo_IdOrderByCreadoEnDesc(Long vehiculoId);
}
