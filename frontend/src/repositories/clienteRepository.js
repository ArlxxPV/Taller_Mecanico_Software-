import http from '../services/http'

// Repository: unico lugar que arma la peticion REST de clientes. El backend
// espera multipart/form-data con dos partes: "datos" (JSON) y "foto" (archivo,
// opcional). Si en el futuro se agregan mas endpoints de clientes (listar,
// editar, asociar a un taller), van aqui, con la misma convencion.
export const clienteRepository = {
  crear(datos, foto) {
    const formData = new FormData()
    formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
    if (foto) {
      formData.append('foto', foto)
    }
    return http.post('/clientes', formData)
  },
  // params: { tallerId, estatusId, q, ordenarPor, direccion, pagina, porPagina, incluirEliminados }, todos opcionales.
  listar(params) {
    return http.get('/clientes', { params })
  },
  obtener(id) {
    return http.get(`/clientes/${id}`)
  },
  actualizar(id, datos, foto) {
    const formData = new FormData()
    formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
    if (foto) {
      formData.append('foto', foto)
    }
    return http.put(`/clientes/${id}`, formData)
  },
  cambiarEstatus(id, estadoCliente) {
    return http.patch(`/clientes/${id}/estatus`, { estadoCliente })
  },
  // params: { tallerId }, opcional.
  resumen(params) {
    return http.get('/clientes/resumen', { params })
  },
  obtenerFotoBlob(id) {
    return http.get(`/clientes/${id}/foto`, { responseType: 'blob' })
  },
  listarSinAcceso() {
    return http.get('/clientes/sin-acceso')
  },
  asignarAcceso(id, password) {
    return http.post(`/clientes/${id}/acceso`, { password })
  },
  historial(id) {
    return http.get(`/clientes/${id}/historial`)
  },
  suspender(id, motivo) {
    return http.patch(`/clientes/${id}/suspender`, { motivo })
  },
  reactivar(id, comentario) {
    return http.patch(`/clientes/${id}/reactivar`, { comentario: comentario || null })
  },
  eliminar(id, motivo) {
    return http.delete(`/clientes/${id}`, { data: { motivo } })
  },
  restaurar(id) {
    return http.patch(`/clientes/${id}/restaurar`)
  },
  verificarCurp(curp) {
    return http.get('/clientes/verificar-curp', { params: { curp } })
  }
}
