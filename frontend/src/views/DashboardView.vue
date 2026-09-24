<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const cargando = ref(true)

const navegacion = [
  { icono: 'pi-home', etiqueta: 'Panel', activo: true },
  { icono: 'pi-users', etiqueta: 'Clientes', activo: false },
  { icono: 'pi-car', etiqueta: 'Vehiculos', activo: false },
  { icono: 'pi-file-edit', etiqueta: 'Ordenes', activo: false },
  { icono: 'pi-box', etiqueta: 'Inventario', activo: false }
]

const nombre = computed(() => auth.usuario?.nombreCompleto ?? '')
const rolPrincipal = computed(() => auth.usuario?.roles?.[0] ?? '')

const ultimoAcceso = computed(() => {
  if (!auth.usuario?.ultimoLoginEn) return 'Este es tu primer inicio de sesion registrado.'
  const fecha = new Date(auth.usuario.ultimoLoginEn)
  return fecha.toLocaleString('es-MX', { dateStyle: 'long', timeStyle: 'short' })
})

onMounted(async () => {
  try {
    await auth.cargarPerfil()
  } finally {
    cargando.value = false
  }
})

function salir() {
  auth.cerrarSesion()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="flex min-h-screen bg-paper">
    <!-- Barra lateral -->
    <aside class="hidden w-64 flex-col justify-between bg-ink px-5 py-7 text-white md:flex">
      <div>
        <span class="font-display text-lg font-semibold tracking-tight">TallerPro</span>

        <nav class="mt-10 space-y-1">
          <div
            v-for="item in navegacion"
            :key="item.etiqueta"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm"
            :class="item.activo ? 'bg-primary text-white' : 'text-white/55'"
          >
            <i class="pi" :class="item.icono"></i>
            <span>{{ item.etiqueta }}</span>
            <span v-if="!item.activo" class="ml-auto rounded-full bg-white/10 px-2 py-0.5 text-[10px] text-white/50">
              proximamente
            </span>
          </div>
        </nav>
      </div>

      <button class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-white/60 hover:bg-white/5 hover:text-white" @click="salir">
        <i class="pi pi-sign-out"></i>
        Cerrar sesion
      </button>
    </aside>

    <!-- Contenido -->
    <main class="flex-1 px-6 py-8 sm:px-10">
      <header class="mb-8 flex items-center justify-between">
        <div>
          <h1 class="font-display text-2xl font-semibold text-ink">Hola, {{ nombre || '...' }}</h1>
          <p class="mt-1 text-sm text-ink/60">Este es el modulo de identidad, usuarios y roles.</p>
        </div>
        <span v-if="rolPrincipal" class="rounded-full bg-mist px-3 py-1.5 text-xs font-medium uppercase tracking-wide text-primary">
          {{ rolPrincipal }}
        </span>
      </header>

      <section class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-2xl border border-mist bg-white p-5">
          <p class="text-sm text-ink/50">Ultimo acceso</p>
          <p class="mt-2 font-display text-lg font-semibold text-ink">{{ ultimoAcceso }}</p>
          <p v-if="auth.usuario?.ultimoLoginIp" class="mt-1 font-mono text-xs text-ink/40">
            IP: {{ auth.usuario.ultimoLoginIp }}
          </p>
        </div>

        <div class="rounded-2xl border border-mist bg-white p-5">
          <p class="text-sm text-ink/50">Correo de la cuenta</p>
          <p class="mt-2 break-all font-display text-lg font-semibold text-ink">{{ auth.usuario?.email }}</p>
        </div>

        <div class="rounded-2xl border border-dashed border-mist bg-white/60 p-5">
          <p class="text-sm text-ink/50">Siguiente modulo</p>
          <p class="mt-2 font-display text-lg font-semibold text-ink/70">Gestion de clientes y vehiculos</p>
          <p class="mt-1 text-sm text-ink/50">Las pantallas de esta seccion llegan en la fase 2.</p>
        </div>
      </section>
    </main>
  </div>
</template>
