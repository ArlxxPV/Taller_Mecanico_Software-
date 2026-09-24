import http from './http'

export const authService = {
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
