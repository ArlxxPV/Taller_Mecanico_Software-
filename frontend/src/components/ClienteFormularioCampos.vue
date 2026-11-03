<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { direccionFacade } from '../facades/direccionFacade'
import { tallerFacade } from '../facades/tallerFacade'

// `formulario` es el objeto reactivo del padre (Registrar o Editar): se
// muta directo aqui adentro via v-model, no hace falta emitir nada para eso.
// `fotoActualUrl` solo aplica en Editar: la foto que el cliente ya tenia
// guardada, para mostrarla mientras no se elija una nueva.
// `bloquearTaller`: true cuando quien captura es RECEPCIONISTA (solo puede
// registrar/editar clientes de su propio taller, ver ClienteRegistroView/
// ClienteEditarView) - el select queda deshabilitado y precargado.
const props = defineProps({
  formulario: { type: Object, required: true },
  fotoActualUrl: { type: String, default: '' },
  bloquearTaller: { type: Boolean, default: false }
})

const fotoArchivo = defineModel('fotoArchivo', { default: null })
const emit = defineEmits(['curp-blur'])

const toast = useToast()
const TIPOS_FOTO_PERMITIDOS = ['image/jpeg', 'image/png']
const FOTO_MAX_BYTES = 12 * 1024 * 1024

const fotoPreviewNueva = ref('')
const fotoMostrada = computed(() => fotoPreviewNueva.value || props.fotoActualUrl)

// Edad: de solo lectura, recalculada en vivo desde la fecha de nacimiento.
// El backend tambien la calcula siempre (nunca confia en un valor capturado).
const edadCalculada = computed(() => {
  if (!props.formulario.fechaNacimiento) return ''
  const nacimiento = new Date(props.formulario.fechaNacimiento)
  if (Number.isNaN(nacimiento.getTime())) return ''
  const hoy = new Date()
  let edad = hoy.getFullYear() - nacimiento.getFullYear()
  const aunNoCumple = hoy.getMonth() < nacimiento.getMonth() ||
    (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() < nacimiento.getDate())
  if (aunNoCumple) edad--
  return edad >= 0 ? edad : ''
})

function elegirFoto(evento) {
  const archivo = evento.target.files?.[0]
  if (!archivo) return

  if (!TIPOS_FOTO_PERMITIDOS.includes(archivo.type)) {
    toast.add({ severity: 'warn', summary: 'Formato no permitido', detail: 'La foto debe ser un archivo JPG o PNG.', life: 4000 })
    evento.target.value = ''
    return
  }
  if (archivo.size > FOTO_MAX_BYTES) {
    toast.add({ severity: 'warn', summary: 'Archivo muy pesado', detail: 'La foto no debe superar 12 MB.', life: 4000 })
    evento.target.value = ''
    return
  }

  if (fotoPreviewNueva.value) URL.revokeObjectURL(fotoPreviewNueva.value)
  fotoArchivo.value = archivo
  fotoPreviewNueva.value = URL.createObjectURL(archivo)
}

function quitarFoto() {
  if (fotoPreviewNueva.value) URL.revokeObjectURL(fotoPreviewNueva.value)
  fotoArchivo.value = null
  fotoPreviewNueva.value = ''
}

function forzarMayusculasCurp() {
  props.formulario.curp = (props.formulario.curp || '').toUpperCase()
}

// Emite curp-blur (opcional: solo lo usa ClienteRegistroView, para el
// chequeo "al salir del campo" de CURP duplicada) sin afectar a quien no
// escuche el evento (ej. el modal de Editar, que valida distinto).
function alSalirCurp() {
  emit('curp-blur')
}

// Selects en cascada Estado -> Municipio -> Colonia -> CP (catalogo COPOMEX,
// via el proxy del backend). Los watchers de abajo solo CARGAN las opciones
// de cada select (se disparan igual al editar un cliente, cuando el padre
// precarga formulario.estado/municipio con Object.assign, que no es una
// "eleccion" del usuario); limpiar los campos dependientes solo debe pasar
// en una eleccion real, por eso esa parte vive en los @change de la plantilla
// y no en estos watchers.
const estados = ref([])
const municipios = ref([])
const colonias = ref([])
const mapaColoniaCp = ref(new Map())
const cargandoEstados = ref(false)
const cargandoMunicipios = ref(false)
const cargandoColonias = ref(false)

// Talleres: lista simple (sin cascada), para el select "Taller".
const talleres = ref([])
const cargandoTalleres = ref(false)

onMounted(async () => {
  cargandoEstados.value = true
  const resultado = await direccionFacade.obtenerEstados()
  cargandoEstados.value = false
  if (resultado.ok) {
    estados.value = resultado.estados
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar los estados', detail: resultado.mensaje, life: 4000 })
  }

  cargandoTalleres.value = true
  const resultadoTalleres = await tallerFacade.listar()
  cargandoTalleres.value = false
  if (resultadoTalleres.ok) {
    talleres.value = resultadoTalleres.talleres
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar los talleres', detail: resultadoTalleres.mensaje, life: 4000 })
  }
})

watch(() => props.formulario.estado, async (estado) => {
  municipios.value = []
  if (!estado) return
  cargandoMunicipios.value = true
  const resultado = await direccionFacade.obtenerMunicipios(estado)
  cargandoMunicipios.value = false
  if (resultado.ok) {
    municipios.value = resultado.municipios
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar los municipios', detail: resultado.mensaje, life: 4000 })
  }
}, { immediate: true })

watch(() => props.formulario.municipio, async (municipio) => {
  colonias.value = []
  mapaColoniaCp.value = new Map()
  if (!municipio || !props.formulario.estado) return
  cargandoColonias.value = true
  const resultado = await direccionFacade.obtenerColonias(props.formulario.estado, municipio)
  cargandoColonias.value = false
  if (resultado.ok) {
    mapaColoniaCp.value = resultado.mapa
    colonias.value = [...resultado.mapa.keys()]
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar las colonias', detail: resultado.mensaje, life: 4000 })
  }
}, { immediate: true })

function onEstadoChange() {
  props.formulario.municipio = ''
  props.formulario.colonia = ''
  props.formulario.codigoPostal = ''
}

function onMunicipioChange() {
  props.formulario.colonia = ''
  props.formulario.codigoPostal = ''
}

function onColoniaChange() {
  props.formulario.codigoPostal = mapaColoniaCp.value.get(props.formulario.colonia) || ''
}

defineExpose({ quitarFoto })
</script>

<template>
  <!-- Datos personales -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Datos personales</h2>
    <div class="mt-4 grid gap-5 sm:grid-cols-2">
      <div>
        <label class="etiqueta-campo" for="nombres">Nombre(s)</label>
        <input id="nombres" v-model="formulario.nombres" type="text" required
               class="campo-tallerpro" placeholder="Ej. Luis Fernando" />
      </div>
      <div>
        <label class="etiqueta-campo" for="apellidoPaterno">Apellido paterno</label>
        <input id="apellidoPaterno" v-model="formulario.apellidoPaterno" type="text" required
               class="campo-tallerpro" placeholder="Ej. Perez" />
      </div>
      <div>
        <label class="etiqueta-campo" for="apellidoMaterno">Apellido materno</label>
        <input id="apellidoMaterno" v-model="formulario.apellidoMaterno" type="text" required
               class="campo-tallerpro" placeholder="Ej. Diaz" />
      </div>

      <div>
        <label class="etiqueta-campo" for="curp">CURP</label>
        <input id="curp" v-model="formulario.curp" type="text" required maxlength="18"
               class="campo-tallerpro uppercase" placeholder="18 caracteres" @input="forzarMayusculasCurp" @blur="alSalirCurp" />
      </div>

      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="contactoAlternativo">Contacto alternativo</label>
        <input id="contactoAlternativo" v-model="formulario.contactoAlternativo" type="text" required
               class="campo-tallerpro" placeholder="Nombre y telefono de otra persona de contacto" />
      </div>

      <div>
        <label class="etiqueta-campo" for="fechaNacimiento">Fecha de nacimiento</label>
        <input id="fechaNacimiento" v-model="formulario.fechaNacimiento" type="date" required
               class="campo-tallerpro" />
      </div>

      <div>
        <label class="etiqueta-campo" for="edad">Edad</label>
        <input id="edad" :value="edadCalculada" type="text" disabled readonly
               class="campo-tallerpro cursor-not-allowed opacity-70" placeholder="Se calcula con la fecha de nacimiento" />
      </div>
    </div>
  </section>

  <!-- Contacto -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Contacto</h2>
    <div class="mt-4 grid gap-5 sm:grid-cols-2">
      <div>
        <label class="etiqueta-campo" for="telefonoPersonal">Telefono personal</label>
        <input id="telefonoPersonal" v-model="formulario.telefonoPersonal" type="tel" required
               maxlength="10" inputmode="numeric" class="campo-tallerpro" placeholder="10 dígitos" />
      </div>

      <div>
        <label class="etiqueta-campo" for="telefonoTrabajo">Telefono de trabajo</label>
        <input id="telefonoTrabajo" v-model="formulario.telefonoTrabajo" type="tel" required
               maxlength="10" inputmode="numeric" class="campo-tallerpro" placeholder="10 dígitos" />
      </div>

      <div>
        <label class="etiqueta-campo" for="email">Correo</label>
        <input id="email" v-model="formulario.email" type="email" required
               class="campo-tallerpro" placeholder="cliente@correo.com" />
      </div>

      <div>
        <label class="etiqueta-campo" for="emailTrabajo">Correo de trabajo</label>
        <input id="emailTrabajo" v-model="formulario.emailTrabajo" type="email" required
               class="campo-tallerpro" placeholder="cliente@trabajo.com" />
      </div>
    </div>
  </section>

  <!-- Taller -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Taller</h2>
    <div class="mt-4">
      <label class="etiqueta-campo" for="tallerId">Taller al que pertenece</label>
      <select id="tallerId" v-model="formulario.tallerId" required class="campo-tallerpro"
              :disabled="cargandoTalleres || bloquearTaller">
        <option value="" disabled>Selecciona un taller</option>
        <option v-for="t in talleres" :key="t.id" :value="t.id">{{ t.nombre }}</option>
      </select>
      <p v-if="!cargandoTalleres && talleres.length === 0" class="mt-1 text-xs text-ink/50">Sin resultados.</p>
      <p v-if="bloquearTaller" class="mt-1 text-xs text-ink/50">Tu cuenta solo puede registrar clientes en su propio taller.</p>
    </div>
  </section>

  <!-- Direccion -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Dirección</h2>
    <div class="mt-4 grid gap-5 sm:grid-cols-2">
      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="calle">Calle y número</label>
        <input id="calle" v-model="formulario.calle" type="text" required class="campo-tallerpro" />
      </div>
      <div>
        <label class="etiqueta-campo" for="estado">Estado</label>
        <select id="estado" v-model="formulario.estado" required class="campo-tallerpro"
                :disabled="cargandoEstados" @change="onEstadoChange">
          <option value="" disabled>Selecciona un estado</option>
          <option v-for="e in estados" :key="e" :value="e">{{ e }}</option>
        </select>
        <p v-if="!cargandoEstados && estados.length === 0" class="mt-1 text-xs text-ink/50">Sin resultados.</p>
      </div>
      <div>
        <label class="etiqueta-campo" for="municipio">Municipio</label>
        <select id="municipio" v-model="formulario.municipio" required class="campo-tallerpro"
                :disabled="!formulario.estado || cargandoMunicipios" @change="onMunicipioChange">
          <option value="" disabled>Selecciona un municipio</option>
          <option v-for="m in municipios" :key="m" :value="m">{{ m }}</option>
        </select>
        <p v-if="formulario.estado && !cargandoMunicipios && municipios.length === 0" class="mt-1 text-xs text-ink/50">Sin resultados.</p>
      </div>
      <div>
        <label class="etiqueta-campo" for="colonia">Colonia</label>
        <select id="colonia" v-model="formulario.colonia" required class="campo-tallerpro"
                :disabled="!formulario.municipio || cargandoColonias" @change="onColoniaChange">
          <option value="" disabled>Selecciona una colonia</option>
          <option v-for="c in colonias" :key="c" :value="c">{{ c }}</option>
        </select>
        <p v-if="formulario.municipio && !cargandoColonias && colonias.length === 0" class="mt-1 text-xs text-ink/50">Sin resultados.</p>
      </div>
      <div>
        <label class="etiqueta-campo" for="codigoPostal">Código postal</label>
        <input id="codigoPostal" v-model="formulario.codigoPostal" type="text" inputmode="numeric" maxlength="5" required
               readonly class="campo-tallerpro" placeholder="Se completa al elegir la colonia" />
      </div>
    </div>
  </section>

  <!-- Foto -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Foto (opcional)</h2>
    <p class="mt-1 text-xs text-ink/50">Formatos JPG o PNG, máximo 12 MB.</p>

    <div class="mt-4 flex items-center gap-4">
      <img v-if="fotoMostrada" :src="fotoMostrada" alt="Vista previa de la foto del cliente"
           class="h-20 w-20 rounded-full border border-mist object-cover" />
      <div v-else class="flex h-20 w-20 items-center justify-center rounded-full border border-dashed border-mist text-ink/30">
        <i class="pi pi-user text-2xl"></i>
      </div>

      <div class="flex flex-col gap-2">
        <label class="cursor-pointer rounded-lg border border-mist px-4 py-2 text-sm font-medium text-ink hover:bg-mist/60">
          {{ fotoMostrada ? 'Cambiar foto' : 'Elegir foto' }}
          <input type="file" accept="image/png,image/jpeg" class="hidden" @change="elegirFoto" />
        </label>
        <button v-if="fotoArchivo" type="button" class="text-sm text-red-600 hover:underline" @click="quitarFoto">
          Quitar foto nueva
        </button>
      </div>
    </div>
  </section>
</template>
