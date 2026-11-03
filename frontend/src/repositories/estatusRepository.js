import http from '../services/http'

export const estatusRepository = {
  listar(incluirBajas = false) {
    return http.get('/estatus', { params: { incluirBajas } })
  },
  obtener(id) {
    return http.get(`/estatus/${id}`)
  },
  crear(datos) {
    return http.post('/estatus', datos)
  },
  actualizar(id, datos) {
    return http.put(`/estatus/${id}`, datos)
  },
  darDeBaja(id) {
    return http.delete(`/estatus/${id}`)
  },
  restaurar(id) {
    return http.patch(`/estatus/${id}/restaurar`)
  }
}
