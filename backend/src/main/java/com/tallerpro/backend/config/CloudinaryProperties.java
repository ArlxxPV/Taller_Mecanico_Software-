package com.tallerpro.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * url: la "API environment variable" de Cloudinary (cloudinary://key:secret@cloud).
 * Viene de la variable de entorno CLOUDINARY_URL; nunca se escribe en el repositorio.
 * carpeta: carpeta raiz dentro de Cloudinary donde se agrupan las fotos de la app.
 */
@ConfigurationProperties(prefix = "tallerpro.cloudinary")
public record CloudinaryProperties(String url, String carpeta) {
}
