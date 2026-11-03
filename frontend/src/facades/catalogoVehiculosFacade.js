import { catalogoVehiculosRepository } from '../repositories/catalogoVehiculosRepository'

// Siempre regresa un resultado usable (lista vacia / objeto vacio en vez de
// error): son catalogos de autocompletar, nunca deben tumbar el formulario
// si NHTSA vPIC no responde (el usuario puede escribir a mano, ver maqueta).
export const catalogoVehiculosFacade = {
  async marcas() {
    try {
      const { data } = await catalogoVehiculosRepository.marcas()
      return data
    } catch {
      return []
    }
  },

  async modelos(marca, anio) {
    if (!marca) return []
    try {
      const { data } = await catalogoVehiculosRepository.modelos(marca, anio)
      return data
    } catch {
      return []
    }
  },

  async decodificarVin(vin) {
    try {
      const { data } = await catalogoVehiculosRepository.decodificarVin(vin)
      return data
    } catch {
      return { marca: null, modelo: null, anio: null, version: null }
    }
  }
}
