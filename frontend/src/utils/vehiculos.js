// Utilidades de VIN/placa compartidas por el modal "Asociar vehiculo" y la
// pantalla "Vehiculos". Las mismas reglas que valida el backend
// (VehiculoRequest/VehiculoService) - esto solo da retroalimentacion
// inmediata en el formulario, el backend es el que de verdad decide.

const REGEX_VIN = /^[A-HJ-NPR-Z0-9]{17}$/
const REGEX_PLACA = /^[A-HJ-NPR-Z]{3}(\d{4}|\d{3}[A-HJ-NPR-Z]?)$/
const REGEX_NUMERO_SERIE = /^[A-HJ-NPR-Z0-9]{8,17}$/

export function normalizarVin(valor) {
  return (valor || '').toUpperCase().replace(/[^A-HJ-NPR-Z0-9]/g, '')
}

export function normalizarPlaca(valor) {
  return (valor || '').toUpperCase().replace(/[^A-HJ-NPR-Z0-9]/g, '')
}

export function validarFormatoVin(vin) {
  return REGEX_VIN.test(vin || '')
}

export function validarFormatoNumeroSerie(valor) {
  return REGEX_NUMERO_SERIE.test(valor || '')
}

export function validarFormatoPlaca(placa) {
  return REGEX_PLACA.test(placa || '')
}

// "HGA123A" -> "HGA-123-A" ; "HGA1234" -> "HGA-1234". Solo para mostrar: lo
// que se guarda siempre es la version sin guiones (ver maqueta).
export function formatearPlaca(placa) {
  if (!placa || placa.length < 6) return placa || ''
  const letras = placa.slice(0, 3)
  const resto = placa.slice(3)
  if (resto.length === 4) return `${letras}-${resto}`
  if (resto.length === 3) return `${letras}-${resto}`
  return `${letras}-${resto.slice(0, 3)}-${resto.slice(3)}`
}

// Digito verificador del VIN (posicion 9, estandar ISO 3779 / NHTSA). Se
// muestra como advertencia, nunca bloquea el envio (asi lo pide la maqueta).
const TABLA_TRANSLITERACION = {
  A: 1, B: 2, C: 3, D: 4, E: 5, F: 6, G: 7, H: 8,
  J: 1, K: 2, L: 3, M: 4, N: 5, P: 7, R: 9,
  S: 2, T: 3, U: 4, V: 5, W: 6, X: 7, Y: 8, Z: 9
}
const PESOS = [8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2]

export function digitoVerificadorEsperado(vin) {
  if (!validarFormatoVin(vin)) return null
  let suma = 0
  for (let i = 0; i < 17; i++) {
    const caracter = vin[i]
    const valor = /\d/.test(caracter) ? Number(caracter) : TABLA_TRANSLITERACION[caracter]
    if (valor === undefined) return null
    suma += valor * PESOS[i]
  }
  const resto = suma % 11
  return resto === 10 ? 'X' : String(resto)
}

export function vinDigitoCorrecto(vin) {
  const esperado = digitoVerificadorEsperado(vin)
  return esperado !== null && esperado === vin[8]
}
