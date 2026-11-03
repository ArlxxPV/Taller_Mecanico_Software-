import { estatusRepository } from '../repositories/estatusRepository'

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

// Misma normalizacion/regex que EstatusService en el backend (la del
// backend es la que cuenta; esta solo evita un viaje de red con un error
// obvio y alimenta la vista previa del chip).
export function normalizarValor(valor) {
  return (valor || '').trim().toUpperCase().replace(/\s+/g, '_')
}

export function validarEstatus(valor, descripcion) {
  const errores = {}
  const valorNormalizado = normalizarValor(valor)
  if (!/^[A-Z_]{3,30}$/.test(valorNormalizado)) {
    errores.valor = 'Solo letras y guion bajo, de 3 a 30 caracteres.'
  }
  const largo = (descripcion || '').trim().length
  if (largo < 10 || largo > 255) {
    errores.descripcion = 'De 10 a 255 caracteres.'
  }
  return errores
}

export const estatusFacade = {
  async listar(incluirBajas = false) {
    try {
      const { data: estatus } = await estatusRepository.listar(incluirBajas)
      return { ok: true, estatus }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el catalogo de estatus.')
    }
  },

  async obtener(id) {
    try {
      const { data: estatus } = await estatusRepository.obtener(id)
      return { ok: true, estatus }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el estatus.')
    }
  },

  async crear(valor, descripcion) {
    try {
      const { data: estatus } = await estatusRepository.crear({ valor: normalizarValor(valor), descripcion })
      return { ok: true, mensaje: 'El estatus se guardo correctamente.', estatus }
    } catch (e) {
      return extraerError(e, 'No se pudo guardar el estatus.')
    }
  },

  async actualizar(id, valor, descripcion) {
    try {
      const { data: estatus } = await estatusRepository.actualizar(id, { valor: normalizarValor(valor), descripcion })
      return { ok: true, mensaje: 'Los cambios se guardaron correctamente.', estatus }
    } catch (e) {
      return extraerError(e, 'No se pudieron guardar los cambios.')
    }
  },

  async darDeBaja(id) {
    try {
      await estatusRepository.darDeBaja(id)
      return { ok: true, mensaje: 'El estatus se dio de baja correctamente.' }
    } catch (e) {
      return extraerError(e, 'No se pudo dar de baja el estatus.')
    }
  },

  async restaurar(id) {
    try {
      await estatusRepository.restaurar(id)
      return { ok: true, mensaje: 'El estatus se restauro correctamente.' }
    } catch (e) {
      return extraerError(e, 'No se pudo restaurar el estatus.')
    }
  }
}
