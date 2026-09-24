package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByCliente_Id(Long clienteId);
    boolean existsByPlacaIgnoreCase(String placa);
}
