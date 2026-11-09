package com.tallerpro.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tallerpro.backend.config.CloudinaryProperties;
import com.tallerpro.backend.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda, entrega y borra las fotos de clientes y talleres en Cloudinary.
 *
 * Las fotos se suben como tipo "authenticated": Cloudinary no las sirve por una
 * URL publica adivinable. El backend las descarga con una URL firmada y las
 * transmite por GET /api/{clientes|talleres}/{id}/foto, que sigue pidiendo JWT
 * (mismo contrato que cuando vivian en disco; el frontend no cambia).
 *
 * En la columna foto_url se guarda el public_id de Cloudinary.
 */
@Service
public class AlmacenFotosService {

    private static final Logger log = LoggerFactory.getLogger(AlmacenFotosService.class);
    private static final String TIPO_ENTREGA = "authenticated";
    private static final String CARPETA_POR_DEFECTO = "tallerpro";

    /** null cuando CLOUDINARY_URL no esta configurada: la app arranca, solo fallan las fotos. */
    private final Cloudinary cloudinary;
    private final String carpetaRaiz;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public AlmacenFotosService(CloudinaryProperties properties) {
        String url = properties.url();
        if (url == null || url.isBlank()) {
            this.cloudinary = null;
            log.warn("CLOUDINARY_URL no esta configurada: subir o ver fotos respondera 503.");
        } else {
            this.cloudinary = new Cloudinary(url.trim());
            this.cloudinary.config.secure = true;
        }
        String carpeta = properties.carpeta();
        this.carpetaRaiz = (carpeta == null || carpeta.isBlank()) ? CARPETA_POR_DEFECTO : carpeta.trim();
    }

    /** Foto lista para responder por HTTP. */
    public record Foto(byte[] contenido, MediaType tipo) {
    }

    /**
     * Sube la foto (ya validada en tipo y tamano por quien llama) y devuelve su
     * public_id. Si la transaccion en curso se revierte despues, la foto se borra
     * de Cloudinary para no dejar archivos huerfanos.
     */
    public String subir(MultipartFile foto, String subcarpeta) {
        Cloudinary cliente = requerirConfigurado();
        String publicId = carpetaRaiz + "/" + subcarpeta + "/" + UUID.randomUUID();
        try {
            Map<?, ?> resultado = cliente.uploader().upload(foto.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "type", TIPO_ENTREGA,
                    "resource_type", "image",
                    "overwrite", false
            ));
            String guardado = (String) resultado.get("public_id");
            borrarSiSeRevierte(guardado);
            return guardado;
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo subir la foto a Cloudinary", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto. Intenta de nuevo.");
        }
    }

    /** Descarga la foto de Cloudinary con una URL firmada. 404 si ya no existe alla. */
    public Foto descargar(String publicId) {
        Cloudinary cliente = requerirConfigurado();
        String url = cliente.url()
                .type(TIPO_ENTREGA)
                .resourceType("image")
                .secure(true)
                .signed(true)
                .generate(publicId);

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> respuesta = httpClient.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
            if (respuesta.statusCode() == 404) {
                throw new ApiException(HttpStatus.NOT_FOUND, "La foto ya no esta disponible.");
            }
            if (respuesta.statusCode() != 200) {
                log.warn("Cloudinary respondio {} al descargar la foto {}", respuesta.statusCode(), publicId);
                throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo obtener la foto. Intenta de nuevo.");
            }
            MediaType tipo = respuesta.headers().firstValue("Content-Type")
                    .map(MediaType::parseMediaType)
                    .orElse(MediaType.IMAGE_JPEG);
            return new Foto(respuesta.body(), tipo);
        } catch (IOException e) {
            log.warn("Error de red al descargar la foto {} de Cloudinary", publicId, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo obtener la foto. Intenta de nuevo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo obtener la foto. Intenta de nuevo.");
        }
    }

    /**
     * Borra la foto anterior, pero solo cuando la transaccion en curso se confirma:
     * si el guardado falla, la fila sigue apuntando a una foto que todavia existe.
     */
    public void borrarAlConfirmar(String publicId) {
        if (publicId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    borrar(publicId);
                }
            });
        } else {
            borrar(publicId);
        }
    }

    private void borrarSiSeRevierte(String publicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    borrar(publicId);
                }
            }
        });
    }

    private void borrar(String publicId) {
        if (cloudinary == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "type", TIPO_ENTREGA,
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException e) {
            // No es fatal: el registro ya quedo bien; solo queda una foto huerfana en Cloudinary.
            log.warn("No se pudo borrar la foto {} de Cloudinary", publicId, e);
        }
    }

    private Cloudinary requerirConfigurado() {
        if (cloudinary == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El almacenamiento de fotos no esta configurado. Avisa al administrador.");
        }
        return cloudinary;
    }
}
