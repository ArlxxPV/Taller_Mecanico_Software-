package com.tallerpro.backend.repository;

import com.tallerpro.backend.model.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
    List<LoginAudit> findTop10ByUsuario_IdOrderByCreadoEnDesc(Long usuarioId);
}
