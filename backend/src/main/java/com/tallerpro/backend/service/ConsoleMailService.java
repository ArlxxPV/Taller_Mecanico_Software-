package com.tallerpro.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailService.class);

    @Override
    public void enviarCorreoRecuperacion(String destinatario, String nombreUsuario, String enlaceRecuperacion) {
        // TODO: reemplazar por un envio real via JavaMailSender cuando se tenga un SMTP.
        log.info("""

                ================= CORREO DE RECUPERACION (simulado) =================
                Para: {} ({})
                Enlace (valido por tiempo limitado): {}
                =======================================================================
                """, nombreUsuario, destinatario, enlaceRecuperacion);
    }
}
