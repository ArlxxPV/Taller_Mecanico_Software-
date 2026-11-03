<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { tallerFacade } from '../facades/tallerFacade'
import ModalFotoAmpliada from '../components/ModalFotoAmpliada.vue'

const toast = useToast()

const talleres = ref([])
const cargando = ref(true)
const fotosPorTaller = reactive({})
const fotoAmpliada = ref(null)

const hayTalleres = computed(() => talleres.value.length > 0)

function formatearFecha(fechaIso) {
  if (!fechaIso) return '—'
  return new Date(fechaIso).toLocaleDateString('es-MX', { dateStyle: 'medium' })
}

async function cargarFotos() {
  for (const taller of talleres.value) {
    if (taller.tieneFoto) {
      fotosPorTaller[taller.id] = await tallerFacade.obtenerFotoUrl(taller.id)
    }
  }
}

async function cargar() {
  cargando.value = true
  const resultado = await tallerFacade.listar()
  cargando.value = false

  if (resultado.ok) {
    talleres.value = resultado.talleres
    cargarFotos()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar la lista', detail: resultado.mensaje, life: 4500 })
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-5xl">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 class="titulo-pagina">Talleres</h1>
        <p class="subtitulo-pagina">Talleres registrados en el sistema. Solo administradores.</p>
      </div>
      <RouterLink :to="{ name: 'talleres-nuevo' }" class="boton-tp-primario">
        + Registrar taller
      </RouterLink>
    </div>

    <div class="mt-6 overflow-hidden rounded-[14px] border border-mist bg-white">
        <div v-if="cargando" class="p-10 text-center text-sm text-ink/50">
          Cargando talleres...
        </div>

        <div v-else-if="!hayTalleres" class="p-10 text-center">
          <p class="text-sm text-ink/60">Aún no hay talleres registrados.</p>
          <RouterLink :to="{ name: 'talleres-nuevo' }" class="mt-3 inline-block text-sm font-medium text-primary hover:text-primary-hover">
            Registrar el primero
          </RouterLink>
        </div>

        <table v-else class="w-full text-left text-sm">
          <thead class="bg-mist/50 text-xs uppercase tracking-wide text-ink/50">
            <tr>
              <th class="px-5 py-3 font-medium">Foto</th>
              <th class="px-5 py-3 font-medium">Nombre</th>
              <th class="px-5 py-3 font-medium">Razón social</th>
              <th class="px-5 py-3 font-medium">Teléfono</th>
              <th class="px-5 py-3 font-medium">RFC</th>
              <th class="px-5 py-3 font-medium">Ubicación</th>
              <th class="px-5 py-3 font-medium">Registrado</th>
              <th class="px-5 py-3 font-medium">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-mist">
            <tr v-for="taller in talleres" :key="taller.id">
              <td class="px-5 py-3">
                <button v-if="fotosPorTaller[taller.id]" type="button" title="Ver foto en grande"
                        @click="fotoAmpliada = { src: fotosPorTaller[taller.id], titulo: taller.nombre }">
                  <img :src="fotosPorTaller[taller.id]" :alt="`Foto de ${taller.nombre}`"
                       class="h-10 w-10 rounded-full border border-mist object-cover transition hover:opacity-80" />
                </button>
                <div v-else class="flex h-10 w-10 items-center justify-center rounded-full border border-dashed border-mist text-ink/30">
                  <i class="pi pi-building"></i>
                </div>
              </td>
              <td class="px-5 py-3 font-medium text-ink">{{ taller.nombre }}</td>
              <td class="px-5 py-3 text-ink/70">{{ taller.razonSocial }}</td>
              <td class="px-5 py-3 text-ink/70">{{ taller.telefono }}</td>
              <td class="px-5 py-3 text-ink/70">{{ taller.rfc }}</td>
              <td class="px-5 py-3 text-ink/70">{{ [taller.municipio, taller.estado].filter(Boolean).join(', ') || '—' }}</td>
              <td class="px-5 py-3 text-ink/50">{{ formatearFecha(taller.creadoEn) }}</td>
              <td class="px-5 py-3">
                <RouterLink :to="{ name: 'talleres-editar', params: { id: taller.id } }" class="text-sm font-semibold text-primary hover:underline">
                  Editar
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
  </div>

  <ModalFotoAmpliada v-if="fotoAmpliada" :src="fotoAmpliada.src" :titulo="fotoAmpliada.titulo" @cerrar="fotoAmpliada = null" />
</template>
