package com.tallerpro.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tallerpro.reset-password")
public record ResetPasswordProperties(long minutosExpiracion, String urlFrontend) {
}
