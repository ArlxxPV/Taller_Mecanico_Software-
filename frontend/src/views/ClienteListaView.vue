<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { clienteFacade } from '../facades/clienteFacade'

const toast = useToast()

const clientes = ref([])
const cargando = ref(true)

const hayClientes = computed(() => clientes.value.length > 0)

function formatearFecha(fechaIso) {
  if (!fechaIso) return '—'
  return new Date(fechaIso).toLocaleDateString('es-MX', { dateStyle: 'medium' })
}

async function cargar() {
  cargando.value = true
  const resultado = await clienteFacade.listar()
  cargando.value = false

  if (resultado.ok) {
    clientes.value = resultado.clientes
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar la lista', detail: resultado.mensaje, life: 4500 })
  }
}

onMounted(cargar)
</script>

<template>
  <div class="min-h-screen bg-paper px-6 py-10 sm:px-10">
    <div class="mx-auto max-w-5xl">
      <RouterLink :to="{ name: 'panel' }" class="text-sm font-medium text-primary hover:text-primary-hover">
        ← Volver al panel
      </RouterLink>

      <div class="mt-3 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="font-display text-2xl font-bold text-ink">Clientes</h1>
          <p class="mt-1 text-sm text-ink/60">Clientes ya registrados en el taller.</p>
        </div>
        <RouterLink :to="{ name: 'clientes-nuevo' }" class="boton-primario w-auto px-5 py-2.5">
          + Registrar cliente
        </RouterLink>
      </div>

      <div class="mt-8 overflow-hidden rounded-2xl border border-mist bg-white">
        <div v-if="cargando" class="p-10 text-center text-sm text-ink/50">
          Cargando clientes...
        </div>

        <div v-else-if="!hayClientes" class="p-10 text-center">
          <p class="text-sm text-ink/60">Aún no hay clientes registrados.</p>
          <RouterLink :to="{ name: 'clientes-nuevo' }" class="mt-3 inline-block text-sm font-medium text-primary hover:text-primary-hover">
            Registrar el primero
          </RouterLink>
        </div>

        <table v-else class="w-full text-left text-sm">
          <thead class="bg-mist/50 text-xs uppercase tracking-wide text-ink/50">
            <tr>
              <th class="px-5 py-3 font-medium">Nombre</th>
              <th class="px-5 py-3 font-medium">Teléfono</th>
              <th class="px-5 py-3 font-medium">Correo</th>
              <th class="px-5 py-3 font-medium">Ubicación</th>
              <th class="px-5 py-3 font-medium">Registrado</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-mist">
            <tr
              v-for="cliente in clientes"
              :key="cliente.id"
              class="cursor-pointer hover:bg-paper"
              @click="$router.push({ name: 'clientes-detalle', params: { id: cliente.id } })"
            >
              <td class="px-5 py-3">
                <div class="flex items-center gap-2">
                  <span
                    class="flex h-8 w-8 items-center justify-center rounded-full bg-mist text-xs font-semibold text-primary"
                    :title="cliente.tieneFoto ? 'Tiene foto registrada' : 'Sin foto'"
                  >
                    {{ cliente.nombreCompleto?.charAt(0)?.toUpperCase() || '?' }}
                  </span>
                  <span class="font-medium text-ink">{{ cliente.nombreCompleto }}</span>
                </div>
              </td>
              <td class="px-5 py-3 text-ink/70">{{ cliente.telefonoPersonal || '—' }}</td>
              <td class="px-5 py-3 text-ink/70">{{ cliente.email || '—' }}</td>
              <td class="px-5 py-3 text-ink/70">
                {{ [cliente.municipio, cliente.estado].filter(Boolean).join(', ') || '—' }}
              </td>
              <td class="px-5 py-3 text-ink/50">{{ formatearFecha(cliente.creadoEn) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
