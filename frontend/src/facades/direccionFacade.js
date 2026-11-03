import { direccionRepository } from '../repositories/direccionRepository'

function extraerError(e, mensajePorDefecto) {
  // El code_error/error_message crudo de COPOMEX solo existe en el backend
  // (que ya lo registro en su log); aqui dejamos constancia del fallo HTTP.
  console.error('Error al consultar el catalogo de direcciones:', e.response?.data || e.message)
  return { ok: false, mensaje: e.response?.data?.mensaje || mensajePorDefecto }
}

export const direccionFacade = {
  async obtenerEstados() {
    try {
      const { data } = await direccionRepository.obtenerEstados()
      return { ok: true, estados: data }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de estados.')
    }
  },

  async obtenerMunicipios(estado) {
    try {
      const { data } = await direccionRepository.obtenerMunicipios(estado)
      return { ok: true, municipios: data }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de municipios.')
    }
  },

  async obtenerColonias(estado, municipio) {
    try {
      const { data } = await direccionRepository.obtenerColonias(estado, municipio)
      return { ok: true, mapa: new Map(Object.entries(data)) }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de colonias.')
    }
  }
}
