package com.tallerpro.backend.service;

/**
 * Puerto de salida para enviar correos. Hoy solo existe ConsoleMailService
 * (registra el enlace en el log) porque no se nos dieron credenciales SMTP.
 * Para enviar correos reales: crea una clase que implemente esta interfaz
 * usando JavaMailSender (ya esta spring-boot-starter-mail en el pom.xml),
 * marcala @Primary o quita el @Service de ConsoleMailService, y agrega
 * spring.mail.host/username/password reales en application.yml.
 */
public interface MailService {
    void enviarCorreoRecuperacion(String destinatario, String nombreUsuario, String enlaceRecuperacion);
}
