<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import { clienteFacade } from '../facades/clienteFacade'
import ClienteFormularioCampos from '../components/ClienteFormularioCampos.vue'

const router = useRouter()
const toast = useToast()

const formulario = reactive({
  nombreCompleto: '',
  contactoAlternativo: '',
  edad: '',
  fechaNacimiento: '',
  telefonoPersonal: '',
  telefonoTrabajo: '',
  email: '',
  emailTrabajo: '',
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

  const resultado = await clienteFacade.registrar(formulario, fotoArchivo.value)
  cargando.value = false

  if (resultado.ok) {
    mostrarConfirmacion.value = true
  } else {
    // No se borra nada de lo ya capturado: solo se muestra el motivo.
    mensajeError.value = resultado.mensaje
    erroresCampos.value = resultado.detalles
    toast.add({ severity: 'error', summary: 'No se pudo registrar al cliente', detail: resultado.mensaje, life: 4500 })
  }
}

function registrarOtro() {
  mostrarConfirmacion.value = false
  limpiarFormulario()
}

function irALista() {
  mostrarConfirmacion.value = false
  router.push({ name: 'clientes-lista' })
}
</script>

<template>
  <div class="min-h-screen bg-paper px-6 py-10 sm:px-10">
    <div class="mx-auto max-w-3xl">
      <RouterLink :to="{ name: 'clientes-lista' }" class="text-sm font-medium text-primary hover:text-primary-hover">
        ← Volver a clientes
      </RouterLink>

      <h1 class="mt-3 font-display text-2xl font-bold text-ink">Registrar cliente</h1>
      <p class="mt-1 text-sm text-ink/60">
        Captura los datos del cliente para darlo de alta en el taller. Solo visible para administradores y recepcion.
      </p>

      <form class="mt-8 space-y-8" @submit.prevent="enviar">
        <div v-if="mensajeError" class="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          <p class="font-medium">{{ mensajeError }}</p>
          <ul v-if="erroresCampos.length" class="mt-1 list-disc pl-4">
            <li v-for="(detalle, i) in erroresCampos" :key="i">{{ detalle }}</li>
          </ul>
        </div>

        <ClienteFormularioCampos ref="campos" :formulario="formulario" v-model:foto-archivo="fotoArchivo" />

        <button type="submit" class="boton-primario sm:w-auto sm:px-8" :disabled="cargando">
          {{ cargando ? 'Guardando...' : 'Registrar cliente' }}
        </button>
      </form>
    </div>

    <Dialog v-model:visible="mostrarConfirmacion" modal header="Cliente registrado" :closable="false" class="w-full max-w-sm">
      <p class="text-sm text-ink/70">
        <strong>{{ formulario.nombreCompleto }}</strong> se registró correctamente en TallerPro.
      </p>
      <template #footer>
        <button class="rounded-lg border border-mist px-4 py-2.5 text-sm font-medium text-ink hover:bg-mist/60" @click="registrarOtro">
          Registrar otro cliente
        </button>
        <button class="boton-primario w-auto px-4 py-2.5" @click="irALista">
          Ver clientes registrados
        </button>
      </template>
    </Dialog>
  </div>
</template>
