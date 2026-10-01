<script setup>
import { reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { clienteFacade } from '../facades/clienteFacade'
import ClienteFormularioCampos from '../components/ClienteFormularioCampos.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const formulario = reactive({
  nombreCompleto: '', contactoAlternativo: '', edad: '', fechaNacimiento: '',
  telefonoPersonal: '', telefonoTrabajo: '', email: '', emailTrabajo: '',
  calle: '', colonia: '', municipio: '', estado: '', codigoPostal: ''
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
  <div class="min-h-screen bg-paper px-6 py-10 sm:px-10">
    <div class="mx-auto max-w-3xl">
      <RouterLink :to="{ name: 'clientes-lista' }" class="text-sm font-medium text-primary hover:text-primary-hover">
        ← Volver a clientes
      </RouterLink>

      <div v-if="cargandoDatos" class="mt-10 text-center text-sm text-ink/50">Cargando...</div>

      <div v-else-if="noEncontrado" class="mt-10 text-center text-sm text-ink/60">
        No se encontró este cliente.
      </div>

      <template v-else>
        <h1 class="mt-3 font-display text-2xl font-bold text-ink">Editar cliente</h1>
        <p class="mt-1 text-sm text-ink/60">{{ formulario.nombreCompleto }}</p>

        <form class="mt-8 space-y-8" @submit.prevent="enviar">
          <div v-if="mensajeError" class="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            <p class="font-medium">{{ mensajeError }}</p>
            <ul v-if="erroresCampos.length" class="mt-1 list-disc pl-4">
              <li v-for="(detalle, i) in erroresCampos" :key="i">{{ detalle }}</li>
            </ul>
          </div>

          <ClienteFormularioCampos :formulario="formulario" :foto-actual-url="fotoActualUrl" v-model:foto-archivo="fotoArchivo" />

          <div class="flex gap-3">
            <button type="submit" class="boton-primario sm:w-auto sm:px-8" :disabled="guardando">
              {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
            </button>
            <RouterLink :to="{ name: 'clientes-detalle', params: { id: route.params.id } }"
                        class="rounded-lg border border-mist px-4 py-2.5 text-sm font-medium text-ink hover:bg-mist/60">
              Cancelar
            </RouterLink>
          </div>
        </form>
      </template>
    </div>
  </div>
</template>
