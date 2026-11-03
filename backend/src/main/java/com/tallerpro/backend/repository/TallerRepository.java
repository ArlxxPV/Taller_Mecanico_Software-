package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.Taller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TallerRepository extends JpaRepository<Taller, Long> {
    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByRfcIgnoreCase(String rfc);

    boolean existsByRfcIgnoreCaseAndIdNot(String rfc, Long id);

    List<Taller> findAllByOrderByNombreAsc();
}
