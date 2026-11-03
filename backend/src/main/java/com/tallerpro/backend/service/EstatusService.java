package com.tallerpro.backend.service;

import com.tallerpro.backend.dto.EstatusRequest;
import com.tallerpro.backend.dto.EstatusResponse;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.Estatus;
import com.tallerpro.backend.repository.ClienteRepository;
import com.tallerpro.backend.repository.EstatusRepository;
import com.tallerpro.backend.repository.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * El catalogo nunca borra fisicamente (regla general del proyecto): dar de
 * baja es soft delete (eliminadoEn) y los 4 estatus de sistema (V10) jamas
 * se pueden dar de baja ni cambiar de valor, solo su descripcion.
 */
@Service
public class EstatusService {

    private static final Pattern VALOR_VALIDO = Pattern.compile("^[A-Z_]{3,30}$");

    private final EstatusRepository estatusRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;

    public EstatusService(EstatusRepository estatusRepository, ClienteRepository clienteRepository, VehiculoRepository vehiculoRepository) {
        this.estatusRepository = estatusRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional(readOnly = true)
    public List<EstatusResponse> listar(boolean incluirBajas) {
        List<Estatus> lista = incluirBajas ? estatusRepository.findAll() : estatusRepository.findByEliminadoEnIsNull();
        return lista.stream()
                .map(e -> EstatusResponse.desde(e, clienteRepository.contarPorEstatusId(e.getId()), vehiculoRepository.countByEstatus_Id(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EstatusResponse obtener(Long id) {
        Estatus estatus = buscarOFallar(id);
        return EstatusResponse.desde(estatus, clienteRepository.contarPorEstatusId(id), vehiculoRepository.countByEstatus_Id(id));
    }

    @Transactional
    public EstatusResponse crear(EstatusRequest datos) {
        String valor = normalizarValor(datos.valor());
        validarFormato(valor);
        if (estatusRepository.existsByValorIgnoreCase(valor)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe ese estatus.");
        }

        Estatus estatus = new Estatus();
        estatus.setValor(valor);
        estatus.setDescripcion(datos.descripcion());
        estatus.setEsSistema(false);
        estatus = estatusRepository.save(estatus);
        return EstatusResponse.desde(estatus, 0, 0);
    }

    @Transactional
    public EstatusResponse actualizar(Long id, EstatusRequest datos) {
        Estatus estatus = buscarOFallar(id);

        if (estatus.isEsSistema()) {
            // Los estatus de sistema solo permiten editar la descripcion
            // (ver maqueta "Editar estatus"): el valor queda bloqueado aunque
            // llegue distinto en la peticion.
            estatus.setDescripcion(datos.descripcion());
        } else {
            String valor = normalizarValor(datos.valor());
            validarFormato(valor);
            if (estatusRepository.existsByValorIgnoreCaseAndIdNot(valor, id)) {
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe ese estatus.");
            }
            estatus.setValor(valor);
            estatus.setDescripcion(datos.descripcion());
        }

        estatus.setActualizadoEn(LocalDateTime.now());
        estatus = estatusRepository.save(estatus);
        return EstatusResponse.desde(estatus, clienteRepository.contarPorEstatusId(id), vehiculoRepository.countByEstatus_Id(id));
    }

    @Transactional
    public void darDeBaja(Long id) {
        Estatus estatus = buscarOFallar(id);
        if (estatus.isEsSistema()) {
            throw new ApiException(HttpStatus.CONFLICT, "Los estatus del sistema no se pueden dar de baja.");
        }
        if (estatus.getEliminadoEn() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Este estatus ya esta dado de baja.");
        }
        long enUso = clienteRepository.contarPorEstatusId(id) + vehiculoRepository.countByEstatus_Id(id);
        if (enUso > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "En uso por " + enUso + " registro" + (enUso == 1 ? "" : "s") + ".");
        }
        estatus.setEliminadoEn(LocalDateTime.now());
        estatusRepository.save(estatus);
    }

    @Transactional
    public void restaurar(Long id) {
        Estatus estatus = buscarOFallar(id);
        if (estatus.getEliminadoEn() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Este estatus no esta dado de baja.");
        }
        estatus.setEliminadoEn(null);
        estatusRepository.save(estatus);
    }

    private Estatus buscarOFallar(Long id) {
        return estatusRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontro el estatus solicitado."));
    }

    private String normalizarValor(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    private void validarFormato(String valor) {
        if (!VALOR_VALIDO.matcher(valor).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El valor solo puede tener letras y guion bajo, de 3 a 30 caracteres.");
        }
    }
}
