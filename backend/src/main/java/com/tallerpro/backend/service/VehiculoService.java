package com.tallerpro.backend.service;

import com.tallerpro.backend.dto.CambiarClienteVehiculoRequest;
import com.tallerpro.backend.dto.ClienteResponse;
import com.tallerpro.backend.dto.DisponibilidadResponse;
import com.tallerpro.backend.dto.HistorialEstatusVehiculoResponse;
import com.tallerpro.backend.dto.PaginaVehiculosResponse;
import com.tallerpro.backend.dto.VehiculoClienteHistorialResponse;
import com.tallerpro.backend.dto.VehiculoRequest;
import com.tallerpro.backend.dto.VehiculoResponse;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.ClavePermiso;
import com.tallerpro.backend.model.Cliente;
import com.tallerpro.backend.model.Estatus;
import com.tallerpro.backend.model.EstatusId;
import com.tallerpro.backend.model.HistorialEstatusVehiculo;
import com.tallerpro.backend.model.RelacionClienteVehiculo;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.model.Vehiculo;
import com.tallerpro.backend.repository.ClienteRepository;
import com.tallerpro.backend.repository.EstatusRepository;
import com.tallerpro.backend.repository.HistorialEstatusVehiculoRepository;
import com.tallerpro.backend.repository.RelacionClienteVehiculoRepository;
import com.tallerpro.backend.repository.VehiculoRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class VehiculoService {

    private static final int ANIO_MINIMO = 1950;
    private static final Set<Integer> TAMANIOS_PAGINA_VALIDOS = Set.of(10, 25, 50);

    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;
    private final EstatusRepository estatusRepository;
    private final PermisoService permisoService;
    private final RelacionClienteVehiculoRepository relacionRepository;
    private final ClienteService clienteService;
    private final HistorialEstatusVehiculoRepository historialRepository;
    private final ObjectMapper objectMapper;

    public VehiculoService(VehiculoRepository vehiculoRepository, ClienteRepository clienteRepository,
                            EstatusRepository estatusRepository, PermisoService permisoService,
                            RelacionClienteVehiculoRepository relacionRepository, ClienteService clienteService,
                            HistorialEstatusVehiculoRepository historialRepository, ObjectMapper objectMapper) {
        this.vehiculoRepository = vehiculoRepository;
        this.clienteRepository = clienteRepository;
        this.estatusRepository = estatusRepository;
        this.permisoService = permisoService;
        this.relacionRepository = relacionRepository;
        this.clienteService = clienteService;
        this.historialRepository = historialRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VehiculoResponse crear(Long clienteId, VehiculoRequest datos, Usuario actor) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado."));

        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerCliente = cliente.getTaller() != null ? cliente.getTaller().getId() : null;
            if (tallerCliente == null || !tallerCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado.");
            }
        }
        if (cliente.getEliminadoEn() != null || !cliente.getEstatus().getId().equals(EstatusId.ACTIVO)) {
            throw new ApiException(HttpStatus.CONFLICT, "El cliente debe estar activo para asociar un vehiculo.");
        }

        int anioMaximo = Year.now().getValue() + 1;
        if (datos.anio() < ANIO_MINIMO || datos.anio() > anioMaximo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El anio debe estar entre " + ANIO_MINIMO + " y " + anioMaximo + ".");
        }
        if (vehiculoRepository.existsByVinIgnoreCase(datos.vin())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un vehiculo registrado con ese VIN.");
        }
        if (vehiculoRepository.existsByPlacaIgnoreCase(datos.placa())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un vehiculo registrado con esa placa.");
        }

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setCliente(cliente);
        vehiculo.setVin(datos.vin());
        vehiculo.setNumeroSerie(datos.numeroSerie());
        vehiculo.setPlaca(datos.placa());
        vehiculo.setMarca(datos.marca());
        vehiculo.setModelo(datos.modelo());
        vehiculo.setVersion(datos.version());
        vehiculo.setAnio((short) (int) datos.anio());
        vehiculo.setColor(datos.color());
        vehiculo.setKilometraje(datos.kilometraje());
        vehiculo.setMarcaApiId(datos.marcaApiId());
        vehiculo.setModeloApiId(datos.modeloApiId());
        vehiculo.setEstatus(estatusRepository.getReferenceById(EstatusId.ACTIVO));
        vehiculo.setCreadoPor(actor);

        vehiculo = vehiculoRepository.save(vehiculo);

        // UC-CV-11: la asociacion queda auditada como la primera relacion vigente.
        RelacionClienteVehiculo relacion = new RelacionClienteVehiculo();
        relacion.setVehiculo(vehiculo);
        relacion.setCliente(cliente);
        relacion.setVigente(true);
        relacion.setFechaInicio(vehiculo.getCreadoEn());
        relacion.setUsuario(actor);
        relacionRepository.save(relacion);

        return VehiculoResponse.desde(vehiculo);
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtener(Long id, Usuario actor) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .filter(v -> v.getEliminadoEn() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado."));

        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerCliente = vehiculo.getCliente().getTaller() != null ? vehiculo.getCliente().getTaller().getId() : null;
            if (tallerCliente == null || !tallerCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado.");
            }
        }
        return VehiculoResponse.desde(vehiculo);
    }

    @Transactional
    public VehiculoResponse actualizar(Long id, VehiculoRequest datos, Usuario actor) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .filter(v -> v.getEliminadoEn() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado."));

        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerCliente = vehiculo.getCliente().getTaller() != null ? vehiculo.getCliente().getTaller().getId() : null;
            if (tallerCliente == null || !tallerCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado.");
            }
        }

        int anioMaximo = Year.now().getValue() + 1;
        if (datos.anio() < ANIO_MINIMO || datos.anio() > anioMaximo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El anio debe estar entre " + ANIO_MINIMO + " y " + anioMaximo + ".");
        }
        if (vehiculoRepository.existsByVinIgnoreCaseAndIdNot(datos.vin(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un vehiculo registrado con ese VIN.");
        }
        if (vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot(datos.placa(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un vehiculo registrado con esa placa.");
        }

        Map<String, String[]> cambios = diferencias(vehiculo, datos);
        if (cambios.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No se detectaron cambios.");
        }

        vehiculo.setVin(datos.vin());
        vehiculo.setNumeroSerie(datos.numeroSerie());
        vehiculo.setPlaca(datos.placa());
        vehiculo.setMarca(datos.marca());
        vehiculo.setModelo(datos.modelo());
        vehiculo.setVersion(datos.version());
        vehiculo.setAnio((short) (int) datos.anio());
        vehiculo.setColor(datos.color());
        vehiculo.setKilometraje(datos.kilometraje());
        vehiculo.setMarcaApiId(datos.marcaApiId());
        vehiculo.setModeloApiId(datos.modeloApiId());
        vehiculo.setActualizadoEn(LocalDateTime.now());

        vehiculo = vehiculoRepository.save(vehiculo);

        // Edicion de datos, no una transicion real de estatus: usa el estatus de
        // sistema ACTUALIZADO como marcador (mismo patron que ClienteService.actualizar,
        // no altera vehiculo.estatus). Nunca toca ordenes (el proyecto no tiene ese modulo
        // todavia): esta fila es auditoria de la ficha del vehiculo unicamente.
        registrarHistorial(vehiculo, null, EstatusId.ACTUALIZADO, "Edicion de datos", aJson(cambios), actor);

        return VehiculoResponse.desde(vehiculo);
    }

    private Map<String, String[]> diferencias(Vehiculo antes, VehiculoRequest despues) {
        Map<String, String[]> cambios = new LinkedHashMap<>();
        agregarSiCambio(cambios, "VIN", antes.getVin(), despues.vin());
        agregarSiCambio(cambios, "Numero de serie", antes.getNumeroSerie(), despues.numeroSerie());
        agregarSiCambio(cambios, "Placa", antes.getPlaca(), despues.placa());
        agregarSiCambio(cambios, "Anio", String.valueOf(antes.getAnio()), String.valueOf(despues.anio()));
        agregarSiCambio(cambios, "Marca", antes.getMarca(), despues.marca());
        agregarSiCambio(cambios, "Modelo", antes.getModelo(), despues.modelo());
        agregarSiCambio(cambios, "Version", antes.getVersion(), despues.version());
        agregarSiCambio(cambios, "Color", antes.getColor(), despues.color());
        agregarSiCambio(cambios, "Kilometraje",
                antes.getKilometraje() != null ? String.valueOf(antes.getKilometraje()) : null,
                despues.kilometraje() != null ? String.valueOf(despues.kilometraje()) : null);
        return cambios;
    }

    private void agregarSiCambio(Map<String, String[]> mapa, String etiqueta, String anterior, String nuevo) {
        String a = (anterior == null || anterior.isBlank()) ? null : anterior;
        String n = (nuevo == null || nuevo.isBlank()) ? null : nuevo;
        if (!Objects.equals(a, n)) {
            mapa.put(etiqueta, new String[]{ a == null ? "(vacio)" : a, n == null ? "(vacio)" : n });
        }
    }

    private String aJson(Map<String, String[]> cambios) {
        try {
            return objectMapper.writeValueAsString(cambios);
        } catch (Exception e) {
            return null;
        }
    }

    private void registrarHistorial(Vehiculo vehiculo, Long estatusAnteriorId, Long estatusNuevoId, String motivo, String cambiosJson, Usuario actor) {
        Estatus nuevo = estatusRepository.getReferenceById(estatusNuevoId);
        HistorialEstatusVehiculo historial = new HistorialEstatusVehiculo();
        historial.setVehiculo(vehiculo);
        if (estatusAnteriorId != null) {
            historial.setEstatusAnterior(estatusRepository.getReferenceById(estatusAnteriorId));
        }
        historial.setEstatusNuevo(nuevo);
        historial.setMotivo(motivo);
        historial.setCambios(cambiosJson);
        historial.setUsuario(actor);
        historialRepository.save(historial);
    }

    /** UC-CV-13: vehiculos de un cliente, vigentes (su asociacion actual) e historicos (los que ya se le reasignaron a otro cliente). */
    @Transactional(readOnly = true)
    public List<VehiculoClienteHistorialResponse> listarPorCliente(Long clienteId, Usuario actor) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado."));

        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerCliente = cliente.getTaller() != null ? cliente.getTaller().getId() : null;
            if (tallerCliente == null || !tallerCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado.");
            }
        }

        return relacionRepository.findByCliente_IdOrderByFechaInicioDesc(clienteId).stream()
                .filter(r -> r.getVehiculo().getEliminadoEn() == null)
                .map(VehiculoClienteHistorialResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaVehiculosResponse listar(Usuario actor, Long estatusId, String busqueda, int pagina, int porPagina) {
        if (!TAMANIOS_PAGINA_VALIDOS.contains(porPagina)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Filas por pagina invalidas. Usa 10, 25 o 50.");
        }
        if (pagina < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El numero de pagina no es valido.");
        }

        Long tallerEfectivo = permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES) ? null : actor.getTallerId();
        String busquedaNormalizada = (busqueda == null || busqueda.isBlank()) ? null : busqueda.trim();
        Pageable pageable = PageRequest.of(pagina, porPagina, Sort.by(Sort.Direction.DESC, "creadoEn"));

        Page<Vehiculo> resultado = vehiculoRepository.buscar(tallerEfectivo, estatusId, busquedaNormalizada, pageable);
        return PaginaVehiculosResponse.desde(resultado);
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse verificarVin(String vin) {
        return new DisponibilidadResponse(vin == null || vin.isBlank() || !vehiculoRepository.existsByVinIgnoreCase(vin));
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse verificarPlaca(String placa) {
        return new DisponibilidadResponse(placa == null || placa.isBlank() || !vehiculoRepository.existsByPlacaIgnoreCase(placa));
    }

    /** Desactiva un vehiculo (deja de estar ACTIVO). Preserva el historial: solo cambia el estatus, nunca borra nada. */
    @Transactional
    public VehiculoResponse desactivar(Long id, Usuario actor) {
        Vehiculo vehiculo = buscarVisibleOFallar(id, actor);
        int filas = vehiculoRepository.cambiarEstatusSiEsta(id, EstatusId.SUSPENDIDO, EstatusId.ACTIVO);
        if (filas == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El vehiculo ya esta desactivado.");
        }
        registrarHistorial(vehiculo, EstatusId.ACTIVO, EstatusId.SUSPENDIDO, null, null, actor);
        return obtener(id, actor);
    }

    /** Reactiva un vehiculo previamente desactivado. */
    @Transactional
    public VehiculoResponse activar(Long id, Usuario actor) {
        Vehiculo vehiculo = buscarVisibleOFallar(id, actor);
        int filas = vehiculoRepository.cambiarEstatusSiEsta(id, EstatusId.ACTIVO, EstatusId.SUSPENDIDO);
        if (filas == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El vehiculo ya esta activo.");
        }
        registrarHistorial(vehiculo, EstatusId.SUSPENDIDO, EstatusId.ACTIVO, null, null, actor);
        return obtener(id, actor);
    }

    /** Historial completo del vehiculo (altas, ediciones, activaciones/desactivaciones). Nunca se borra una fila. */
    @Transactional(readOnly = true)
    public List<HistorialEstatusVehiculoResponse> historial(Long id, Usuario actor) {
        buscarVisibleOFallar(id, actor);
        return historialRepository.findByVehiculo_IdOrderByCreadoEnDesc(id).stream()
                .map(HistorialEstatusVehiculoResponse::desde)
                .toList();
    }

    private Vehiculo buscarVisibleOFallar(Long id, Usuario actor) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .filter(v -> v.getEliminadoEn() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado."));

        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerCliente = vehiculo.getCliente().getTaller() != null ? vehiculo.getCliente().getTaller().getId() : null;
            if (tallerCliente == null || !tallerCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado.");
            }
        }
        return vehiculo;
    }

    /** UC-CV-14: cliente vigente de un vehiculo (reusa ClienteService.obtener para respetar los mismos permisos/aislamiento). */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerClienteVigente(Long vehiculoId, Usuario actor) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .filter(v -> v.getEliminadoEn() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado."));

        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerCliente = vehiculo.getCliente().getTaller() != null ? vehiculo.getCliente().getTaller().getId() : null;
            if (tallerCliente == null || !tallerCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado.");
            }
        }
        return clienteService.obtener(vehiculo.getCliente().getId(), actor);
    }

    /**
     * UC-CV-12: cambia el cliente al que esta asociado un vehiculo.
     * R2 (solo una relacion vigente a la vez): cierra la relacion anterior antes de abrir la nueva, nunca coexisten dos vigentes.
     * R3: cambiar el cliente (el "usuario"/propietario del vehiculo) lo suspende automaticamente.
     * La relacion anterior nunca se borra, solo se marca vigente=false con fecha_fin y motivo (historial completo).
     */
    @Transactional
    public VehiculoResponse cambiarPropietario(Long vehiculoId, CambiarClienteVehiculoRequest datos, Usuario actor) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .filter(v -> v.getEliminadoEn() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado."));

        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerClienteActual = vehiculo.getCliente().getTaller() != null ? vehiculo.getCliente().getTaller().getId() : null;
            if (tallerClienteActual == null || !tallerClienteActual.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo solicitado.");
            }
        }

        if (datos.nuevoClienteId().equals(vehiculo.getCliente().getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "El vehiculo ya esta asociado a ese cliente.");
        }

        Cliente nuevoCliente = clienteRepository.findById(datos.nuevoClienteId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado."));
        if (!permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES)) {
            Long tallerNuevoCliente = nuevoCliente.getTaller() != null ? nuevoCliente.getTaller().getId() : null;
            if (tallerNuevoCliente == null || !tallerNuevoCliente.equals(actor.getTallerId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro el cliente solicitado.");
            }
        }
        if (nuevoCliente.getEliminadoEn() != null || !nuevoCliente.getEstatus().getId().equals(EstatusId.ACTIVO)) {
            throw new ApiException(HttpStatus.CONFLICT, "El cliente debe estar activo para asociarle un vehiculo.");
        }

        LocalDateTime ahora = LocalDateTime.now();

        relacionRepository.findByVehiculo_IdAndVigenteTrue(vehiculoId).ifPresent(relacionAnterior -> {
            relacionAnterior.setVigente(false);
            relacionAnterior.setFechaFin(ahora);
            relacionAnterior.setMotivo(datos.motivo());
            relacionRepository.save(relacionAnterior);
        });

        RelacionClienteVehiculo nuevaRelacion = new RelacionClienteVehiculo();
        nuevaRelacion.setVehiculo(vehiculo);
        nuevaRelacion.setCliente(nuevoCliente);
        nuevaRelacion.setVigente(true);
        nuevaRelacion.setFechaInicio(ahora);
        nuevaRelacion.setUsuario(actor);
        relacionRepository.save(nuevaRelacion);

        vehiculo.setCliente(nuevoCliente);
        vehiculo.setEstatus(estatusRepository.getReferenceById(EstatusId.SUSPENDIDO));
        vehiculo.setActualizadoEn(ahora);
        vehiculo = vehiculoRepository.save(vehiculo);

        return VehiculoResponse.desde(vehiculo);
    }
}
