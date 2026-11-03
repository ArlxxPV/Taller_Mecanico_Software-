import { usuarioRepository } from '../repositories/usuarioRepository'

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

export const usuarioFacade = {
  async listar() {
    try {
      const { data: usuarios } = await usuarioRepository.listar()
      return { ok: true, usuarios }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de personal.')
    }
  },

  async crear(formulario) {
    try {
      const datos = {
        nombres: formulario.nombres,
        apellidoPaterno: formulario.apellidoPaterno,
        apellidoMaterno: formulario.apellidoMaterno,
        email: formulario.email,
        password: formulario.password,
        rolId: formulario.rolId !== '' ? Number(formulario.rolId) : null,
        tallerId: formulario.tallerId !== '' && formulario.tallerId !== null ? Number(formulario.tallerId) : null
      }
      const { data: usuario } = await usuarioRepository.crear(datos)
      return { ok: true, mensaje: 'La cuenta se creo correctamente.', usuario }
    } catch (e) {
      return extraerError(e, 'No se pudo crear la cuenta.')
    }
  },

  async cambiarRol(id, rolId, tallerId) {
    try {
      const { data: usuario } = await usuarioRepository.cambiarRol(
        id,
        rolId !== '' ? Number(rolId) : null,
        tallerId !== '' && tallerId !== null ? Number(tallerId) : null
      )
      return { ok: true, mensaje: 'El rol se actualizo correctamente.', usuario }
    } catch (e) {
      return extraerError(e, 'No se pudo actualizar el rol.')
    }
  }
}
