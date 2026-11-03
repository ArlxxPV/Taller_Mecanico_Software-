<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import { tallerFacade } from '../facades/tallerFacade'
import TallerFormularioCampos from '../components/TallerFormularioCampos.vue'

const router = useRouter()
const toast = useToast()

const formulario = reactive({
  nombre: '',
  razonSocial: '',
  telefono: '',
  rfc: '',
  emailContacto: '',
  calle: '',
  colonia: '',
  municipio: '',
  estado: '',
  codigoPostal: ''
})

const campos = ref(null)
const fotoArchivo = ref(null)
const cargando = ref(false)
const mensajeError = ref('')
const erroresCampos = ref([])
const mostrarConfirmacion = ref(false)

function limpiarFormulario() {
  Object.keys(formulario).forEach((campo) => { formulario[campo] = '' })
  campos.value?.quitarFoto()
  mensajeError.value = ''
  erroresCampos.value = []
}

async function enviar() {
  mensajeError.value = ''
  erroresCampos.value = []
  cargando.value = true

  const resultado = await tallerFacade.registrar(formulario, fotoArchivo.value)
  cargando.value = false

  if (resultado.ok) {
    mostrarConfirmacion.value = true
  } else {
    mensajeError.value = resultado.mensaje
    erroresCampos.value = resultado.detalles
    toast.add({ severity: 'error', summary: 'No se pudo registrar el taller', detail: resultado.mensaje, life: 4500 })
  }
}

function registrarOtro() {
  mostrarConfirmacion.value = false
  limpiarFormulario()
}

function irALista() {
  mostrarConfirmacion.value = false
  router.push({ name: 'talleres-lista' })
}
</script>

<template>
  <div class="mx-auto max-w-3xl">
    <h1 class="titulo-pagina">Registrar taller</h1>
    <p class="subtitulo-pagina">Captura los datos del taller para darlo de alta en el sistema. Solo administradores.</p>

    <form class="mt-8 space-y-8" @submit.prevent="enviar">
      <div v-if="mensajeError" class="caja-error-tp">
        <p class="font-semibold">{{ mensajeError }}</p>
        <ul v-if="erroresCampos.length" class="mt-1 list-disc pl-4">
          <li v-for="(detalle, i) in erroresCampos" :key="i">{{ detalle }}</li>
        </ul>
      </div>

      <TallerFormularioCampos ref="campos" :formulario="formulario" v-model:foto-archivo="fotoArchivo" />

      <button type="submit" class="boton-tp-primario w-auto px-8" :disabled="cargando">
        {{ cargando ? 'Guardando...' : 'Registrar taller' }}
      </button>
    </form>

    <Dialog v-model:visible="mostrarConfirmacion" modal header="Taller registrado" :closable="false" class="w-full max-w-sm">
      <p class="text-sm text-ink/70">
        <strong>{{ formulario.nombre }}</strong> se registró correctamente en TallerPro.
      </p>
      <template #footer>
        <button class="rounded-lg border border-mist px-4 py-2.5 text-sm font-medium text-ink hover:bg-mist/60" @click="registrarOtro">
          Registrar otro taller
        </button>
        <button class="boton-primario w-auto px-4 py-2.5" @click="irALista">
          Ver talleres registrados
        </button>
      </template>
    </Dialog>
  </div>
</template>
