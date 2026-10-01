import { authRepository } from '../repositories/authRepository'
import { useAuthStore } from '../stores/auth'

// Facade: unico punto de entrada que usan las Vistas para todo lo de cuentas.
// Combina el Repository (peticiones REST) con el Store (estado de sesion) y
// le entrega a la vista una respuesta ya lista para mostrar como alerta:
// { ok: boolean, mensaje: string, detalles?: string[] }

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

export const authFacade = {
  async registrar(datos) {
    try {
      const { data: usuario } = await authRepository.registrar(datos)
      return { ok: true, mensaje: 'Tu cuenta se creo correctamente. Ya puedes iniciar sesion.', usuario }
    } catch (e) {
      return extraerError(e, 'No se pudo crear la cuenta.')
    }
  },

  async login(credenciales) {
    try {
      const { data } = await authRepository.login(credenciales)
      const auth = useAuthStore()
      auth.establecerSesion(data.token, data.usuario)
      return { ok: true, mensaje: 'Sesion iniciada correctamente.', usuario: data.usuario }
    } catch (e) {
      return extraerError(e, 'No se pudo iniciar sesion. Intenta de nuevo.')
    }
  },

  async cargarPerfil() {
    const auth = useAuthStore()
    if (!auth.token) return { ok: false, mensaje: 'No hay sesion activa.' }
    try {
      const { data: usuario } = await authRepository.obtenerPerfil()
      auth.establecerUsuario(usuario)
      return { ok: true, usuario }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar tu perfil.')
    }
  },

  cerrarSesion() {
    const auth = useAuthStore()
    auth.limpiarSesion()
    return { ok: true, mensaje: 'Sesion cerrada correctamente.' }
  },

  async olvidePassword(email) {
    try {
      const { data } = await authRepository.olvidePassword(email)
      return { ok: true, mensaje: data.mensaje }
    } catch (e) {
      return extraerError(e, 'No se pudo procesar la solicitud. Intenta de nuevo en unos minutos.')
    }
  },

  async restablecerPassword(token, nuevaPassword) {
    try {
      const { data } = await authRepository.restablecerPassword(token, nuevaPassword)
      return { ok: true, mensaje: data.mensaje }
    } catch (e) {
      return extraerError(e, 'El enlace no es valido o ya expiro.')
    }
  }
}
