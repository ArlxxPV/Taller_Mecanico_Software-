import { rolRepository } from '../repositories/rolRepository'

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

export const rolFacade = {
  async listar() {
    try {
      const { data: roles } = await rolRepository.listar()
      return { ok: true, roles }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de roles.')
    }
  },

  async permisosDisponibles() {
    try {
      const { data: permisos } = await rolRepository.permisosDisponibles()
      return { ok: true, permisos }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el catalogo de permisos.')
    }
  },

  async crear(nombre, descripcion, permisos) {
    try {
      const { data: rol } = await rolRepository.crear({ nombre, descripcion, permisos })
      return { ok: true, mensaje: 'El rol se creo correctamente.', rol }
    } catch (e) {
      return extraerError(e, 'No se pudo crear el rol.')
    }
  },

  async actualizarPermisos(id, permisos) {
    try {
      const { data: rol } = await rolRepository.actualizarPermisos(id, permisos)
      return { ok: true, mensaje: 'Los permisos del rol se actualizaron correctamente.', rol }
    } catch (e) {
      return extraerError(e, 'No se pudieron actualizar los permisos del rol.')
    }
  },

  async cambiarEstado(id, activo) {
    try {
      const { data: rol } = await rolRepository.cambiarEstado(id, activo)
      return { ok: true, mensaje: activo ? 'El rol se reactivo correctamente.' : 'El rol se suspendio correctamente.', rol }
    } catch (e) {
      return extraerError(e, 'No se pudo cambiar el estado del rol.')
    }
  }
}
