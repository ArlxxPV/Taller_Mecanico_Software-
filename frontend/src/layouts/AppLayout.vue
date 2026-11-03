<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '../stores/auth'
import { authFacade } from '../facades/authFacade'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const auth = useAuthStore()
const cargando = ref(true)

const puedeRegistrarClientes = computed(() => {
  const roles = auth.rolesUsuario
  return roles.includes('ADMIN') || roles.includes('RECEPCIONISTA')
})

const esAdmin = computed(() => auth.rolesUsuario.includes('ADMIN'))
const puedeVerPersonal = computed(() =>
  auth.permisosUsuario.includes('ROLES_ADMINISTRAR') ||
  auth.permisosUsuario.includes('USUARIOS_VER') ||
  auth.permisosUsuario.includes('USUARIOS_CREAR')
)
const puedeAdministrarEstatus = computed(() => auth.permisosUsuario.includes('ESTATUS_ADMINISTRAR'))

// Items normales del menu. Las de "CATALOGOS" se arman aparte (seccion con
// encabezado propio, ver template). Si "ruta" es null, el item se muestra
// pero no navega a ningun lado (hoy ninguna entrada esta en ese estado, se
// deja por si una seccion vuelve a quedar restringida).
const navegacion = computed(() => [
  { icono: 'pi-home', etiqueta: 'Panel', ruta: 'panel' },
  { icono: 'pi-users', etiqueta: 'Clientes', ruta: puedeRegistrarClientes.value ? 'clientes-lista' : null },
  { icono: 'pi-car', etiqueta: 'Vehiculos', ruta: puedeRegistrarClientes.value ? 'vehiculos-lista' : null },
  { icono: 'pi-building', etiqueta: 'Talleres', ruta: esAdmin.value ? 'talleres-lista' : null },
  { icono: 'pi-id-card', etiqueta: 'Roles y personal', ruta: puedeVerPersonal.value ? 'personal' : null },
  { icono: 'pi-file-edit', etiqueta: 'Ordenes', proximamente: true },
  { icono: 'pi-box', etiqueta: 'Inventario', proximamente: true }
])

const navegacionCatalogos = computed(() => [
  { icono: 'pi-tags', etiqueta: 'Estatus', ruta: puedeAdministrarEstatus.value ? 'catalogos-estatus' : null }
].filter((item) => item.ruta))

function estaActivo(nombreRuta) {
  return route.matched.some((r) => r.name === nombreRuta)
}

const migas = computed(() => route.meta.breadcrumb || [])

const nombre = computed(() => auth.usuario?.nombreCompleto ?? '')
const rolPrincipal = computed(() => auth.usuario?.roles?.[0] ?? '')
const inicial = computed(() => (nombre.value || auth.usuario?.email || '?').charAt(0).toUpperCase())

// Barra lateral desplegable: colapsa a solo iconos (se guarda la preferencia
// para que no "salte" cada vez que se entra al panel).
const sidebarAbierto = ref(localStorage.getItem('tallerpro_sidebar') !== 'cerrado')
function alternarSidebar() {
  sidebarAbierto.value = !sidebarAbierto.value
  localStorage.setItem('tallerpro_sidebar', sidebarAbierto.value ? 'abierto' : 'cerrado')
}

onMounted(async () => {
  if (!auth.usuario) {
    const resultado = await authFacade.cargarPerfil()
    if (!resultado.ok) {
      toast.add({ severity: 'error', summary: 'No se pudo cargar tu perfil', detail: resultado.mensaje, life: 4000 })
    }
  }
  cargando.value = false
})

function salir() {
  const resultado = authFacade.cerrarSesion()
  toast.add({ severity: 'success', summary: 'Hasta pronto', detail: resultado.mensaje, life: 2500 })
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="flex min-h-screen bg-paper">
    <!-- Barra lateral, desplegable (colapsa a solo iconos) -->
    <aside
      class="hidden flex-col justify-between bg-sidebar text-white transition-all duration-300 md:flex"
      :class="sidebarAbierto ? 'w-64 px-5 py-7' : 'w-20 px-3 py-7'"
    >
      <div>
        <div class="flex items-center gap-2.5" :class="!sidebarAbierto && 'justify-center'">
          <span class="flex h-[34px] w-[34px] shrink-0 items-center justify-center rounded-[9px] bg-accentStrong text-white">
            <span class="font-display text-sm font-bold">T</span>
          </span>
          <span v-if="sidebarAbierto" class="font-display text-lg font-bold tracking-tight">TallerPro</span>
        </div>

        <nav class="mt-10 space-y-1">
          <component
            :is="item.ruta ? 'RouterLink' : 'div'"
            v-for="item in navegacion"
            :key="item.etiqueta"
            :to="item.ruta ? { name: item.ruta } : undefined"
            class="flex items-center gap-3 rounded-[10px] px-3.5 py-3 text-sm"
            :class="[
              item.ruta && estaActivo(item.ruta) ? 'bg-primary font-semibold text-white' : item.ruta ? 'text-[#CBD5E1] hover:bg-white/10 hover:text-white' : 'text-[#CBD5E1]/70',
              !sidebarAbierto && 'justify-center'
            ]"
            :title="!sidebarAbierto ? item.etiqueta : undefined"
          >
            <i class="pi" :class="item.icono"></i>
            <span v-if="sidebarAbierto">{{ item.etiqueta }}</span>
            <span v-if="sidebarAbierto && item.proximamente" class="ml-auto rounded-full bg-[#22314D] px-2 py-0.5 text-[11px] text-[#9AA8BF]">
              próximamente
            </span>
          </component>

          <template v-if="navegacionCatalogos.length">
            <p v-if="sidebarAbierto" class="mb-1 mt-6 px-3.5 text-[11px] font-bold uppercase tracking-[0.08em] text-[#9AA8BF]">
              Catálogos
            </p>
            <div v-else class="my-4 border-t border-[#2A3A57]"></div>
            <component
              :is="item.ruta ? 'RouterLink' : 'div'"
              v-for="item in navegacionCatalogos"
              :key="item.etiqueta"
              :to="item.ruta ? { name: item.ruta } : undefined"
              class="flex items-center gap-3 rounded-[10px] px-3.5 py-3 text-sm"
              :class="[
                item.ruta && estaActivo(item.ruta) ? 'bg-primary font-semibold text-white' : 'text-[#CBD5E1] hover:bg-white/10 hover:text-white',
                !sidebarAbierto && 'justify-center'
              ]"
              :title="!sidebarAbierto ? item.etiqueta : undefined"
            >
              <i class="pi" :class="item.icono"></i>
              <span v-if="sidebarAbierto">{{ item.etiqueta }}</span>
            </component>
          </template>
        </nav>
      </div>

      <!-- Sesion actual + cerrar sesion -->
      <div class="border-t border-[#2A3A57] pt-4">
        <div class="flex items-center gap-2.5 rounded-lg px-2 py-2" :class="!sidebarAbierto && 'justify-center'" :title="!sidebarAbierto ? nombre : undefined">
          <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-accentStrong text-sm font-semibold">
            {{ inicial }}
          </span>
          <div v-if="sidebarAbierto" class="min-w-0">
            <p class="truncate text-sm font-medium text-white">{{ nombre || 'Cargando...' }}</p>
            <p class="truncate text-xs text-[#9AA8BF]">{{ rolPrincipal }}</p>
          </div>
        </div>
        <button
          class="mt-1 flex w-full items-center gap-3 rounded-lg border border-[#2A3A57] px-3 py-2.5 text-sm text-white/70 hover:bg-white/5 hover:text-white"
          :class="!sidebarAbierto && 'justify-center'"
          title="Cerrar sesion"
          @click="salir"
        >
          <i class="pi pi-sign-out"></i>
          <span v-if="sidebarAbierto">Cerrar sesión</span>
        </button>
      </div>
    </aside>

    <!-- Columna derecha: barra superior + contenido -->
    <div class="flex flex-1 flex-col">
      <!-- Barra superior -->
      <header class="flex items-center justify-between border-b border-mist bg-white px-4 py-3.5 sm:px-8">
        <div class="flex items-center gap-3">
          <button
            class="hidden h-9 w-9 items-center justify-center rounded-lg text-ink/60 transition hover:bg-mist/60 md:flex"
            title="Mostrar u ocultar el menu"
            @click="alternarSidebar"
          >
            <i class="pi" :class="sidebarAbierto ? 'pi-angle-double-left' : 'pi-angle-double-right'"></i>
          </button>

          <div class="flex items-center gap-2 md:hidden">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-accentStrong text-white">
              <span class="font-display text-sm font-bold">T</span>
            </span>
            <span class="font-display text-lg font-bold text-ink">TallerPro</span>
          </div>

          <nav class="hidden items-center gap-1.5 text-sm sm:flex">
            <RouterLink :to="{ name: 'panel' }" class="font-medium" :class="migas.length ? 'text-secondary hover:text-ink' : 'text-ink'">
              Panel
            </RouterLink>
            <template v-for="(miga, i) in migas" :key="i">
              <span class="text-secondary">/</span>
              <span :class="i === migas.length - 1 ? 'font-semibold text-ink' : 'text-secondary'">{{ miga }}</span>
            </template>
          </nav>
        </div>

        <div class="flex items-center gap-3">
          <span v-if="auth.estadoClienteUsuario" class="insignia-estado"
                :class="auth.estadoClienteUsuario === 'SUSPENDIDO' ? 'bg-red-100 text-red-700' : 'bg-emerald-100 text-emerald-700'">
            {{ auth.estadoClienteUsuario === 'SUSPENDIDO' ? 'Suspendido' : 'Activo' }}
          </span>
          <span v-if="rolPrincipal" class="hidden rounded-full bg-[#E8EFFD] px-3 py-1.5 text-xs font-bold uppercase tracking-wide text-[#1E40AF] sm:inline">
            {{ rolPrincipal }}
          </span>
          <span class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-semibold text-white" :title="nombre">
            {{ inicial }}
          </span>
          <button class="flex h-9 w-9 items-center justify-center rounded-lg text-ink/50 transition hover:bg-mist/60 hover:text-ink md:hidden"
                  title="Cerrar sesion" @click="salir">
            <i class="pi pi-sign-out"></i>
          </button>
        </div>
      </header>

      <!-- Contenido de cada pantalla -->
      <main class="flex-1 px-6 py-8 sm:px-10">
        <div v-if="cargando" class="mx-auto max-w-6xl text-center text-sm text-secondary">Cargando...</div>
        <RouterView v-else />
      </main>
    </div>
  </div>
</template>
