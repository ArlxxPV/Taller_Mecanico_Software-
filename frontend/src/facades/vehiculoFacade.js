import { vehiculoRepository } from '../repositories/vehiculoRepository'

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

export const vehiculoFacade = {
  async asociar(clienteId, datos) {
    try {
      const { data: vehiculo } = await vehiculoRepository.asociar(clienteId, datos)
      return { ok: true, mensaje: 'El vehiculo se registro y asocio correctamente.', vehiculo }
    } catch (e) {
      return extraerError(e, 'No se pudo registrar el vehiculo.')
    }
  },

  async obtener(id) {
    try {
      const { data: vehiculo } = await vehiculoRepository.obtener(id)
      return { ok: true, vehiculo }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el vehiculo.')
    }
  },

  async actualizar(id, datos) {
    try {
      const { data: vehiculo } = await vehiculoRepository.actualizar(id, datos)
      return { ok: true, mensaje: 'El vehiculo se actualizo correctamente.', vehiculo }
    } catch (e) {
      return extraerError(e, 'No se pudo actualizar el vehiculo.')
    }
  },

  // Cada elemento viene como { vehiculo, vigente, fechaInicio, fechaFin, motivo }:
  // vigente=true es la asociacion actual, vigente=false es un vehiculo que
  // este cliente tuvo antes y ya se reasigno a otro (UC-CV-13).
  async listarPorCliente(clienteId) {
    try {
      const { data: vehiculos } = await vehiculoRepository.listarPorCliente(clienteId)
      return { ok: true, vehiculos }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar los vehiculos del cliente.')
    }
  },

  async obtenerCliente(id) {
    try {
      const { data: cliente } = await vehiculoRepository.obtenerCliente(id)
      return { ok: true, cliente }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el cliente de este vehiculo.')
    }
  },

  async cambiarCliente(id, nuevoClienteId, motivo) {
    try {
      const { data: vehiculo } = await vehiculoRepository.cambiarCliente(id, { nuevoClienteId, motivo })
      return { ok: true, mensaje: 'El vehiculo se reasigno correctamente. Quedo suspendido.', vehiculo }
    } catch (e) {
      return extraerError(e, 'No se pudo cambiar el cliente del vehiculo.')
    }
  },

  async desactivar(id) {
    try {
      const { data: vehiculo } = await vehiculoRepository.desactivar(id)
      return { ok: true, mensaje: 'El vehiculo se desactivo correctamente.', vehiculo }
    } catch (e) {
      return extraerError(e, 'No se pudo desactivar el vehiculo.')
    }
  },

  async activar(id) {
    try {
      const { data: vehiculo } = await vehiculoRepository.activar(id)
      return { ok: true, mensaje: 'El vehiculo se activo correctamente.', vehiculo }
    } catch (e) {
      return extraerError(e, 'No se pudo activar el vehiculo.')
    }
  },

  async historial(id) {
    try {
      const { data: historial } = await vehiculoRepository.historial(id)
      return { ok: true, historial }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el historial del vehiculo.')
    }
  },

  async listar(filtros = {}) {
    try {
      const { data } = await vehiculoRepository.listar(filtros)
      return { ok: true, pagina: data }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de vehiculos.')
    }
  },

  // No usan el contrato {ok,...}: son chequeos silenciosos "al salir del
  // campo" (ver clienteFacade.verificarCurp, mismo patron). Si falla la red
  // se asume "disponible" para no bloquear al usuario por un problema ajeno.
  async verificarVin(vin) {
    try {
      const { data } = await vehiculoRepository.verificarVin(vin)
      return data.disponible
    } catch {
      return true
    }
  },

  async verificarPlaca(placa) {
    try {
      const { data } = await vehiculoRepository.verificarPlaca(placa)
      return data.disponible
    } catch {
      return true
    }
  }
}
