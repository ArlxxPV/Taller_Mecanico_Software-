package com.tallerpro.backend.service;

import com.tallerpro.backend.dto.MovimientoPanelResponse;
import com.tallerpro.backend.dto.PanelResumenResponse;
import com.tallerpro.backend.model.ClavePermiso;
import com.tallerpro.backend.model.EstatusId;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.ClienteRepository;
import com.tallerpro.backend.repository.HistorialEstatusClienteRepository;
import com.tallerpro.backend.repository.VehiculoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Indicadores y "ultimos movimientos" del panel (fase 4). Mismo aislamiento
 * por taller que el resto de la app: sin CLIENTES_TODOS_TALLERES, todo se
 * acota al taller del actor.
 */
@Service
public class PanelService {

    private static final int TAMANIO_ULTIMOS_MOVIMIENTOS = 5;

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final HistorialEstatusClienteRepository historialRepository;
    private final PermisoService permisoService;

    public PanelService(ClienteRepository clienteRepository, VehiculoRepository vehiculoRepository,
                         HistorialEstatusClienteRepository historialRepository, PermisoService permisoService) {
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.historialRepository = historialRepository;
        this.permisoService = permisoService;
    }

    @Transactional(readOnly = true)
    public PanelResumenResponse resumen(Usuario actor) {
        Long tallerEfectivo = tallerEfectivo(actor);
        LocalDateTime inicioDeMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        if (tallerEfectivo != null) {
            return new PanelResumenResponse(
                    clienteRepository.countByTaller_IdAndEstatus_IdAndEliminadoEnIsNull(tallerEfectivo, EstatusId.ACTIVO),
                    clienteRepository.countByTaller_IdAndEstatus_IdAndEliminadoEnIsNull(tallerEfectivo, EstatusId.SUSPENDIDO),
                    vehiculoRepository.countByCliente_Taller_IdAndEliminadoEnIsNull(tallerEfectivo),
                    clienteRepository.countByTaller_IdAndCreadoEnGreaterThanEqualAndEliminadoEnIsNull(tallerEfectivo, inicioDeMes)
            );
        }
        return new PanelResumenResponse(
                clienteRepository.countByEstatus_IdAndEliminadoEnIsNull(EstatusId.ACTIVO),
                clienteRepository.countByEstatus_IdAndEliminadoEnIsNull(EstatusId.SUSPENDIDO),
                vehiculoRepository.countByEliminadoEnIsNull(),
                clienteRepository.countByCreadoEnGreaterThanEqualAndEliminadoEnIsNull(inicioDeMes)
        );
    }

    @Transactional(readOnly = true)
    public List<MovimientoPanelResponse> ultimosMovimientos(Usuario actor) {
        Long tallerEfectivo = tallerEfectivo(actor);
        return historialRepository.ultimosMovimientos(tallerEfectivo, PageRequest.of(0, TAMANIO_ULTIMOS_MOVIMIENTOS))
                .stream()
                .map(MovimientoPanelResponse::desde)
                .toList();
    }

    private Long tallerEfectivo(Usuario actor) {
        return permisoService.tiene(actor, ClavePermiso.CLIENTES_TODOS_TALLERES) ? null : actor.getTallerId();
    }
}
