package com.tallerpro.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tallerpro.jwt")
public record JwtProperties(String secret, long expiracionMinutos) {
}
