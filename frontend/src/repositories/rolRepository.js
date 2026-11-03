import http from '../services/http'

// Repository: catalogo de roles y permisos. JSON puro (sin multipart, no hay
// archivos involucrados en este modulo).
export const rolRepository = {
  listar() {
    return http.get('/roles')
  },
  permisosDisponibles() {
    return http.get('/roles/permisos-disponibles')
  },
  crear(datos) {
    return http.post('/roles', datos)
  },
  actualizarPermisos(id, permisos) {
    return http.put(`/roles/${id}/permisos`, { permisos })
  },
  cambiarEstado(id, activo) {
    return http.patch(`/roles/${id}/estado`, { activo })
  }
}
