package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.Estatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstatusRepository extends JpaRepository<Estatus, Long> {
    List<Estatus> findByEliminadoEnIsNull();

    Optional<Estatus> findByValorIgnoreCase(String valor);

    boolean existsByValorIgnoreCase(String valor);

    boolean existsByValorIgnoreCaseAndIdNot(String valor, Long id);
}
