package com.tallerpro.backend.service;

import com.tallerpro.backend.config.ArchivosProperties;
import com.tallerpro.backend.dto.ClienteRequest;
import com.tallerpro.backend.dto.ClienteResponse;
import com.tallerpro.backend.dto.DireccionRequest;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.Cliente;
import com.tallerpro.backend.repository.ClienteRepository;
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
public class ClienteService {

    private static final Set<String> TIPOS_FOTO_PERMITIDOS = Set.of("image/jpeg", "image/png");

    private final ClienteRepository clienteRepository;
    private final ArchivosProperties archivosProperties;

    public ClienteService(ClienteRepository clienteRepository, ArchivosProperties archivosProperties) {
        this.clienteRepository = clienteRepository;
        this.archivosProperties = archivosProperties;
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest datos, MultipartFile foto) {
        if (clienteRepository.existsByEmailIgnoreCase(datos.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con ese correo.");
        }

        Cliente cliente = new Cliente();
        aplicarDatos(cliente, datos);

        if (foto != null && !foto.isEmpty()) {
            cliente.setFotoUrl(guardarFoto(foto));
        }

        cliente = clienteRepository.save(cliente);
        return ClienteResponse.desde(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAllByOrderByCreadoEnDesc().stream()
                .map(ClienteResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return ClienteResponse.desde(buscarOFallar(id));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest datos, MultipartFile foto) {
        Cliente cliente = buscarOFallar(id);

        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(datos.email(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe otro cliente registrado con ese correo.");
        }

        aplicarDatos(cliente, datos);

        // La foto es opcional al editar: si no se manda una nueva, se conserva la que ya tenia.
        if (foto != null && !foto.isEmpty()) {
            String fotoAnterior = cliente.getFotoUrl();
            cliente.setFotoUrl(guardarFoto(foto));
            if (fotoAnterior != null) {
                borrarFotoSiExiste(fotoAnterior);
            }
        }

        cliente = clienteRepository.save(cliente);
        return ClienteResponse.desde(cliente);
    }

    /**
     * Ruta en disco de la foto de un cliente, para que el controller la
     * transmita. Lanza 404 si el cliente no existe o no tiene foto guardada.
     */
    @Transactional(readOnly = true)
    public Path obtenerRutaFoto(Long id) {
        Cliente cliente = buscarOFallar(id);
        if (cliente.getFotoUrl() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Este cliente no tiene una foto registrada.");
        }
        return Path.of(archivosProperties.directorioFotosClientes()).resolve(cliente.getFotoUrl());
    }

    private Cliente buscarOFallar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado."));
    }

    private void aplicarDatos(Cliente cliente, ClienteRequest datos) {
        cliente.setNombreCompleto(datos.nombreCompleto());
        cliente.setContactoAlternativo(datos.contactoAlternativo());
        cliente.setEdad(datos.edad());
        cliente.setFechaNacimiento(datos.fechaNacimiento());
        cliente.setTelefonoPersonal(datos.telefonoPersonal());
        cliente.setTelefonoTrabajo(datos.telefonoTrabajo());
        cliente.setEmail(datos.email());
        cliente.setEmailTrabajo(datos.emailTrabajo());

        DireccionRequest direccion = datos.direccion();
        cliente.setCalle(direccion.calle());
        cliente.setColonia(direccion.colonia());
        cliente.setMunicipio(direccion.municipio());
        cliente.setEstadoDireccion(direccion.estado());
        cliente.setCodigoPostal(direccion.codigoPostal());
    }

    private String guardarFoto(MultipartFile foto) {
        String tipo = foto.getContentType();
        if (tipo == null || !TIPOS_FOTO_PERMITIDOS.contains(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La foto debe ser un archivo JPG o PNG.");
        }
        if (foto.getSize() > archivosProperties.fotoMaxBytes()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La foto no debe superar 12 MB.");
        }

        String extension = tipo.equals("image/png") ? ".png" : ".jpg";
        String nombreArchivo = UUID.randomUUID() + extension;

        try {
            Path directorio = Path.of(archivosProperties.directorioFotosClientes());
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
            Files.deleteIfExists(Path.of(archivosProperties.directorioFotosClientes()).resolve(nombreArchivo));
        } catch (IOException e) {
            // No es un error fatal: el cliente ya se actualizo bien, solo queda un
            // archivo huerfano en disco que no afecta el funcionamiento de la app.
        }
    }
}
