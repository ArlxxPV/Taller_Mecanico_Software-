import http from '../services/http'

// Repository: unico lugar que sabe la ruta y la forma exacta de cada
// peticion REST de autenticacion. No conoce Pinia ni las vistas, solo API.
export const authRepository = {
  registrar(datos) {
    return http.post('/auth/registro', datos)
  },
  login(credenciales) {
    return http.post('/auth/login', credenciales)
  },
  olvidePassword(email) {
    return http.post('/auth/olvide-password', { email })
  },
  restablecerPassword(token, nuevaPassword) {
    return http.post('/auth/restablecer-password', { token, nuevaPassword })
  },
  obtenerPerfil() {
    return http.get('/usuarios/yo')
  }
}
