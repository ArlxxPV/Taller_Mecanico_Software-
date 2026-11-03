package com.tallerpro.backend.service;

import com.tallerpro.backend.config.ArchivosProperties;
import com.tallerpro.backend.dto.AsignarAccesoRequest;
import com.tallerpro.backend.dto.ClienteRequest;
import com.tallerpro.backend.dto.ClienteResponse;
import com.tallerpro.backend.dto.ClienteSinAccesoResponse;
import com.tallerpro.backend.dto.CurpDuplicadaResponse;
import com.tallerpro.backend.dto.DireccionRequest;
import com.tallerpro.backend.dto.HistorialEstatusClienteResponse;
import com.tallerpro.backend.dto.PaginaClientesResponse;
import com.tallerpro.backend.dto.ResumenClientesResponse;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.ClavePermiso;
import com.tallerpro.backend.model.Cliente;
import com.tallerpro.backend.model.EstadoCliente;
import com.tallerpro.backend.model.Estatus;
import com.tallerpro.backend.model.EstatusId;
import com.tallerpro.backend.model.HistorialEstatusCliente;
import com.tallerpro.backend.model.Rol;
import com.tallerpro.backend.model.Taller;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.ClienteRepository;
import com.tallerpro.backend.repository.EstatusRepository;
import com.tallerpro.backend.repository.HistorialEstatusClienteRepository;
import com.tallerpro.backend.repository.RolRepository;
import com.tallerpro.backend.repository.TallerRepository;
import com.tallerpro.backend.repository.UsuarioRepository;
import com.tallerpro.backend.util.NormalizadorTexto;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private static final Set<String> TIPOS_FOTO_PERMITIDOS = Set.of("image/jpeg", "image/png");
    private static final Set<String> ORDENAR_POR_VALIDOS = Set.of("nombre", "creadoEn");
    private static final Set<String> DIRECCIONES_VALIDAS = Set.of("asc", "desc");
    private static final Set<Integer> TAMANIOS_PAGINA_VALIDOS = Set.of(10, 25, 50);

    private final ClienteRepository clienteRepository;
    private final TallerRepository tallerRepository;
    private final ArchivosProperties archivosProperties;
    private final PermisoService permisoService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EstatusRepository estatusRepository;
    private final HistorialEstatusClienteRepository historialRepository;
    private final ObjectMapper objectMapper;

    public ClienteService(ClienteRepository clienteRepository, TallerRepository tallerRepository,
                           ArchivosProperties archivosProperties, PermisoService permisoService,
                           UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           PasswordEncoder passwordEncoder, EstatusRepository estatusRepository,
                           HistorialEstatusClienteRepository historialRepository, ObjectMapper objectMapper) {
        this.clienteRepository = clienteRepository;
        this.tallerRepository = tallerRepository;
        this.archivosProperties = archivosProperties;
        this.permisoService = permisoService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.estatusRepository = estatusRepository;
        this.historialRepository = historialRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest datos, MultipartFile foto, Usuario actor) {
        if (clienteRepository.existsByEmailIgnoreCase(datos.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con ese correo.");
        }
        if (clienteRepository.existsByCurpIgnoreCase(datos.curp())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con esa CURP.");
        }
        validarCurpFechaNacimiento(datos.curp(), datos.fechaNacimiento());

        Taller taller = resolverTallerPermitido(datos.tallerId(), actor);

        Cliente cliente = new Cliente();
        aplicarDatos(cliente, datos, taller);
        cliente.setOrigenRegistro(origenRegistroPara(actor));
        cliente.setEstatus(estatusRepository.getReferenceById(EstatusId.ACTIVO));

        if (foto != null && !foto.isEmpty()) {
            cliente.setFotoUrl(guardarFoto(foto));
        }

        cliente = clienteRepository.save(cliente);
        registrarHistorial(cliente, null, EstatusId.ACTIVO, "Alta", null, actor);
        return ClienteResponse.desde(cliente);
    }

    @Transactional(readOnly = true)
    public PaginaClientesResponse listar(Usuario actor, Long tallerIdFiltro, Long estatusIdFiltro, String busqueda,
                                          String ordenarPor, String direccion, int pagina, int porPagina,
                                          boolean incluirEliminados) {
        if (!ORDENAR_POR_VALIDOS.contains(ordenarPor)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El criterio de orden no es valido. Usa 'nombre' o 'creadoEn'.");
        }
        if (!DIRECCIONES_VALIDAS.contains(direccion)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La direccion de orden no es valida. Usa 'asc' o 'desc'.");
        }
        if (pagina < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El numero de pagina no es valido.");
        }
        if (!TAMANIOS_PAGINA_VALIDOS.contains(porPagina)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Filas por pagina invalidas. Usa 10, 25 o 50.");
        }

        // "nombreCompleto" va al final como respaldo: los clientes autorregistrados
        // (AuthService.registrar(), ver D5 en registro_Fase02.md) y los que ya
        // existian antes de esta fase no tienen nombres/apellidos divididos, solo
        // nombreCompleto. Sin este respaldo, esos clientes quedan todos "empatados"
        // (NULL = NULL) y el orden asc/desc entre ellos no se nota.
        Sort.Direction dir = direccion.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort orden = ordenarPor.equals("nombre")
                ? Sort.by(dir, "apellidoPaterno", "apellidoMaterno", "nombres", "nombreCompleto")
                : Sort.by(dir, "creadoEn");
        Pageable pageable = PageRequest.of(pagina, porPagina, orden);

        // Sin CLIENTES_TODOS_TALLERES solo se ve el propio taller: se ignora
        // cualquier tallerId recibido y se fuerza el suyo (minimo privilegio en backend).
        Long tallerEfectivo = tallerIdFiltro;
        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            tallerEfectivo = actor.getTallerId();
        }

        // incluirEliminados solo se honra si de verdad tiene el permiso -
        // minimo privilegio en backend, no solo ocultarlo en la UI.
        boolean verEliminados = incluirEliminados && permisoService.tiene(actor, ClavePermiso.CLIENTES_ELIMINAR);
        String busquedaNormalizada = (busqueda == null || busqueda.isBlank()) ? null : busqueda.trim();

        Page<Cliente> pagina0 = clienteRepository.buscar(tallerEfectivo, estatusIdFiltro, verEliminados, busquedaNormalizada, pageable);
        return PaginaClientesResponse.desde(pagina0);
    }

    /** Conteos para las tarjetas de resumen (clicables), con el mismo aislamiento por taller que listar(). */
    @Transactional(readOnly = true)
    public ResumenClientesResponse resumen(Usuario actor, Long tallerIdFiltro) {
        Long tallerEfectivo = permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES) ? tallerIdFiltro : actor.getTallerId();
        boolean verEliminados = permisoService.tiene(actor, ClavePermiso.CLIENTES_ELIMINAR);

        if (tallerEfectivo != null) {
            return new ResumenClientesResponse(
                    clienteRepository.countByTaller_IdAndEliminadoEnIsNull(tallerEfectivo),
                    clienteRepository.countByTaller_IdAndEstatus_IdAndEliminadoEnIsNull(tallerEfectivo, EstatusId.ACTIVO),
                    clienteRepository.countByTaller_IdAndEstatus_IdAndEliminadoEnIsNull(tallerEfectivo, EstatusId.SUSPENDIDO),
                    verEliminados ? clienteRepository.countByTaller_IdAndEliminadoEnIsNotNull(tallerEfectivo) : 0
            );
        }
        return new ResumenClientesResponse(
                clienteRepository.countByEliminadoEnIsNull(),
                clienteRepository.countByEstatus_IdAndEliminadoEnIsNull(EstatusId.ACTIVO),
                clienteRepository.countByEstatus_IdAndEliminadoEnIsNull(EstatusId.SUSPENDIDO),
                verEliminados ? clienteRepository.countByEliminadoEnIsNotNull() : 0
        );
    }

    /** Para el chequeo "al salir del campo CURP" del formulario de registro (ver maqueta). */
    @Transactional(readOnly = true)
    public CurpDuplicadaResponse verificarCurp(String curp) {
        if (curp == null || curp.isBlank()) {
            return CurpDuplicadaResponse.ninguno();
        }
        return clienteRepository.findByCurpIgnoreCase(curp)
                .map(c -> new CurpDuplicadaResponse(true, c.getId(), c.getNombreCompleto()))
                .orElseGet(CurpDuplicadaResponse::ninguno);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id, Usuario actor) {
        return ClienteResponse.desde(buscarAccesibleOFallar(id, actor));
    }

    @Transactional(readOnly = true)
    public List<HistorialEstatusClienteResponse> historial(Long id, Usuario actor) {
        buscarAccesibleOFallar(id, actor);
        return historialRepository.buscarPorCliente(id).stream()
                .map(HistorialEstatusClienteResponse::desde)
                .toList();
    }

    /** Clientes dados de alta por personal (sin cuenta propia) que todavia no pueden iniciar sesion. */
    @Transactional(readOnly = true)
    public List<ClienteSinAccesoResponse> listarSinAcceso() {
        return clienteRepository.findByUsuarioIsNull().stream()
                .map(ClienteSinAccesoResponse::desde)
                .toList();
    }

    /**
     * Crea la cuenta (rol CLIENTE) que le faltaba a un cliente dado de alta
     * por personal, usando el correo con el que ya esta registrado, para que
     * pueda iniciar sesion con la contrasena que aqui se le asigna.
     */
    @Transactional
    public void asignarAcceso(Long id, AsignarAccesoRequest datos) {
        Cliente cliente = buscarOFallar(id);

        if (cliente.getUsuario() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Este cliente ya tiene una cuenta para iniciar sesion.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(cliente.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }

        Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "El rol CLIENTE no existe. Verifica las migraciones de Flyway."));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(cliente.getNombreCompleto());
        usuario.setEmail(cliente.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(datos.password()));
        usuario.setRoles(Set.of(rolCliente));
        usuario = usuarioRepository.save(usuario);

        cliente.setUsuario(usuario);
        clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest datos, MultipartFile foto, Usuario actor) {
        Cliente cliente = buscarAccesibleOFallar(id, actor);

        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(datos.email(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe otro cliente registrado con ese correo.");
        }
        if (clienteRepository.existsByCurpIgnoreCaseAndIdNot(datos.curp(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe otro cliente registrado con esa CURP.");
        }
        validarCurpFechaNacimiento(datos.curp(), datos.fechaNacimiento());

        Taller taller = resolverTallerPermitido(datos.tallerId(), actor);

        Map<String, String[]> cambios = diferencias(cliente, datos, taller);
        boolean hayFotoNueva = foto != null && !foto.isEmpty();
        if (cambios.isEmpty() && !hayFotoNueva) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No se detectaron cambios.");
        }

        aplicarDatos(cliente, datos, taller);
        cliente.setActualizadoEn(LocalDateTime.now());
        cliente.setActualizadoPor(actor);

        // La foto es opcional al editar: si no se manda una nueva, se conserva la que ya tenia.
        if (hayFotoNueva) {
            String fotoAnterior = cliente.getFotoUrl();
            cliente.setFotoUrl(guardarFoto(foto));
            if (fotoAnterior != null) {
                borrarFotoSiExiste(fotoAnterior);
            }
        }

        cliente = clienteRepository.save(cliente);

        if (!cambios.isEmpty()) {
            // "Anterior" no aplica aqui (editar no cambia el estatus del
            // cliente, solo sus datos): el movimiento ACTUALIZADO se
            // distingue por tener "cambios" poblado, no por una transicion
            // de estatus real (ver HistorialEstatusClienteResponse/maqueta).
            registrarHistorial(cliente, null, EstatusId.ACTUALIZADO, "Edicion de datos", aJson(cambios), actor);
        }

        return ClienteResponse.desde(cliente);
    }

    /** Endpoint anterior (toggle sin motivo): se deja intacto, ya no lo llama la pantalla nueva. */
    @Transactional
    public ClienteResponse cambiarEstatus(Long id, EstadoCliente nuevoEstatus) {
        // Sin chequeo de aislamiento por taller: solo quien tiene CLIENTES_SUSPENDER
        // llega aqui (ver SecurityConfig), y hoy ese permiso solo lo tiene ADMIN
        // (via esSistema), que administra todos los talleres.
        Cliente cliente = buscarOFallar(id);
        cliente.setEstadoCliente(nuevoEstatus);
        cliente = clienteRepository.save(cliente);
        return ClienteResponse.desde(cliente);
    }

    @Transactional
    public ClienteResponse suspender(Long id, String motivo, Usuario actor) {
        Cliente cliente = buscarAccesibleOFallar(id, actor);
        int filas = clienteRepository.cambiarEstatusSiEsta(id, EstatusId.SUSPENDIDO, EstatusId.ACTIVO, actor.getId());
        if (filas == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El cliente ya esta suspendido.");
        }
        registrarHistorial(cliente, EstatusId.ACTIVO, EstatusId.SUSPENDIDO, motivo, null, actor);
        return obtener(id, actor);
    }

    @Transactional
    public ClienteResponse reactivar(Long id, String comentario, Usuario actor) {
        Cliente cliente = buscarAccesibleOFallar(id, actor);
        int filas = clienteRepository.cambiarEstatusSiEsta(id, EstatusId.ACTIVO, EstatusId.SUSPENDIDO, actor.getId());
        if (filas == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El cliente no esta suspendido.");
        }
        String motivo = (comentario == null || comentario.isBlank()) ? null : comentario.trim();
        registrarHistorial(cliente, EstatusId.SUSPENDIDO, EstatusId.ACTIVO, motivo, null, actor);
        return obtener(id, actor);
    }

    /** Solo ADMIN llega aqui (ver SecurityConfig: PERM_CLIENTES_ELIMINAR), sin aislamiento por taller, igual que cambiarEstatus(). */
    @Transactional
    public ClienteResponse eliminar(Long id, String motivo, Usuario actor) {
        Cliente cliente = buscarOFallar(id);
        Long estatusAnteriorId = cliente.getEstatus().getId();
        int filas = clienteRepository.eliminarSiNoEliminado(id, EstatusId.ELIMINADO, actor.getId());
        if (filas == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El cliente ya esta eliminado.");
        }
        registrarHistorial(cliente, estatusAnteriorId, EstatusId.ELIMINADO, motivo, null, actor);
        return obtener(id, actor);
    }

    @Transactional
    public ClienteResponse restaurar(Long id, Usuario actor) {
        Cliente cliente = buscarOFallar(id);
        int filas = clienteRepository.restaurarSiEliminado(id, EstatusId.ACTIVO, actor.getId());
        if (filas == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Este cliente no esta eliminado.");
        }
        registrarHistorial(cliente, EstatusId.ELIMINADO, EstatusId.ACTIVO, "Restaurado", null, actor);
        return obtener(id, actor);
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

    /**
     * Igual que buscarOFallar, pero ademas aplica el aislamiento por taller
     * (una RECEPCIONISTA que pide un cliente de otro taller recibe el mismo
     * 404 que si no existiera) y, si el cliente esta eliminado, exige
     * CLIENTES_ELIMINAR (ver maqueta solo-admin) - en ambos casos 404, para
     * no revelar que el registro existe.
     */
    private Cliente buscarAccesibleOFallar(Long id, Usuario actor) {
        Cliente cliente = buscarOFallar(id);
        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerCliente = cliente.getTaller() != null ? cliente.getTaller().getId() : null;
            if (tallerCliente == null || !tallerCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado.");
            }
        }
        if (cliente.getEliminadoEn() != null && !permisoService.tiene(actor, ClavePermiso.CLIENTES_ELIMINAR)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado.");
        }
        return cliente;
    }

    /** ADMIN puede elegir cualquier taller; RECEPCIONISTA solo el suyo (validado en backend, no solo en UI). */
    private Taller resolverTallerPermitido(Long tallerId, Usuario actor) {
        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            if (actor.getTallerId() == null) {
                throw new ApiException(HttpStatus.CONFLICT, "Tu cuenta no tiene un taller asignado. Contacta al administrador.");
            }
            if (!actor.getTallerId().equals(tallerId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "No puedes registrar o mover clientes a otro taller.");
            }
        }
        return tallerRepository.findById(tallerId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El taller seleccionado no es valido."));
    }

    /** Nombre(s) de rol del actor, unidos por coma (ej. "RECEPCIONISTA" o "ADMIN, Supervisor"). */
    private String origenRegistroPara(Usuario actor) {
        return actor.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(", "));
    }

    private void aplicarDatos(Cliente cliente, ClienteRequest datos, Taller taller) {
        String nombres = NormalizadorTexto.aTitulo(datos.nombres());
        String apellidoPaterno = NormalizadorTexto.aTitulo(datos.apellidoPaterno());
        String apellidoMaterno = NormalizadorTexto.aTitulo(datos.apellidoMaterno());

        cliente.setNombres(nombres);
        cliente.setApellidoPaterno(apellidoPaterno);
        cliente.setApellidoMaterno(apellidoMaterno);
        cliente.setNombreCompleto(nombres + " " + apellidoPaterno + " " + apellidoMaterno);
        cliente.setCurp(NormalizadorTexto.aMayusculas(datos.curp()));
        cliente.setContactoAlternativo(NormalizadorTexto.aTitulo(datos.contactoAlternativo()));
        cliente.setFechaNacimiento(datos.fechaNacimiento());
        cliente.setTelefonoPersonal(datos.telefonoPersonal());
        cliente.setTelefonoTrabajo(datos.telefonoTrabajo());
        cliente.setEmail(NormalizadorTexto.aMinusculas(datos.email()));
        cliente.setEmailTrabajo(NormalizadorTexto.aMinusculas(datos.emailTrabajo()));
        cliente.setTaller(taller);

        DireccionRequest direccion = datos.direccion();
        cliente.setCalle(NormalizadorTexto.aTitulo(direccion.calle()));
        cliente.setColonia(NormalizadorTexto.aTitulo(direccion.colonia()));
        cliente.setMunicipio(direccion.municipio());
        cliente.setEstadoDireccion(direccion.estado());
        cliente.setCodigoPostal(direccion.codigoPostal());
    }

    /** CURP trae la fecha de nacimiento codificada (posiciones 5-10, AAMMDD); deben coincidir. */
    private void validarCurpFechaNacimiento(String curp, LocalDate fechaNacimiento) {
        if (fechaNacimiento == null || curp == null || curp.length() < 10) {
            return;
        }
        String yyMMddCurp = curp.substring(4, 10);
        String yyMMddFecha = String.format("%02d%02d%02d",
                fechaNacimiento.getYear() % 100, fechaNacimiento.getMonthValue(), fechaNacimiento.getDayOfMonth());
        if (!yyMMddCurp.equalsIgnoreCase(yyMMddFecha)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento no coincide con la CURP.");
        }
    }

    /**
     * Compara el cliente tal como esta en BD contra los datos nuevos del
     * formulario y regresa solo los campos que de verdad cambiaron, como
     * {"Etiqueta": ["valor anterior", "valor nuevo"]} (ver maqueta, pestana
     * Historial: "Campo: anterior -> nuevo"). Vacio = nada cambio.
     */
    private Map<String, String[]> diferencias(Cliente antes, ClienteRequest despues, Taller tallerNuevo) {
        Map<String, String[]> cambios = new LinkedHashMap<>();
        agregarSiCambio(cambios, "Nombre(s)", antes.getNombres(), NormalizadorTexto.aTitulo(despues.nombres()));
        agregarSiCambio(cambios, "Apellido paterno", antes.getApellidoPaterno(), NormalizadorTexto.aTitulo(despues.apellidoPaterno()));
        agregarSiCambio(cambios, "Apellido materno", antes.getApellidoMaterno(), NormalizadorTexto.aTitulo(despues.apellidoMaterno()));
        agregarSiCambio(cambios, "CURP", antes.getCurp(), NormalizadorTexto.aMayusculas(despues.curp()));
        agregarSiCambio(cambios, "Contacto alternativo", antes.getContactoAlternativo(), NormalizadorTexto.aTitulo(despues.contactoAlternativo()));
        agregarSiCambio(cambios, "Fecha de nacimiento", formatoFecha(antes.getFechaNacimiento()), formatoFecha(despues.fechaNacimiento()));
        agregarSiCambio(cambios, "Telefono personal", antes.getTelefonoPersonal(), despues.telefonoPersonal());
        agregarSiCambio(cambios, "Telefono de trabajo", antes.getTelefonoTrabajo(), despues.telefonoTrabajo());
        agregarSiCambio(cambios, "Correo", antes.getEmail(), NormalizadorTexto.aMinusculas(despues.email()));
        agregarSiCambio(cambios, "Correo de trabajo", antes.getEmailTrabajo(), NormalizadorTexto.aMinusculas(despues.emailTrabajo()));
        agregarSiCambio(cambios, "Taller", antes.getTaller() != null ? antes.getTaller().getNombre() : null, tallerNuevo.getNombre());
        agregarSiCambio(cambios, "Calle", antes.getCalle(), NormalizadorTexto.aTitulo(despues.direccion().calle()));
        agregarSiCambio(cambios, "Colonia", antes.getColonia(), NormalizadorTexto.aTitulo(despues.direccion().colonia()));
        agregarSiCambio(cambios, "Municipio", antes.getMunicipio(), despues.direccion().municipio());
        agregarSiCambio(cambios, "Estado", antes.getEstadoDireccion(), despues.direccion().estado());
        agregarSiCambio(cambios, "Codigo postal", antes.getCodigoPostal(), despues.direccion().codigoPostal());
        return cambios;
    }

    private void agregarSiCambio(Map<String, String[]> mapa, String etiqueta, String anterior, String nuevo) {
        String a = (anterior == null || anterior.isBlank()) ? null : anterior;
        String n = (nuevo == null || nuevo.isBlank()) ? null : nuevo;
        if (!Objects.equals(a, n)) {
            mapa.put(etiqueta, new String[]{ a == null ? "(vacio)" : a, n == null ? "(vacio)" : n });
        }
    }

    private String formatoFecha(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private String aJson(Map<String, String[]> cambios) {
        try {
            return objectMapper.writeValueAsString(cambios);
        } catch (Exception e) {
            return null;
        }
    }

    private void registrarHistorial(Cliente cliente, Long estatusAnteriorId, Long estatusNuevoId, String motivo, String cambiosJson, Usuario actor) {
        Estatus nuevo = estatusRepository.getReferenceById(estatusNuevoId);
        HistorialEstatusCliente historial = new HistorialEstatusCliente();
        historial.setCliente(cliente);
        if (estatusAnteriorId != null) {
            historial.setEstatusAnterior(estatusRepository.getReferenceById(estatusAnteriorId));
        }
        historial.setEstatusNuevo(nuevo);
        historial.setMotivo(motivo);
        historial.setCambios(cambiosJson);
        historial.setUsuario(actor);
        historialRepository.save(historial);
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
