import http from '../services/http'

// Repository: igual patron que clienteRepository.js. Crear/actualizar viajan
// como multipart/form-data con dos partes: "datos" (JSON) y "foto" (opcional).
export const tallerRepository = {
  crear(datos, foto) {
    const formData = new FormData()
    formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
    if (foto) {
      formData.append('foto', foto)
    }
    return http.post('/talleres', formData)
  },
  listar() {
    return http.get('/talleres')
  },
  obtener(id) {
    return http.get(`/talleres/${id}`)
  },
  actualizar(id, datos, foto) {
    const formData = new FormData()
    formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
    if (foto) {
      formData.append('foto', foto)
    }
    return http.put(`/talleres/${id}`, formData)
  },
  obtenerFotoBlob(id) {
    return http.get(`/talleres/${id}/foto`, { responseType: 'blob' })
  }
}
