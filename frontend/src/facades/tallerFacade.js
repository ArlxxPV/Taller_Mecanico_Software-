import { tallerRepository } from '../repositories/tallerRepository'

function extraerError(e, mensajePorDefecto) {
  return {
    ok: false,
    mensaje: e.response?.data?.mensaje || mensajePorDefecto,
    detalles: e.response?.data?.detalles || []
  }
}

// Mismo objeto que espera el backend (TallerRequest), igual patron que
// clienteFacade.armarDatos/aFormulario.
function armarDatos(formulario) {
  return {
    nombre: formulario.nombre,
    razonSocial: formulario.razonSocial,
    telefono: formulario.telefono,
    rfc: formulario.rfc,
    emailContacto: formulario.emailContacto,
    direccion: {
      calle: formulario.calle,
      colonia: formulario.colonia,
      municipio: formulario.municipio,
      estado: formulario.estado,
      codigoPostal: formulario.codigoPostal
    }
  }
}

function aFormulario(taller) {
  return {
    nombre: taller.nombre || '',
    razonSocial: taller.razonSocial || '',
    telefono: taller.telefono || '',
    rfc: taller.rfc || '',
    emailContacto: taller.emailContacto || '',
    calle: taller.calle || '',
    colonia: taller.colonia || '',
    municipio: taller.municipio || '',
    estado: taller.estado || '',
    codigoPostal: taller.codigoPostal || ''
  }
}

export const tallerFacade = {
  async registrar(formulario, foto) {
    try {
      const { data: taller } = await tallerRepository.crear(armarDatos(formulario), foto)
      return { ok: true, mensaje: 'El taller se registro correctamente.', taller }
    } catch (e) {
      return extraerError(e, 'No se pudo registrar el taller.')
    }
  },

  async listar() {
    try {
      const { data: talleres } = await tallerRepository.listar()
      return { ok: true, talleres }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la lista de talleres.')
    }
  },

  async obtener(id) {
    try {
      const { data: taller } = await tallerRepository.obtener(id)
      return { ok: true, taller, formulario: aFormulario(taller) }
    } catch (e) {
      return extraerError(e, 'No se pudo cargar la informacion del taller.')
    }
  },

  async actualizar(id, formulario, foto) {
    try {
      const { data: taller } = await tallerRepository.actualizar(id, armarDatos(formulario), foto)
      return { ok: true, mensaje: 'Los datos del taller se actualizaron correctamente.', taller }
    } catch (e) {
      return extraerError(e, 'No se pudieron actualizar los datos del taller.')
    }
  },

  async obtenerFotoUrl(id) {
    try {
      const { data: blob } = await tallerRepository.obtenerFotoBlob(id)
      return URL.createObjectURL(blob)
    } catch {
      return null
    }
  }
}
