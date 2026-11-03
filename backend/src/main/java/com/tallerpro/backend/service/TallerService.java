package com.tallerpro.backend.service;

import com.tallerpro.backend.config.TallerArchivosProperties;
import com.tallerpro.backend.dto.DireccionRequest;
import com.tallerpro.backend.dto.TallerRequest;
import com.tallerpro.backend.dto.TallerResponse;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.Taller;
import com.tallerpro.backend.repository.TallerRepository;
import com.tallerpro.backend.util.NormalizadorTexto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TallerService {

    private static final Set<String> TIPOS_FOTO_PERMITIDOS = Set.of("image/jpeg", "image/png");

    private final TallerRepository tallerRepository;
    private final TallerArchivosProperties archivosProperties;

    public TallerService(TallerRepository tallerRepository, TallerArchivosProperties archivosProperties) {
        this.tallerRepository = tallerRepository;
        this.archivosProperties = archivosProperties;
    }

    @Transactional
    public TallerResponse crear(TallerRequest datos, MultipartFile foto) {
        if (tallerRepository.existsByNombreIgnoreCase(datos.nombre())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un taller registrado con ese nombre.");
        }
        if (tallerRepository.existsByRfcIgnoreCase(datos.rfc())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un taller registrado con ese RFC.");
        }

        Taller taller = new Taller();
        aplicarDatos(taller, datos);

        if (foto != null && !foto.isEmpty()) {
            taller.setFotoUrl(guardarFoto(foto));
        }

        taller = tallerRepository.save(taller);
        return TallerResponse.desde(taller);
    }

    @Transactional(readOnly = true)
    public List<TallerResponse> listar() {
        return tallerRepository.findAllByOrderByNombreAsc().stream()
                .map(TallerResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public TallerResponse obtener(Long id) {
        return TallerResponse.desde(buscarOFallar(id));
    }

    @Transactional
    public TallerResponse actualizar(Long id, TallerRequest datos, MultipartFile foto) {
        Taller taller = buscarOFallar(id);

        if (tallerRepository.existsByNombreIgnoreCaseAndIdNot(datos.nombre(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe otro taller registrado con ese nombre.");
        }
        if (tallerRepository.existsByRfcIgnoreCaseAndIdNot(datos.rfc(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe otro taller registrado con ese RFC.");
        }

        aplicarDatos(taller, datos);

        if (foto != null && !foto.isEmpty()) {
            String fotoAnterior = taller.getFotoUrl();
            taller.setFotoUrl(guardarFoto(foto));
            if (fotoAnterior != null) {
                borrarFotoSiExiste(fotoAnterior);
            }
        }

        taller = tallerRepository.save(taller);
        return TallerResponse.desde(taller);
    }

    @Transactional(readOnly = true)
    public Path obtenerRutaFoto(Long id) {
        Taller taller = buscarOFallar(id);
        if (taller.getFotoUrl() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Este taller no tiene una foto registrada.");
        }
        return Path.of(archivosProperties.directorioFotosTalleres()).resolve(taller.getFotoUrl());
    }

    private Taller buscarOFallar(Long id) {
        return tallerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el taller solicitado."));
    }

    private void aplicarDatos(Taller taller, TallerRequest datos) {
        taller.setNombre(NormalizadorTexto.aTitulo(datos.nombre()));
        taller.setRazonSocial(NormalizadorTexto.aTitulo(datos.razonSocial()));
        taller.setTelefono(datos.telefono());
        taller.setRfc(NormalizadorTexto.aMayusculas(datos.rfc()));
        taller.setEmailContacto(NormalizadorTexto.aMinusculas(datos.emailContacto()));

        DireccionRequest direccion = datos.direccion();
        taller.setCalle(NormalizadorTexto.aTitulo(direccion.calle()));
        taller.setColonia(NormalizadorTexto.aTitulo(direccion.colonia()));
        taller.setMunicipio(direccion.municipio());
        taller.setEstadoDireccion(direccion.estado());
        taller.setCodigoPostal(direccion.codigoPostal());
    }

    private String guardarFoto(MultipartFile foto) {
        String tipo = foto.getContentType();
        if (tipo == null || !TIPOS_FOTO_PERMITIDOS.contains(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La foto debe ser un archivo JPG o PNG.");
        }
        if (foto.getSize() > archivosProperties.fotoMaxBytes()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La foto no debe superar 15 MB.");
        }

        String extension = tipo.equals("image/png") ? ".png" : ".jpg";
        String nombreArchivo = UUID.randomUUID() + extension;

        try {
            Path directorio = Path.of(archivosProperties.directorioFotosTalleres());
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreArchivo);
            foto.transferTo(destino);
            return nombreArchivo;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto. Intenta de nuevo.");
        }
    }

    private void borrarFotoSiExiste(String nombreArchivo) {
        try {
            Files.deleteIfExists(Path.of(archivosProperties.directorioFotosTalleres()).resolve(nombreArchivo));
        } catch (IOException e) {
            // No es fatal: queda un archivo huerfano en disco, no afecta el funcionamiento.
        }
    }
}
