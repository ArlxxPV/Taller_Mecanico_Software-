import http from '../services/http'

// Repository: pide los catalogos de direccion (estados/municipios/colonias)
// al proxy propio del backend (nunca directo a COPOMEX, ver DireccionController
// en el backend). Mismo cliente http (axios) que usa clienteRepository.
export const direccionRepository = {
  obtenerEstados() {
    return http.get('/clientes/direcciones/estados')
  },
  obtenerMunicipios(estado) {
    return http.get('/clientes/direcciones/municipios', { params: { estado } })
  },
  obtenerColonias(estado, municipio) {
    return http.get('/clientes/direcciones/colonias', { params: { estado, municipio } })
  }
}
