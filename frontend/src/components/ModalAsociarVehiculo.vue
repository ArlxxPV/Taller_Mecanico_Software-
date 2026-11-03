<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import ModalBase from './ModalBase.vue'
import { vehiculoFacade } from '../facades/vehiculoFacade'
import { catalogoVehiculosFacade } from '../facades/catalogoVehiculosFacade'
import {
  normalizarVin, normalizarPlaca, validarFormatoVin, validarFormatoPlaca, validarFormatoNumeroSerie,
  formatearPlaca, vinDigitoCorrecto
} from '../utils/vehiculos'

const props = defineProps({ cliente: { type: Object, required: true } })
const emit = defineEmits(['cerrar', 'guardado'])

const toast = useToast()
const ANIO_ACTUAL = new Date().getFullYear()
const anios = Array.from({ length: ANIO_ACTUAL + 1 - 1950 + 1 }, (_, i) => ANIO_ACTUAL + 1 - i)

const form = reactive({
  vin: '', numeroSerie: '', placa: '', anio: ANIO_ACTUAL, marca: '', modelo: '', version: '', color: '', kilometraje: ''
})
const numeroSerieIgualVin = ref(true)
watch(numeroSerieIgualVin, (igual) => { if (igual) form.numeroSerie = form.vin })
watch(() => form.vin, (vin) => { if (numeroSerieIgualVin.value) form.numeroSerie = vin })

const marcas = ref([])
const modelos = ref([])
const marcaApiId = ref(null)
const modeloApiId = ref(null)
const cargandoVin = ref(false)
const guardando = ref(false)
const mensajeError = ref('')
const erroresCampos = ref([])

// === VIN: formato, digito verificador (solo advertencia) y disponibilidad ===
const vinFormatoValido = computed(() => validarFormatoVin(form.vin))
const vinDigitoOk = computed(() => form.vin.length === 17 && vinDigitoCorrecto(form.vin))
const vinDisponible = ref(true)
let temporizadorVin = null
watch(() => form.vin, (vin) => {
  form.vin = normalizarVin(vin)
  vinDisponible.value = true
  clearTimeout(temporizadorVin)
  if (validarFormatoVin(form.vin)) {
    temporizadorVin = setTimeout(async () => { vinDisponible.value = await vehiculoFacade.verificarVin(form.vin) }, 400)
  }
})
const estadoVin = computed(() => {
  if (!form.vin) return ''
  if (!vinFormatoValido.value) return 'Formato invalido (17 caracteres, sin I, O ni Q)'
  const partes = ['Formato valido']
  partes.push(vinDigitoOk.value ? 'digito verificador correcto' : 'digito verificador no coincide (no bloquea)')
  partes.push(vinDisponible.value ? 'no registrado' : 'ya registrado')
  return partes.join(' · ')
})
const vinEsValidoParaGuardar = computed(() => vinFormatoValido.value && vinDisponible.value)

async function llenarConVin() {
  if (!validarFormatoVin(form.vin)) return
  cargandoVin.value = true
  const datos = await catalogoVehiculosFacade.decodificarVin(form.vin)
  cargandoVin.value = false
  if (datos.anio) form.anio = Number(datos.anio)
  if (datos.marca) form.marca = datos.marca
  if (datos.version) form.version = datos.version
  if (!datos.marca && !datos.anio) {
    toast.add({ severity: 'warn', summary: 'Sin datos para ese VIN', detail: 'NHTSA vPIC no tiene informacion para este VIN. Completa los datos a mano.', life: 4000 })
  }
  if (datos.marca) await cargarModelos()
  if (datos.modelo) form.modelo = datos.modelo
}

// === Placa ===
const placaFormatoValido = computed(() => validarFormatoPlaca(form.placa))
const placaDisponible = ref(true)
let temporizadorPlaca = null
watch(() => form.placa, (placa) => {
  form.placa = normalizarPlaca(placa)
  placaDisponible.value = true
  clearTimeout(temporizadorPlaca)
  if (validarFormatoPlaca(form.placa)) {
    temporizadorPlaca = setTimeout(async () => { placaDisponible.value = await vehiculoFacade.verificarPlaca(form.placa) }, 400)
  }
})
const placaEsValidaParaGuardar = computed(() => placaFormatoValido.value && placaDisponible.value)

// === Marcas / modelos (NHTSA vPIC, via el backend) ===
async function cargarModelos() {
  modelos.value = form.marca ? await catalogoVehiculosFacade.modelos(form.marca, form.anio) : []
}
watch(() => [form.marca, form.anio], cargarModelos)

onMounted(async () => {
  marcas.value = await catalogoVehiculosFacade.marcas()
})

// === Validacion general y envio ===
const formularioValido = computed(() =>
  vinEsValidoParaGuardar.value &&
  validarFormatoNumeroSerie(form.numeroSerie) &&
  placaEsValidaParaGuardar.value &&
  form.anio >= 1950 && form.anio <= ANIO_ACTUAL + 1 &&
  !!form.marca && !!form.modelo && !!form.version &&
  /^[A-Za-zÀ-ÿ ]{3,40}$/.test(form.color || '') &&
  (form.kilometraje === '' || (Number(form.kilometraje) >= 0 && Number(form.kilometraje) <= 2000000))
)

async function guardar() {
  if (!formularioValido.value) return
  mensajeError.value = ''
  erroresCampos.value = []
  guardando.value = true
  const resultado = await vehiculoFacade.asociar(props.cliente.id, {
    vin: form.vin,
    numeroSerie: form.numeroSerie,
    placa: form.placa,
    anio: Number(form.anio),
    marca: form.marca,
    modelo: form.modelo,
    version: form.version,
    color: form.color,
    kilometraje: form.kilometraje === '' ? null : Number(form.kilometraje),
    marcaApiId: marcaApiId.value,
    modeloApiId: modeloApiId.value
  })
  guardando.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Vehiculo registrado', detail: resultado.mensaje, life: 3000 })
    emit('guardado', resultado.vehiculo)
  } else {
    mensajeError.value = resultado.mensaje
    erroresCampos.value = resultado.detalles
  }
}
</script>

<template>
  <ModalBase titulo="Asociar vehículo" @cerrar="$emit('cerrar')">
    <template #subtitulo>
      <span>{{ cliente.nombreCompleto }}</span>
      <span class="ml-2 chip-estatus-activo">ACTIVO</span>
    </template>

    <div v-if="mensajeError" class="caja-error-tp mb-5">
      <p class="font-semibold">{{ mensajeError }}</p>
      <ul v-if="erroresCampos.length" class="mt-1 list-disc pl-4">
        <li v-for="(d, i) in erroresCampos" :key="i">{{ d }}</li>
      </ul>
    </div>

    <section class="mb-6">
      <h3 class="mb-3 font-display text-base font-semibold text-ink">Identificación</h3>

      <label class="etiqueta-tp" for="vin">VIN <span class="float-right font-normal text-secondary">{{ form.vin.length }} / 17</span></label>
      <div class="flex gap-2">
        <input id="vin" v-model="form.vin" type="text" maxlength="17" class="campo-tp uppercase"
               :class="form.vin && !vinFormatoValido && 'campo-tp-error'" placeholder="17 caracteres" />
        <button type="button" class="boton-tp-secundario shrink-0" :disabled="!vinFormatoValido || cargandoVin" @click="llenarConVin">
          {{ cargandoVin ? '...' : 'Llenar con el VIN' }}
        </button>
      </div>
      <p v-if="estadoVin" class="mt-1 text-xs font-semibold" :class="vinEsValidoParaGuardar ? 'text-success' : 'text-danger'">{{ estadoVin }}</p>

      <label class="mt-4 flex items-center gap-2 text-sm text-ink">
        <input v-model="numeroSerieIgualVin" type="checkbox" class="h-4 w-4 rounded border-borderInput text-primary focus:ring-primary/30" />
        No. de serie igual al VIN
      </label>
      <div class="mt-2">
        <label class="etiqueta-tp" for="numeroSerie">No. de serie</label>
        <input id="numeroSerie" v-model="form.numeroSerie" type="text" maxlength="17" :disabled="numeroSerieIgualVin"
               class="campo-tp uppercase" @input="form.numeroSerie = normalizarVin(form.numeroSerie)" />
      </div>

      <div class="mt-4">
        <label class="etiqueta-tp" for="placa">Placa</label>
        <input id="placa" v-model="form.placa" type="text" maxlength="7" class="campo-tp uppercase"
               :class="form.placa && !placaFormatoValido && 'campo-tp-error'" placeholder="HGA-123-A" />
        <p class="ayuda-tp">
          <template v-if="form.placa && placaFormatoValido">Se guarda como {{ form.placa }} · se muestra como {{ formatearPlaca(form.placa) }}</template>
          <template v-else>Se normaliza a mayúsculas, sin espacios ni guiones</template>
        </p>
        <p v-if="form.placa && placaFormatoValido && !placaDisponible" class="error-tp">Esta placa ya esta registrada.</p>
      </div>
    </section>

    <section>
      <div class="mb-3 flex items-center justify-between">
        <h3 class="font-display text-base font-semibold text-ink">Datos del vehículo</h3>
        <span class="text-xs text-secondary">Marcas y modelos: API NHTSA vPIC</span>
      </div>

      <div class="grid gap-5 sm:grid-cols-2">
        <div>
          <label class="etiqueta-tp" for="anio">Año</label>
          <select id="anio" v-model.number="form.anio" class="campo-tp">
            <option v-for="a in anios" :key="a" :value="a">{{ a }}</option>
          </select>
        </div>
        <div>
          <label class="etiqueta-tp" for="marca">Marca</label>
          <input id="marca" v-model="form.marca" type="text" class="campo-tp" list="lista-marcas" placeholder="Ej. NISSAN" />
          <datalist id="lista-marcas"><option v-for="m in marcas" :key="m" :value="m" /></datalist>
        </div>
        <div>
          <label class="etiqueta-tp" for="modelo">Modelo</label>
          <input id="modelo" v-model="form.modelo" type="text" class="campo-tp" list="lista-modelos" placeholder="Ej. Versa" />
          <datalist id="lista-modelos"><option v-for="m in modelos" :key="m" :value="m" /></datalist>
          <p v-if="form.marca && !modelos.length" class="ayuda-tp">Sin modelos de catálogo para esta marca/año: escríbelo a mano.</p>
        </div>
        <div>
          <label class="etiqueta-tp" for="version">Versión</label>
          <input id="version" v-model="form.version" type="text" maxlength="80" class="campo-tp" />
        </div>
        <div>
          <label class="etiqueta-tp" for="color">Color</label>
          <input id="color" v-model="form.color" type="text" maxlength="40" class="campo-tp" placeholder="Ej. Blanco" />
        </div>
        <div>
          <label class="etiqueta-tp" for="kilometraje">Kilometraje (opcional)</label>
          <input id="kilometraje" v-model="form.kilometraje" type="number" min="0" max="2000000" class="campo-tp" placeholder="Ej. 45000" />
        </div>
      </div>
    </section>

    <p class="caja-info-tp mt-6">
      Al guardar, el vehículo queda ACTIVO y asociado a {{ cliente.nombreCompleto }}.
    </p>

    <template #footer>
      <button type="button" class="boton-tp-secundario" @click="$emit('cerrar')">Cancelar</button>
      <button type="button" class="boton-tp-primario" :disabled="!formularioValido || guardando" @click="guardar">
        {{ guardando ? 'Guardando...' : 'Registrar y asociar' }}
      </button>
    </template>
  </ModalBase>
</template>
