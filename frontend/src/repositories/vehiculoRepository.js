import http from '../services/http'

export const vehiculoRepository = {
  asociar(clienteId, datos) {
    return http.post(`/clientes/${clienteId}/vehiculos`, datos)
  },
  // params: { estatusId, q, pagina, porPagina }, todos opcionales.
  listar(params) {
    return http.get('/vehiculos', { params })
  },
  verificarVin(vin) {
    return http.get('/vehiculos/verificar-vin', { params: { vin } })
  },
  verificarPlaca(placa) {
    return http.get('/vehiculos/verificar-placa', { params: { placa } })
  },
  listarPorCliente(clienteId) {
    return http.get(`/clientes/${clienteId}/vehiculos`)
  },
  obtener(id) {
    return http.get(`/vehiculos/${id}`)
  },
  actualizar(id, datos) {
    return http.put(`/vehiculos/${id}`, datos)
  },
  obtenerCliente(id) {
    return http.get(`/vehiculos/${id}/cliente`)
  },
  cambiarCliente(id, datos) {
    return http.patch(`/vehiculos/${id}/cliente`, datos)
  },
  desactivar(id) {
    return http.patch(`/vehiculos/${id}/desactivar`)
  },
  activar(id) {
    return http.patch(`/vehiculos/${id}/activar`)
  },
  historial(id) {
    return http.get(`/vehiculos/${id}/historial`)
  }
}
