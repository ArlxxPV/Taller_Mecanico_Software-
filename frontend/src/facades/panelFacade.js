import { panelRepository } from '../repositories/panelRepository'

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

export const panelFacade = {
  async resumen() {
    try {
      const { data: resumen } = await panelRepository.resumen()
      return { ok: true, resumen }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el resumen del panel.')
    }
  },

  async ultimosMovimientos() {
    try {
      const { data: movimientos } = await panelRepository.ultimosMovimientos()
      return { ok: true, movimientos }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar los ultimos movimientos.')
    }
  }
}
