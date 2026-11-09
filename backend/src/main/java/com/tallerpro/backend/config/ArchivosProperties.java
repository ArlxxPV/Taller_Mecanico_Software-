package com.tallerpro.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tallerpro.archivos")
public record ArchivosProperties(long fotoMaxBytes) {
}
