<script setup>
import { reactive, ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { tallerFacade } from '../facades/tallerFacade'
import TallerFormularioCampos from '../components/TallerFormularioCampos.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const nombreActual = computed(() => formulario.nombre)

const formulario = reactive({
  nombre: '', razonSocial: '', telefono: '', rfc: '', emailContacto: '',
  calle: '', colonia: '', municipio: '', estado: '', codigoPostal: ''
})

const fotoArchivo = ref(null)
const fotoActualUrl = ref('')
const cargandoDatos = ref(true)
const guardando = ref(false)
const noEncontrado = ref(false)
const mensajeError = ref('')
const erroresCampos = ref([])

watch(
  () => route.params.id,
  async (id) => {
    cargandoDatos.value = true
    noEncontrado.value = false
    fotoArchivo.value = null
    fotoActualUrl.value = ''

    const resultado = await tallerFacade.obtener(id)

    if (!resultado.ok) {
      noEncontrado.value = true
      toast.add({ severity: 'error', summary: 'No se pudo abrir el taller', detail: resultado.mensaje, life: 4500 })
      cargandoDatos.value = false
      return
    }

    Object.assign(formulario, resultado.formulario)
    if (resultado.taller.tieneFoto) {
      fotoActualUrl.value = (await tallerFacade.obtenerFotoUrl(id)) || ''
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
  const resultado = await tallerFacade.actualizar(id, formulario, fotoArchivo.value)
  guardando.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Taller actualizado', detail: resultado.mensaje, life: 3000 })
    router.push({ name: 'talleres-lista' })
  } else {
    mensajeError.value = resultado.mensaje
    erroresCampos.value = resultado.detalles
    toast.add({ severity: 'error', summary: 'No se pudo actualizar', detail: resultado.mensaje, life: 4500 })
  }
}
</script>

<template>
  <div class="mx-auto max-w-3xl">
    <div v-if="cargandoDatos" class="mt-10 text-center text-sm text-secondary">Cargando...</div>

    <div v-else-if="noEncontrado" class="mt-10 text-center text-sm text-secondary">
      No se encontró este taller.
    </div>

    <template v-else>
      <h1 class="titulo-pagina">Editar taller</h1>
      <p class="subtitulo-pagina">{{ nombreActual }}</p>

      <form class="mt-8 space-y-6" @submit.prevent="enviar">
        <div v-if="mensajeError" class="caja-error-tp">
          <p class="font-semibold">{{ mensajeError }}</p>
          <ul v-if="erroresCampos.length" class="mt-1 list-disc pl-4">
            <li v-for="(detalle, i) in erroresCampos" :key="i">{{ detalle }}</li>
          </ul>
        </div>

        <TallerFormularioCampos :formulario="formulario" :foto-actual-url="fotoActualUrl" v-model:foto-archivo="fotoArchivo" />

        <div class="flex gap-3">
          <button type="submit" class="boton-tp-primario" :disabled="guardando">
            {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
          </button>
          <RouterLink :to="{ name: 'talleres-lista' }" class="boton-tp-secundario">Cancelar</RouterLink>
        </div>
      </form>
    </template>
  </div>
</template>
