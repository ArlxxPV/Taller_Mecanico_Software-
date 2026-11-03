<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { direccionFacade } from '../facades/direccionFacade'

const props = defineProps({
  formulario: { type: Object, required: true },
  fotoActualUrl: { type: String, default: '' }
})

const fotoArchivo = defineModel('fotoArchivo', { default: null })

const toast = useToast()
const TIPOS_FOTO_PERMITIDOS = ['image/jpeg', 'image/png']
const FOTO_MAX_BYTES = 15 * 1024 * 1024

const fotoPreviewNueva = ref('')
const fotoMostrada = computed(() => fotoPreviewNueva.value || props.fotoActualUrl)

function elegirFoto(evento) {
  const archivo = evento.target.files?.[0]
  if (!archivo) return

  if (!TIPOS_FOTO_PERMITIDOS.includes(archivo.type)) {
    toast.add({ severity: 'warn', summary: 'Formato no permitido', detail: 'La foto debe ser un archivo JPG o PNG.', life: 4000 })
    evento.target.value = ''
    return
  }
  if (archivo.size > FOTO_MAX_BYTES) {
    toast.add({ severity: 'warn', summary: 'Archivo muy pesado', detail: 'La foto no debe superar 15 MB.', life: 4000 })
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

// Mismo patron de cascada Estado->Municipio->Colonia->CP que ClienteFormularioCampos.
const estados = ref([])
const municipios = ref([])
const colonias = ref([])
const mapaColoniaCp = ref(new Map())
const cargandoEstados = ref(false)
const cargandoMunicipios = ref(false)
const cargandoColonias = ref(false)

onMounted(async () => {
  cargandoEstados.value = true
  const resultado = await direccionFacade.obtenerEstados()
  cargandoEstados.value = false
  if (resultado.ok) {
    estados.value = resultado.estados
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar los estados', detail: resultado.mensaje, life: 4000 })
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

function forzarMayusculasRfc() {
  props.formulario.rfc = (props.formulario.rfc || '').toUpperCase()
}

defineExpose({ quitarFoto })
</script>

<template>
  <!-- Datos del taller -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Datos del taller</h2>
    <div class="mt-4 grid gap-5 sm:grid-cols-2">
      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="nombre">Nombre del taller</label>
        <input id="nombre" v-model="formulario.nombre" type="text" required
               class="campo-tallerpro" placeholder="Ej. Taller Centro" />
      </div>

      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="razonSocial">Razón social</label>
        <input id="razonSocial" v-model="formulario.razonSocial" type="text" required
               class="campo-tallerpro" placeholder="Ej. Talleres Pro SA de CV" />
      </div>

      <div>
        <label class="etiqueta-campo" for="telefono">Teléfono</label>
        <input id="telefono" v-model="formulario.telefono" type="tel" required maxlength="10" inputmode="numeric"
               class="campo-tallerpro" placeholder="10 dígitos" />
      </div>

      <div>
        <label class="etiqueta-campo" for="rfc">RFC</label>
        <input id="rfc" v-model="formulario.rfc" type="text" required maxlength="13"
               class="campo-tallerpro uppercase" placeholder="Ej. TPR920101AB1" @input="forzarMayusculasRfc" />
      </div>

      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="emailContacto">Correo de contacto</label>
        <input id="emailContacto" v-model="formulario.emailContacto" type="email" required
               class="campo-tallerpro" placeholder="contacto@taller.com" />
      </div>
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
    <h2 class="font-display text-lg font-semibold text-ink">Foto del taller (opcional)</h2>
    <p class="mt-1 text-xs text-ink/50">Formatos JPG o PNG, máximo 15 MB.</p>

    <div class="mt-4 flex items-center gap-4">
      <img v-if="fotoMostrada" :src="fotoMostrada" alt="Vista previa de la foto del taller"
           class="h-20 w-20 rounded-full border border-mist object-cover" />
      <div v-else class="flex h-20 w-20 items-center justify-center rounded-full border border-dashed border-mist text-ink/30">
        <i class="pi pi-building text-2xl"></i>
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
