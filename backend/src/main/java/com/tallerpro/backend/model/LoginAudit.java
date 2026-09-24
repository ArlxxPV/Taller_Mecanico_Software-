package com.tallerpro.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_audit")
@Getter
@Setter
@NoArgsConstructor
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "email_intento", nullable = false, length = 150)
    private String emailIntento;

    @Column(nullable = false)
    private boolean exitoso;

    @Column(name = "motivo_fallo", length = 100)
    private String motivoFallo;

    @Column(name = "ip_origen", length = 64)
    private String ipOrigen;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public static LoginAudit exitoso(Usuario usuario, String ip, String userAgent) {
        LoginAudit auditoria = new LoginAudit();
        auditoria.usuario = usuario;
        auditoria.emailIntento = usuario.getEmail();
        auditoria.exitoso = true;
        auditoria.ipOrigen = ip;
        auditoria.userAgent = userAgent;
        return auditoria;
    }

    public static LoginAudit fallido(String emailIntento, String motivo, String ip, String userAgent) {
        LoginAudit auditoria = new LoginAudit();
        auditoria.emailIntento = emailIntento;
        auditoria.exitoso = false;
        auditoria.motivoFallo = motivo;
        auditoria.ipOrigen = ip;
        auditoria.userAgent = userAgent;
        return auditoria;
    }
}
