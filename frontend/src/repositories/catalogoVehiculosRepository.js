import http from '../services/http'

export const catalogoVehiculosRepository = {
  marcas() {
    return http.get('/clientes/catalogos/vehiculos/marcas')
  },
  modelos(marca, anio) {
    return http.get('/clientes/catalogos/vehiculos/modelos', { params: { marca, anio: anio || undefined } })
  },
  decodificarVin(vin) {
    return http.get(`/clientes/catalogos/vehiculos/vin/${vin}`)
  }
}
