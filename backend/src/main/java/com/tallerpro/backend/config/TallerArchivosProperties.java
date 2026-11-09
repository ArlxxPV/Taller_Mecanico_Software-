package com.tallerpro.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tallerpro.talleres")
public record TallerArchivosProperties(long fotoMaxBytes) {
}
