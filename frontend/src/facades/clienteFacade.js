import { clienteRepository } from '../repositories/clienteRepository'

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

// Mismo objeto que espera el backend (ClienteRequest) tanto para crear como
// para actualizar: junta los campos sueltos del formulario y arma la
// direccion anidada. La edad NO se envia: el backend la calcula siempre
// desde fechaNacimiento.
function armarDatos(formulario) {
  return {
    nombres: formulario.nombres,
    apellidoPaterno: formulario.apellidoPaterno,
    apellidoMaterno: formulario.apellidoMaterno,
    curp: formulario.curp,
    contactoAlternativo: formulario.contactoAlternativo,
    fechaNacimiento: formulario.fechaNacimiento || null,
    telefonoPersonal: formulario.telefonoPersonal,
    telefonoTrabajo: formulario.telefonoTrabajo,
    email: formulario.email,
    emailTrabajo: formulario.emailTrabajo,
    direccion: {
      calle: formulario.calle,
      colonia: formulario.colonia,
      municipio: formulario.municipio,
      estado: formulario.estado,
      codigoPostal: formulario.codigoPostal
    },
    tallerId: formulario.tallerId !== '' && formulario.tallerId !== null ? Number(formulario.tallerId) : null
  }
}

// Vuelca un ClienteResponse del backend sobre los campos sueltos que usa el
// formulario (lo inverso de armarDatos), para precargar la pantalla de editar.
function aFormulario(cliente) {
  return {
    nombres: cliente.nombres || '',
    apellidoPaterno: cliente.apellidoPaterno || '',
    apellidoMaterno: cliente.apellidoMaterno || '',
    curp: cliente.curp || '',
    contactoAlternativo: cliente.contactoAlternativo || '',
    fechaNacimiento: cliente.fechaNacimiento || '',
    telefonoPersonal: cliente.telefonoPersonal || '',
    telefonoTrabajo: cliente.telefonoTrabajo || '',
    email: cliente.email || '',
    emailTrabajo: cliente.emailTrabajo || '',
    calle: cliente.calle || '',
    colonia: cliente.colonia || '',
    municipio: cliente.municipio || '',
    estado: cliente.estado || '',
    codigoPostal: cliente.codigoPostal || '',
    tallerId: cliente.tallerId ?? ''
  }
}

export const clienteFacade = {
  async registrar(formulario, foto) {
    try {
      const { data: cliente } = await clienteRepository.crear(armarDatos(formulario), foto)
      return { ok: true, mensaje: 'El cliente se registro correctamente.', cliente }
    } catch (e) {
      return extraerError(e, 'No se pudo registrar el cliente.')
    }
  },

  // filtros: { tallerId, estadoCliente, ordenarPor, direccion, pagina }, todos opcionales.
  async listar(filtros = {}) {
    try {
      const { data } = await clienteRepository.listar(filtros)
      return { ok: true, pagina: data }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de clientes.')
    }
  },

  async obtener(id) {
    try {
      const { data: cliente } = await clienteRepository.obtener(id)
      return { ok: true, cliente, formulario: aFormulario(cliente) }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la informacion del cliente.')
    }
  },

  async actualizar(id, formulario, foto) {
    try {
      const { data: cliente } = await clienteRepository.actualizar(id, armarDatos(formulario), foto)
      return { ok: true, mensaje: 'Los datos del cliente se actualizaron correctamente.', cliente }
    } catch (e) {
      return extraerError(e, 'No se pudieron actualizar los datos del cliente.')
    }
  },

  async cambiarEstatus(id, estadoCliente) {
    try {
      const { data: cliente } = await clienteRepository.cambiarEstatus(id, estadoCliente)
      return { ok: true, mensaje: 'El estatus del cliente se actualizo correctamente.', cliente }
    } catch (e) {
      return extraerError(e, 'No se pudo actualizar el estatus del cliente.')
    }
  },

  // tallerId: opcional (filtro de la pantalla de lista; para RECEPCIONISTA el
  // backend ignora cualquier valor y siempre cuenta solo su propio taller).
  async resumen(tallerId) {
    try {
      const { data: resumen } = await clienteRepository.resumen({ tallerId: tallerId || undefined })
      return { ok: true, resumen }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el resumen de clientes.')
    }
  },

  // Descarga la foto (con el JWT, via el interceptor de http.js) y la
  // convierte en una URL que <img> puede usar. null si no hay foto o falla.
  async obtenerFotoUrl(id) {
    try {
      const { data: blob } = await clienteRepository.obtenerFotoBlob(id)
      return URL.createObjectURL(blob)
    } catch {
      return null
    }
  },

  // Clientes dados de alta por personal (no autorregistrados) que todavia no
  // tienen con que iniciar sesion.
  async listarSinAcceso() {
    try {
      const { data: clientes } = await clienteRepository.listarSinAcceso()
      return { ok: true, clientes }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de clientes sin acceso.')
    }
  },

  async asignarAcceso(id, password) {
    try {
      await clienteRepository.asignarAcceso(id, password)
      return { ok: true, mensaje: 'Se le asigno una contrasena a este cliente: ya puede iniciar sesion con su correo.' }
    } catch (e) {
      return extraerError(e, 'No se pudo asignar la contrasena.')
    }
  },

  async historial(id) {
    try {
      const { data: movimientos } = await clienteRepository.historial(id)
      return { ok: true, movimientos }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar el historial del cliente.')
    }
  },

  async suspender(id, motivo) {
    try {
      const { data: cliente } = await clienteRepository.suspender(id, motivo)
      return { ok: true, mensaje: 'Cliente suspendido correctamente.', cliente }
    } catch (e) {
      return extraerError(e, 'No se pudo suspender al cliente.')
    }
  },

  async reactivar(id, comentario) {
    try {
      const { data: cliente } = await clienteRepository.reactivar(id, comentario)
      return { ok: true, mensaje: 'Cliente reactivado correctamente.', cliente }
    } catch (e) {
      return extraerError(e, 'No se pudo reactivar al cliente.')
    }
  },

  async eliminar(id, motivo) {
    try {
      const { data: cliente } = await clienteRepository.eliminar(id, motivo)
      return { ok: true, mensaje: 'Cliente eliminado correctamente.', cliente }
    } catch (e) {
      return extraerError(e, 'No se pudo eliminar al cliente.')
    }
  },

  async restaurar(id) {
    try {
      const { data: cliente } = await clienteRepository.restaurar(id)
      return { ok: true, mensaje: 'Cliente restaurado correctamente.', cliente }
    } catch (e) {
      return extraerError(e, 'No se pudo restaurar al cliente.')
    }
  },

  // No usa el contrato {ok,...}: siempre regresa un resultado valido (o
  // "ninguno") porque es un chequeo silencioso al salir del campo, no una
  // accion que el usuario dispare a proposito.
  async verificarCurp(curp) {
    try {
      const { data } = await clienteRepository.verificarCurp(curp)
      return data
    } catch {
      return { existe: false, clienteId: null, nombreCompleto: null }
    }
  }
}

// Mismas reglas que SuspenderClienteRequest/EliminarClienteRequest en el
// backend (la de ahi es la que cuenta): motivo obligatorio, 10 a 255
// caracteres. Se exporta aparte porque la usan los modales directamente
// para deshabilitar el boton antes de intentar guardar.
export function validarMotivo(motivo) {
  const largo = (motivo || '').trim().length
  if (largo === 0) return 'El motivo es obligatorio.'
  if (largo < 10) return 'El motivo debe tener al menos 10 caracteres.'
  if (largo > 255) return 'El motivo no debe superar 255 caracteres.'
  return ''
}
