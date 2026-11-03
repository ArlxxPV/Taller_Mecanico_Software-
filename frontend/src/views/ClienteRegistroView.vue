<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import { clienteFacade } from '../facades/clienteFacade'
import ClienteFormularioCampos from '../components/ClienteFormularioCampos.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const toast = useToast()
const auth = useAuthStore()

// Una RECEPCIONISTA solo puede registrar clientes en su propio taller: el
// select queda bloqueado y precargado (el backend igual lo valida, esto es
// solo para no dejarla elegir algo que el servidor va a rechazar).
const bloquearTaller = computed(() => auth.rolesUsuario.includes('RECEPCIONISTA'))

const formulario = reactive({
  nombres: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  curp: '',
  contactoAlternativo: '',
  fechaNacimiento: '',
  telefonoPersonal: '',
  telefonoTrabajo: '',
  email: '',
  emailTrabajo: '',
  calle: '',
  colonia: '',
  municipio: '',
  estado: '',
  codigoPostal: '',
  tallerId: bloquearTaller.value ? (auth.usuario?.tallerId ?? '') : ''
})

const campos = ref(null)
const fotoArchivo = ref(null)
const cargando = ref(false)
const mensajeError = ref('')
const erroresCampos = ref([])
const mostrarConfirmacion = ref(false)

// Revisa CURP duplicada al salir del campo (ver maqueta "Registrar cliente").
const curpDuplicada = ref(null) // { existe, clienteId, nombreCompleto } | null
async function alSalirCurp() {
  curpDuplicada.value = null
  if (!formulario.curp || formulario.curp.length < 18) return
  const resultado = await clienteFacade.verificarCurp(formulario.curp)
  if (resultado.existe) curpDuplicada.value = resultado
}
watch(() => formulario.curp, () => { curpDuplicada.value = null })

function limpiarFormulario() {
  Object.keys(formulario).forEach((campo) => { formulario[campo] = '' })
  if (bloquearTaller.value) {
    formulario.tallerId = auth.usuario?.tallerId ?? ''
  }
  campos.value?.quitarFoto()
  mensajeError.value = ''
  erroresCampos.value = []
  curpDuplicada.value = null
}

async function enviar() {
  if (curpDuplicada.value?.existe) return
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

function verClienteExistente() {
  mostrarConfirmacion.value = false
  router.push({ name: 'clientes-detalle', params: { id: curpDuplicada.value.clienteId } })
}
</script>

<template>
  <div class="mx-auto max-w-3xl">
    <h1 class="titulo-pagina">Registrar cliente</h1>
    <p class="subtitulo-pagina">Captura los datos del cliente para darlo de alta en el taller. Solo administradores y recepción.</p>

    <form class="mt-8 space-y-6" @submit.prevent="enviar">
      <div v-if="mensajeError" class="caja-error-tp">
        <p class="font-semibold">{{ mensajeError }}</p>
        <ul v-if="erroresCampos.length" class="mt-1 list-disc pl-4">
          <li v-for="(detalle, i) in erroresCampos" :key="i">{{ detalle }}</li>
        </ul>
      </div>

      <div v-if="curpDuplicada?.existe" class="caja-error-tp flex items-center justify-between gap-3">
        <span>Ya existe un cliente con esta CURP: <strong>{{ curpDuplicada.nombreCompleto }}</strong>. No se puede registrar de nuevo.</span>
        <button type="button" class="shrink-0 font-semibold underline" @click="verClienteExistente">Ver cliente</button>
      </div>

      <ClienteFormularioCampos ref="campos" :formulario="formulario" :bloquear-taller="bloquearTaller" v-model:foto-archivo="fotoArchivo"
                                @curp-blur="alSalirCurp" />

      <button type="submit" class="boton-tp-primario w-auto px-8" :disabled="cargando || curpDuplicada?.existe">
        {{ cargando ? 'Guardando...' : 'Registrar cliente' }}
      </button>
    </form>

    <Dialog v-model:visible="mostrarConfirmacion" modal header="Cliente registrado" :closable="false" class="w-full max-w-sm">
      <p class="text-sm text-ink/70">
        <strong>{{ formulario.nombres }} {{ formulario.apellidoPaterno }} {{ formulario.apellidoMaterno }}</strong> se registró correctamente en TallerPro.
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
