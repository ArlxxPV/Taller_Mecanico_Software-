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
  listar() {
    return http.get('/clientes')
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
  obtenerFotoBlob(id) {
    return http.get(`/clientes/${id}/foto`, { responseType: 'blob' })
  }
}
