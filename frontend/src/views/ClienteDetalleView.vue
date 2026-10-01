<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { clienteFacade } from '../facades/clienteFacade'

const route = useRoute()
const toast = useToast()

const cliente = ref(null)
const fotoUrl = ref('')
const cargando = ref(true)
const noEncontrado = ref(false)

function formatearFecha(fechaIso, conHora = false) {
  if (!fechaIso) return '—'
  const fecha = new Date(fechaIso)
  // toLocaleDateString no acepta "timeStyle" (solo "dateStyle"); para
  // incluir la hora hay que usar toLocaleString.
  return conHora
    ? fecha.toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' })
    : fecha.toLocaleDateString('es-MX', { dateStyle: 'long' })
}

// watch (no onMounted): si ya estabas en el detalle de un cliente y navegas
// al de otro (mismo componente, cambia solo el :id en la URL), Vue reutiliza
// la instancia y "mounted" no vuelve a dispararse. Con watch + immediate se
// vuelve a cargar cada vez que cambia el id, incluida la primera vez.
watch(
  () => route.params.id,
  async (id) => {
    cargando.value = true
    noEncontrado.value = false
    cliente.value = null
    fotoUrl.value = ''

    const resultado = await clienteFacade.obtener(id)
    if (!resultado.ok) {
      noEncontrado.value = true
      toast.add({ severity: 'error', summary: 'No se pudo abrir el cliente', detail: resultado.mensaje, life: 4500 })
      cargando.value = false
      return
    }

    cliente.value = resultado.cliente
    if (cliente.value.tieneFoto) {
      fotoUrl.value = (await clienteFacade.obtenerFotoUrl(id)) || ''
    }
    cargando.value = false
  },
  { immediate: true }
)
</script>

<template>
  <div class="min-h-screen bg-paper px-6 py-10 sm:px-10">
    <div class="mx-auto max-w-3xl">
      <RouterLink :to="{ name: 'clientes-lista' }" class="text-sm font-medium text-primary hover:text-primary-hover">
        ← Volver a clientes
      </RouterLink>

      <div v-if="cargando" class="mt-10 text-center text-sm text-ink/50">Cargando...</div>

      <div v-else-if="noEncontrado" class="mt-10 text-center text-sm text-ink/60">
        No se encontró este cliente.
      </div>

      <template v-else>
        <div class="mt-4 flex flex-wrap items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <img v-if="fotoUrl" :src="fotoUrl" :alt="`Foto de ${cliente.nombreCompleto}`"
                 class="h-16 w-16 rounded-full border border-mist object-cover" />
            <div v-else class="flex h-16 w-16 items-center justify-center rounded-full border border-dashed border-mist text-ink/30">
              <i class="pi pi-user text-2xl"></i>
            </div>
            <div>
              <h1 class="font-display text-2xl font-bold text-ink">{{ cliente.nombreCompleto }}</h1>
              <p class="text-sm text-ink/50">Registrado el {{ formatearFecha(cliente.creadoEn, true) }}</p>
            </div>
          </div>
          <RouterLink :to="{ name: 'clientes-editar', params: { id: route.params.id } }" class="boton-primario w-auto px-5 py-2.5">
            Editar cliente
          </RouterLink>
        </div>

        <div class="mt-8 grid gap-5 sm:grid-cols-2">
          <section class="rounded-2xl border border-mist bg-white p-6">
            <h2 class="font-display text-base font-semibold text-ink">Datos personales</h2>
            <dl class="mt-3 space-y-2 text-sm">
              <div><dt class="text-ink/50">Contacto alternativo</dt><dd class="text-ink">{{ cliente.contactoAlternativo || '—' }}</dd></div>
              <div><dt class="text-ink/50">Edad</dt><dd class="text-ink">{{ cliente.edad ?? '—' }}</dd></div>
              <div><dt class="text-ink/50">Fecha de nacimiento</dt><dd class="text-ink">{{ formatearFecha(cliente.fechaNacimiento) }}</dd></div>
            </dl>
          </section>

          <section class="rounded-2xl border border-mist bg-white p-6">
            <h2 class="font-display text-base font-semibold text-ink">Contacto</h2>
            <dl class="mt-3 space-y-2 text-sm">
              <div><dt class="text-ink/50">Teléfono personal</dt><dd class="text-ink">{{ cliente.telefonoPersonal || '—' }}</dd></div>
              <div><dt class="text-ink/50">Teléfono de trabajo</dt><dd class="text-ink">{{ cliente.telefonoTrabajo || '—' }}</dd></div>
              <div><dt class="text-ink/50">Correo</dt><dd class="break-all text-ink">{{ cliente.email || '—' }}</dd></div>
              <div><dt class="text-ink/50">Correo de trabajo</dt><dd class="break-all text-ink">{{ cliente.emailTrabajo || '—' }}</dd></div>
            </dl>
          </section>

          <section class="rounded-2xl border border-mist bg-white p-6 sm:col-span-2">
            <h2 class="font-display text-base font-semibold text-ink">Dirección</h2>
            <dl class="mt-3 grid gap-2 text-sm sm:grid-cols-2">
              <div><dt class="text-ink/50">Calle</dt><dd class="text-ink">{{ cliente.calle || '—' }}</dd></div>
              <div><dt class="text-ink/50">Colonia</dt><dd class="text-ink">{{ cliente.colonia || '—' }}</dd></div>
              <div><dt class="text-ink/50">Municipio</dt><dd class="text-ink">{{ cliente.municipio || '—' }}</dd></div>
              <div><dt class="text-ink/50">Estado</dt><dd class="text-ink">{{ cliente.estado || '—' }}</dd></div>
              <div><dt class="text-ink/50">Código postal</dt><dd class="text-ink">{{ cliente.codigoPostal || '—' }}</dd></div>
            </dl>
          </section>
        </div>
      </template>
    </div>
  </div>
</template>
