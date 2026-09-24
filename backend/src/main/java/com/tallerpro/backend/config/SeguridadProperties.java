package com.tallerpro.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tallerpro.seguridad")
public record SeguridadProperties(int maxIntentosFallidos, long minutosBloqueo) {
}
