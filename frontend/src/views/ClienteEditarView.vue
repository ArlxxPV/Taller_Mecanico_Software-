<script setup>
import { reactive, ref, watch, computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { clienteFacade } from '../facades/clienteFacade'
import ClienteFormularioCampos from '../components/ClienteFormularioCampos.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const auth = useAuthStore()

const bloquearTaller = computed(() => auth.rolesUsuario.includes('RECEPCIONISTA'))
const nombreCompleto = computed(() => [formulario.nombres, formulario.apellidoPaterno, formulario.apellidoMaterno].filter(Boolean).join(' '))

const formulario = reactive({
  nombres: '', apellidoPaterno: '', apellidoMaterno: '', curp: '', contactoAlternativo: '', fechaNacimiento: '',
  telefonoPersonal: '', telefonoTrabajo: '', email: '', emailTrabajo: '',
  calle: '', colonia: '', municipio: '', estado: '', codigoPostal: '', tallerId: ''
})

const fotoArchivo = ref(null)
const fotoActualUrl = ref('')
const cargandoDatos = ref(true)
const guardando = ref(false)
const noEncontrado = ref(false)
const mensajeError = ref('')
const erroresCampos = ref([])

// watch + immediate en vez de onMounted: si vienes de editar otro cliente,
// Vue reutiliza esta misma pantalla y "mounted" no se vuelve a disparar.
watch(
  () => route.params.id,
  async (id) => {
    cargandoDatos.value = true
    noEncontrado.value = false
    fotoArchivo.value = null
    fotoActualUrl.value = ''

    const resultado = await clienteFacade.obtener(id)

    if (!resultado.ok) {
      noEncontrado.value = true
      toast.add({ severity: 'error', summary: 'No se pudo abrir el cliente', detail: resultado.mensaje, life: 4500 })
      cargandoDatos.value = false
      return
    }

    Object.assign(formulario, resultado.formulario)
    if (resultado.cliente.tieneFoto) {
      fotoActualUrl.value = (await clienteFacade.obtenerFotoUrl(id)) || ''
    }
    cargandoDatos.value = false
  },
  { immediate: true }
)

async function enviar() {
  mensajeError.value = ''
  erroresCampos.value = []
  guardando.value = true

  const id = route.params.id
  const resultado = await clienteFacade.actualizar(id, formulario, fotoArchivo.value)
  guardando.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente actualizado', detail: resultado.mensaje, life: 3000 })
    router.push({ name: 'clientes-detalle', params: { id } })
  } else {
    mensajeError.value = resultado.mensaje
    erroresCampos.value = resultado.detalles
    toast.add({ severity: 'error', summary: 'No se pudo actualizar', detail: resultado.mensaje, life: 4500 })
  }
}
</script>

<template>
  <div class="mx-auto max-w-6xl">
    <div v-if="cargandoDatos" class="mt-10 text-center text-sm text-secondary">Cargando...</div>

    <div v-else-if="noEncontrado" class="mt-10 text-center text-sm text-secondary">
      No se encontró este cliente.
    </div>

    <template v-else>
      <h1 class="titulo-pagina">Editar cliente</h1>
      <p class="subtitulo-pagina">{{ nombreCompleto }}</p>

      <form class="mt-8" @submit.prevent="enviar">
        <div v-if="mensajeError" class="caja-error-tp mb-6">
          <p class="font-semibold">{{ mensajeError }}</p>
          <ul v-if="erroresCampos.length" class="mt-1 list-disc pl-4">
            <li v-for="(detalle, i) in erroresCampos" :key="i">{{ detalle }}</li>
          </ul>
        </div>

        <!-- Las secciones del formulario fluyen en 2 columnas tipo mosaico
             (cada tarjeta cae en la columna mas corta) para aprovechar el
             ancho de la pantalla en vez de apilarse una tras otra. -->
        <div class="columns-1 gap-6 lg:columns-2 [&>section]:mb-6 [&>section]:break-inside-avoid-column">
          <ClienteFormularioCampos :formulario="formulario" :foto-actual-url="fotoActualUrl"
                                    :bloquear-taller="bloquearTaller" v-model:foto-archivo="fotoArchivo" />
        </div>

        <div class="flex gap-3">
          <button type="submit" class="boton-tp-primario" :disabled="guardando">
            {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
          </button>
          <RouterLink :to="{ name: 'clientes-detalle', params: { id: route.params.id } }" class="boton-tp-secundario">
            Cancelar
          </RouterLink>
        </div>
      </form>
    </template>
  </div>
</template>
