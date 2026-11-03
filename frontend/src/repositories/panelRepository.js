import http from '../services/http'

export const panelRepository = {
  resumen() {
    return http.get('/panel/resumen')
  },
  ultimosMovimientos() {
    return http.get('/panel/ultimos-movimientos')
  }
}
