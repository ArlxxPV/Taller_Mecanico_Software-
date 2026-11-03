<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '../stores/auth'
import { panelFacade } from '../facades/panelFacade'
import { estatusFacade } from '../facades/estatusFacade'

const toast = useToast()
const auth = useAuthStore()

// Panel "completo" (indicadores, accesos rapidos, ultimos movimientos) solo
// para quien trabaja con clientes. Un CLIENTE autoservicio (sin permisos)
// sigue viendo el panel simple de siempre (ultimo acceso + correo) - no se
// le rompe nada de lo que ya tenia.
const puedeVerPanelCompleto = computed(() =>
  auth.permisosUsuario.includes('CLIENTES_VER') ||
  auth.permisosUsuario.includes('CLIENTES_CREAR') ||
  auth.permisosUsuario.includes('CLIENTES_EDITAR'))
const puedeAdministrarEstatus = computed(() => auth.permisosUsuario.includes('ESTATUS_ADMINISTRAR'))

const nombre = computed(() => auth.usuario?.nombreCompleto ?? '')

const ultimoAcceso = computed(() => {
  if (!auth.usuario?.ultimoLoginEn) return 'Este es tu primer inicio de sesion registrado.'
  const fecha = new Date(auth.usuario.ultimoLoginEn)
  return fecha.toLocaleString('es-MX', { dateStyle: 'long', timeStyle: 'short' })
})

const resumen = ref({ clientesActivos: 0, clientesSuspendidos: 0, vehiculosRegistrados: 0, clientesNuevosEsteMes: 0 })
const movimientos = ref([])
const cargandoPanel = ref(true)
const idEstatus = ref({})

const ETIQUETAS_ORIGEN = { ADMIN: 'Administrador', RECEPCIONISTA: 'Recepción', AUTORREGISTRO: 'Autorregistro' }
function etiquetaOrigen(origen) {
  return ETIQUETAS_ORIGEN[origen] || origen || '—'
}
function claseChip(valor) {
  if (valor === 'ACTIVO') return 'chip-estatus-activo'
  if (valor === 'SUSPENDIDO') return 'chip-estatus-suspendido'
  if (valor === 'ACTUALIZADO') return 'chip-estatus-actualizado'
  return 'chip-estatus-neutro'
}
function formatearFecha(fechaIso) {
  if (!fechaIso) return '—'
  return new Date(fechaIso).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' })
}
function detalleDe(mov) {
  if (mov.movimiento !== 'ACTUALIZADO' || !mov.cambios) return mov.detalle || '—'
  try {
    return Object.keys(JSON.parse(mov.cambios)).join(', ')
  } catch {
    return mov.detalle || '—'
  }
}

onMounted(async () => {
  if (!puedeVerPanelCompleto.value) return

  const resultadoEstatus = await estatusFacade.listar()
  if (resultadoEstatus.ok) {
    for (const e of resultadoEstatus.estatus) idEstatus.value[e.valor] = e.id
  }

  const [resResumen, resMovs] = await Promise.all([panelFacade.resumen(), panelFacade.ultimosMovimientos()])
  if (resResumen.ok) resumen.value = resResumen.resumen
  else toast.add({ severity: 'error', summary: 'No se pudo cargar el resumen', detail: resResumen.mensaje, life: 4000 })

  if (resMovs.ok) movimientos.value = resMovs.movimientos
  else toast.add({ severity: 'error', summary: 'No se pudieron cargar los movimientos', detail: resMovs.mensaje, life: 4000 })

  cargandoPanel.value = false
})
</script>

<template>
  <div class="mx-auto max-w-6xl">
    <div class="mb-8">
      <h1 class="titulo-pagina">Hola, {{ nombre || '...' }}</h1>
      <p class="subtitulo-pagina">
        {{ puedeVerPanelCompleto ? 'Resumen de clientes y vehículos del taller.' : 'Este es el modulo de identidad, usuarios y roles.' }}
      </p>
    </div>

    <section class="grid gap-5 sm:grid-cols-2">
      <div class="tarjeta-tp">
        <p class="text-sm text-secondary">Ultimo acceso</p>
        <p class="mt-2 font-display text-lg font-semibold text-ink">{{ ultimoAcceso }}</p>
        <p v-if="auth.usuario?.ultimoLoginIp" class="mt-1 font-mono text-xs text-secondary/70">
          IP: {{ auth.usuario.ultimoLoginIp }}
        </p>
      </div>

      <div class="tarjeta-tp">
        <p class="text-sm text-secondary">Correo de la cuenta</p>
        <p class="mt-2 break-all font-display text-lg font-semibold text-ink">{{ auth.usuario?.email }}</p>
      </div>
    </section>

    <template v-if="puedeVerPanelCompleto">
      <h2 class="mb-3 mt-8 font-display text-base font-semibold text-ink">Indicadores</h2>
      <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <RouterLink :to="{ name: 'clientes-lista', query: { estatusId: idEstatus.ACTIVO } }" class="tarjeta-tp transition hover:border-primary/40">
          <p class="text-sm text-secondary">Clientes activos</p>
          <p class="mt-1 font-display text-[28px] font-bold text-success">{{ resumen.clientesActivos }}</p>
          <p class="mt-1 text-[13px] font-semibold text-primary">Ver lista →</p>
        </RouterLink>
        <RouterLink :to="{ name: 'clientes-lista', query: { estatusId: idEstatus.SUSPENDIDO } }" class="tarjeta-tp transition hover:border-primary/40">
          <p class="text-sm text-secondary">Clientes suspendidos</p>
          <p class="mt-1 font-display text-[28px] font-bold text-danger">{{ resumen.clientesSuspendidos }}</p>
          <p class="mt-1 text-[13px] font-semibold text-primary">Ver lista →</p>
        </RouterLink>
        <RouterLink :to="{ name: 'vehiculos-lista' }" class="tarjeta-tp transition hover:border-primary/40">
          <p class="text-sm text-secondary">Vehículos registrados</p>
          <p class="mt-1 font-display text-[28px] font-bold text-ink">{{ resumen.vehiculosRegistrados }}</p>
          <p class="mt-1 text-[13px] font-semibold text-primary">Ver vehículos →</p>
        </RouterLink>
        <RouterLink :to="{ name: 'clientes-lista' }" class="tarjeta-tp transition hover:border-primary/40">
          <p class="text-sm text-secondary">Clientes nuevos este mes</p>
          <p class="mt-1 font-display text-[28px] font-bold text-ink">{{ resumen.clientesNuevosEsteMes }}</p>
          <p class="mt-1 text-[13px] font-semibold text-primary">Ver lista →</p>
        </RouterLink>
      </section>

      <h2 class="mb-3 mt-8 font-display text-base font-semibold text-ink">Accesos rápidos</h2>
      <section class="flex flex-wrap gap-3">
        <RouterLink :to="{ name: 'clientes-nuevo' }" class="boton-tp-primario">+ Registrar cliente</RouterLink>
        <RouterLink :to="{ name: 'clientes-lista' }" class="boton-tp-secundario">Asociar vehículo</RouterLink>
        <RouterLink v-if="puedeAdministrarEstatus" :to="{ name: 'catalogos-estatus' }" class="boton-tp-secundario">Catálogo de estatus</RouterLink>
      </section>

      <div class="mt-8 flex items-center justify-between">
        <h2 class="font-display text-base font-semibold text-ink">Últimos movimientos</h2>
        <RouterLink :to="{ name: 'clientes-lista' }" class="text-[13px] font-semibold text-primary hover:underline">Ver clientes</RouterLink>
      </div>
      <div class="mt-3 overflow-hidden rounded-[14px] border border-mist bg-white">
        <div v-if="cargandoPanel" class="p-8 text-center text-sm text-secondary">Cargando...</div>
        <div v-else-if="!movimientos.length" class="p-8 text-center text-sm text-secondary">Sin movimientos recientes.</div>
        <div v-else class="overflow-x-auto">
          <table class="tabla-tp w-full">
            <thead>
              <tr><th>Cliente</th><th>Movimiento</th><th>Detalle</th><th>Usuario</th><th>Fecha</th></tr>
            </thead>
            <tbody>
              <tr v-for="(mov, i) in movimientos" :key="i">
                <td>
                  <RouterLink :to="{ name: 'clientes-detalle', params: { id: mov.clienteId } }" class="font-medium text-primary hover:underline">
                    {{ mov.clienteNombre }}
                  </RouterLink>
                </td>
                <td><span :class="claseChip(mov.movimiento)">{{ mov.movimiento }}</span></td>
                <td class="text-ink/70">{{ detalleDe(mov) }}</td>
                <td class="text-ink/70">{{ mov.usuarioNombre || etiquetaOrigen(mov.origenRegistro) }}</td>
                <td class="text-secondary">{{ formatearFecha(mov.fecha) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
