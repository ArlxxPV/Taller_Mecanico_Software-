import http from '../services/http'

// Repository: cuentas de personal (no clientes). JSON puro, sin foto.
export const usuarioRepository = {
  listar() {
    return http.get('/usuarios')
  },
  crear(datos) {
    return http.post('/usuarios', datos)
  },
  cambiarRol(id, rolId, tallerId) {
    return http.put(`/usuarios/${id}/rol`, { rolId, tallerId })
  }
}
