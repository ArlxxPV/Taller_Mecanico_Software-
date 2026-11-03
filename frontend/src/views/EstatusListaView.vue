<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { estatusFacade } from '../facades/estatusFacade'

const toast = useToast()

const lista = ref([])
const cargando = ref(true)
const mostrarBajas = ref(false)

const hayEstatus = computed(() => lista.value.length > 0)

const CLASES_CHIP = {
  ACTIVO: 'chip-estatus-activo',
  SUSPENDIDO: 'chip-estatus-suspendido',
  ACTUALIZADO: 'chip-estatus-actualizado',
  ELIMINADO: 'chip-estatus-neutro'
}
function claseChip(valor) {
  return CLASES_CHIP[valor] || 'chip-estatus-neutro'
}

function etiquetaEnUso(estatus) {
  const partes = []
  if (estatus.clientesEnUso > 0) partes.push(`${estatus.clientesEnUso} cliente${estatus.clientesEnUso === 1 ? '' : 's'}`)
  if (estatus.vehiculosEnUso > 0) partes.push(`${estatus.vehiculosEnUso} vehículo${estatus.vehiculosEnUso === 1 ? '' : 's'}`)
  if (partes.length) return partes.join(' · ')
  return estatus.valor === 'ACTUALIZADO' ? 'Solo historial' : 'Sin uso'
}

function formatearFecha(fechaIso) {
  if (!fechaIso) return '—'
  return new Date(fechaIso).toLocaleDateString('es-MX', { dateStyle: 'medium' })
}

async function cargar() {
  cargando.value = true
  const resultado = await estatusFacade.listar(mostrarBajas.value)
  cargando.value = false

  if (resultado.ok) {
    lista.value = resultado.estatus
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar el catalogo', detail: resultado.mensaje, life: 4500 })
  }
}

async function darDeBaja(estatus) {
  if (!confirm(`¿Dar de baja el estatus "${estatus.valor}"? Podras restaurarlo despues.`)) return
  const resultado = await estatusFacade.darDeBaja(estatus.id)
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Estatus dado de baja', detail: resultado.mensaje, life: 3000 })
    cargar()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo dar de baja', detail: resultado.mensaje, life: 4500 })
  }
}

async function restaurar(estatus) {
  const resultado = await estatusFacade.restaurar(estatus.id)
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Estatus restaurado', detail: resultado.mensaje, life: 3000 })
    cargar()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo restaurar', detail: resultado.mensaje, life: 4500 })
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-6xl">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 class="titulo-pagina">Catálogo de estatus</h1>
        <p class="subtitulo-pagina">Los estatus no se eliminan: solo se editan o se dan de baja.</p>
      </div>
      <RouterLink :to="{ name: 'catalogos-estatus-nuevo' }" class="boton-tp-primario">
        + Agregar estatus
      </RouterLink>
    </div>

    <label class="mt-5 flex w-fit items-center gap-2 text-sm text-ink">
      <input type="checkbox" v-model="mostrarBajas" @change="cargar" class="h-4 w-4 rounded border-borderInput text-primary focus:ring-primary/30" />
      Mostrar dados de baja
    </label>

    <div class="mt-5 overflow-hidden rounded-[14px] border border-mist bg-white">
      <div v-if="cargando" class="p-10 text-center text-sm text-secondary">Cargando...</div>
      <div v-else-if="!hayEstatus" class="p-10 text-center text-sm text-secondary">No hay estatus registrados.</div>

      <div v-else class="overflow-x-auto">
        <table class="tabla-tp w-full">
          <thead>
            <tr>
              <th>ID</th>
              <th>Valor</th>
              <th>Descripción</th>
              <th>Tipo</th>
              <th>En uso</th>
              <th>Estado</th>
              <th>Actualizado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="estatus in lista" :key="estatus.id" :class="!estatus.vigente && 'opacity-60'">
              <td class="text-secondary">{{ estatus.id }}</td>
              <td><span :class="claseChip(estatus.valor)">{{ estatus.valor }}</span></td>
              <td class="max-w-xs text-ink/80">{{ estatus.descripcion }}</td>
              <td>
                <span :class="estatus.esSistema ? 'pill-tp' : 'pill-tp-personalizado'">
                  {{ estatus.esSistema ? 'Sistema' : 'Personalizado' }}
                </span>
              </td>
              <td class="text-ink/70">{{ etiquetaEnUso(estatus) }}</td>
              <td>
                <span class="text-sm font-semibold" :class="estatus.vigente ? 'text-success' : 'text-secondary'">
                  {{ estatus.vigente ? 'Vigente' : 'De baja' }}
                </span>
              </td>
              <td class="text-secondary">{{ formatearFecha(estatus.actualizadoEn) }}</td>
              <td>
                <div class="flex items-center gap-3">
                  <RouterLink :to="{ name: 'catalogos-estatus-editar', params: { id: estatus.id } }" class="boton-tp-texto text-primary">
                    Editar
                  </RouterLink>
                  <button v-if="estatus.vigente" type="button" class="boton-tp-texto text-danger"
                          :disabled="estatus.esSistema || estatus.clientesEnUso > 0"
                          :title="estatus.esSistema ? 'Los estatus del sistema no se pueden dar de baja' : (estatus.clientesEnUso > 0 ? 'En uso: no se puede dar de baja' : '')"
                          @click="darDeBaja(estatus)">
                    Dar de baja
                  </button>
                  <button v-else type="button" class="boton-tp-texto text-success" @click="restaurar(estatus)">
                    Restaurar
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
