package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.NombreRol;
import com.tallerpro.backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(NombreRol nombre);
}
