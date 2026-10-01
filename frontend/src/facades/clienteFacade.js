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
// direccion anidada.
function armarDatos(formulario) {
  return {
    nombreCompleto: formulario.nombreCompleto,
    contactoAlternativo: formulario.contactoAlternativo || null,
    edad: formulario.edad !== '' && formulario.edad !== null ? Number(formulario.edad) : null,
    fechaNacimiento: formulario.fechaNacimiento || null,
    telefonoPersonal: formulario.telefonoPersonal,
    telefonoTrabajo: formulario.telefonoTrabajo || null,
    email: formulario.email,
    emailTrabajo: formulario.emailTrabajo || null,
    direccion: {
      calle: formulario.calle,
      colonia: formulario.colonia,
      municipio: formulario.municipio,
      estado: formulario.estado,
      codigoPostal: formulario.codigoPostal
    }
  }
}

// Vuelca un ClienteResponse del backend sobre los campos sueltos que usa el
// formulario (lo inverso de armarDatos), para precargar la pantalla de editar.
function aFormulario(cliente) {
  return {
    nombreCompleto: cliente.nombreCompleto || '',
    contactoAlternativo: cliente.contactoAlternativo || '',
    edad: cliente.edad ?? '',
    fechaNacimiento: cliente.fechaNacimiento || '',
    telefonoPersonal: cliente.telefonoPersonal || '',
    telefonoTrabajo: cliente.telefonoTrabajo || '',
    email: cliente.email || '',
    emailTrabajo: cliente.emailTrabajo || '',
    calle: cliente.calle || '',
    colonia: cliente.colonia || '',
    municipio: cliente.municipio || '',
    estado: cliente.estado || '',
    codigoPostal: cliente.codigoPostal || ''
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

  async listar() {
    try {
      const { data: clientes } = await clienteRepository.listar()
      return { ok: true, clientes }
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

  // Descarga la foto (con el JWT, via el interceptor de http.js) y la
  // convierte en una URL que <img> puede usar. null si no hay foto o falla.
  async obtenerFotoUrl(id) {
    try {
      const { data: blob } = await clienteRepository.obtenerFotoBlob(id)
      return URL.createObjectURL(blob)
    } catch {
      return null
    }
  }
}
