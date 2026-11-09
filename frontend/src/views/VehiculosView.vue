<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { vehiculoFacade } from '../facades/vehiculoFacade'
import { estatusFacade } from '../facades/estatusFacade'
import { formatearPlaca } from '../utils/vehiculos'
import ModalEditarVehiculo from '../components/ModalEditarVehiculo.vue'
import ModalCambiarClienteVehiculo from '../components/ModalCambiarClienteVehiculo.vue'
import ModalBase from '../components/ModalBase.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const puedeEditar = computed(() => auth.permisosUsuario.includes('CLIENTES_CREAR') || auth.permisosUsuario.includes('CLIENTES_EDITAR'))

const modalEditarAbierto = ref(false)
const vehiculoEditando = ref(null)

function abrirEditar(vehiculo) {
  vehiculoEditando.value = vehiculo
  modalEditarAbierto.value = true
}

function alGuardarEdicion() {
  modalEditarAbierto.value = false
  cargar()
}

const modalCambiarClienteAbierto = ref(false)
const vehiculoReasignando = ref(null)

function abrirCambiarCliente(vehiculo) {
  vehiculoReasignando.value = vehiculo
  modalCambiarClienteAbierto.value = true
}

function alCambiarCliente() {
  modalCambiarClienteAbierto.value = false
  cargar()
}

// Activar/desactivar: el usuario confirma antes de aplicar el cambio (ver orquestador).
const modalEstatusAbierto = ref(false)
const vehiculoCambiandoEstatus = ref(null)
const guardandoEstatus = ref(false)
const accionEstatus = computed(() => vehiculoCambiandoEstatus.value?.estatus === 'ACTIVO' ? 'desactivar' : 'activar')

function abrirCambiarEstatus(vehiculo) {
  vehiculoCambiandoEstatus.value = vehiculo
  modalEstatusAbierto.value = true
}

async function confirmarCambiarEstatus() {
  guardandoEstatus.value = true
  const resultado = accionEstatus.value === 'desactivar'
    ? await vehiculoFacade.desactivar(vehiculoCambiandoEstatus.value.id)
    : await vehiculoFacade.activar(vehiculoCambiandoEstatus.value.id)
  guardandoEstatus.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: accionEstatus.value === 'desactivar' ? 'Vehículo desactivado' : 'Vehículo activado', detail: resultado.mensaje, life: 3000 })
    modalEstatusAbierto.value = false
    cargar()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo aplicar el cambio', detail: resultado.mensaje, life: 4500 })
  }
}

const route = useRoute()
const router = useRouter()
const toast = useToast()

const vehiculos = ref([])
const cargando = ref(true)
const estatusCatalogo = ref([])

const busqueda = ref(route.query.q || '')
const busquedaAplicada = ref(route.query.q || '')
const filtroEstatus = ref(route.query.estatusId || '')
const paginaActual = ref(Number(route.query.pagina) || 0)
const totalPaginas = ref(0)
const totalElementos = ref(0)

let temporizador = null
watch(busqueda, (valor) => {
  clearTimeout(temporizador)
  temporizador = setTimeout(() => { busquedaAplicada.value = valor }, 400)
})

function claseChip(valor) {
  if (valor === 'ACTIVO') return 'chip-estatus-activo'
  if (valor === 'SUSPENDIDO') return 'chip-estatus-suspendido'
  return 'chip-estatus-neutro'
}
function formatearFecha(fechaIso) {
  if (!fechaIso) return '—'
  return new Date(fechaIso).toLocaleDateString('es-MX', { dateStyle: 'medium' })
}

async function cargar() {
  cargando.value = true
  const resultado = await vehiculoFacade.listar({
    estatusId: filtroEstatus.value || undefined,
    q: busquedaAplicada.value || undefined,
    pagina: paginaActual.value,
    porPagina: 10
  })
  cargando.value = false
  if (resultado.ok) {
    vehiculos.value = resultado.pagina.vehiculos
    totalPaginas.value = resultado.pagina.totalPaginas
    totalElementos.value = resultado.pagina.totalElementos
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar la lista', detail: resultado.mensaje, life: 4500 })
  }
}

watch([filtroEstatus, busquedaAplicada], () => { paginaActual.value = 0 })
watch([filtroEstatus, busquedaAplicada, paginaActual], () => {
  router.replace({ query: { q: busquedaAplicada.value || undefined, estatusId: filtroEstatus.value || undefined, pagina: paginaActual.value || undefined } })
  cargar()
})

onMounted(async () => {
  const resultado = await estatusFacade.listar()
  if (resultado.ok) estatusCatalogo.value = resultado.estatus
  cargar()
})
</script>

<template>
  <div class="mx-auto max-w-6xl">
    <h1 class="titulo-pagina">Vehículos</h1>
    <p class="subtitulo-pagina">Vehículos asociados a los clientes del taller.</p>

    <div class="caja-info-tp mt-6 flex flex-wrap items-center justify-between gap-3">
      <span>Para registrar un vehículo, elige primero al cliente en Administración de clientes.</span>
      <RouterLink :to="{ name: 'clientes-lista' }" class="boton-tp-secundario shrink-0">Ir a clientes</RouterLink>
    </div>

    <div class="tarjeta-tp mt-6 flex flex-wrap items-end gap-4">
      <div class="min-w-[220px] flex-1">
        <label class="etiqueta-tp" for="buscar">Buscar</label>
        <input id="buscar" v-model="busqueda" type="text" class="campo-tp" placeholder="Placa o VIN" />
      </div>
      <div class="w-full sm:w-52">
        <label class="etiqueta-tp" for="filtroEstatus">Estatus</label>
        <select id="filtroEstatus" v-model="filtroEstatus" class="campo-tp">
          <option value="">Todos</option>
          <option v-for="e in estatusCatalogo.filter(x => ['ACTIVO', 'SUSPENDIDO'].includes(x.valor))" :key="e.id" :value="String(e.id)">
            {{ e.valor }}
          </option>
        </select>
      </div>
    </div>

    <div class="mt-6 overflow-hidden rounded-[14px] border border-mist bg-white">
      <div v-if="cargando" class="p-10 text-center text-sm text-secondary">Cargando...</div>
      <div v-else-if="!vehiculos.length" class="p-10 text-center text-sm text-secondary">No hay vehículos con estos filtros.</div>

      <div v-else class="overflow-x-auto">
        <table class="tabla-tp w-full">
          <thead>
            <tr>
              <th>Placa</th><th>VIN</th><th>Vehículo</th><th>Color</th><th>Cliente</th><th>Estatus</th><th>Registrado</th>
              <th v-if="puedeEditar">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in vehiculos" :key="v.id">
              <td class="font-semibold text-ink">{{ formatearPlaca(v.placa) }}</td>
              <td class="font-mono text-xs text-ink/70" :title="v.vin">…{{ v.vin.slice(-8) }}</td>
              <td class="text-ink/80">{{ v.marca }} {{ v.modelo }} {{ v.anio }} · {{ v.version }}</td>
              <td class="text-ink/70">{{ v.color }}</td>
              <td>
                <RouterLink :to="{ name: 'clientes-detalle', params: { id: v.clienteId } }" class="font-medium text-primary hover:underline">
                  {{ v.clienteNombre }}
                </RouterLink>
              </td>
              <td><span :class="claseChip(v.estatus)">{{ v.estatus }}</span></td>
              <td class="text-secondary">{{ formatearFecha(v.creadoEn) }}</td>
              <td v-if="puedeEditar">
                <div class="flex items-center gap-3">
                  <button type="button" class="boton-tp-texto text-primary" @click="abrirEditar(v)">Editar</button>
                  <button type="button" class="boton-tp-texto text-primary" @click="abrirCambiarCliente(v)">Cambiar cliente</button>
                  <button type="button" class="boton-tp-texto" :class="v.estatus === 'ACTIVO' ? 'text-danger' : 'text-success'" @click="abrirCambiarEstatus(v)">
                    {{ v.estatus === 'ACTIVO' ? 'Desactivar' : 'Activar' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="vehiculos.length" class="flex flex-wrap items-center justify-between gap-3 border-t border-mist px-4 py-3 text-sm text-secondary">
        <span>Mostrando {{ paginaActual * 10 + 1 }}–{{ Math.min((paginaActual + 1) * 10, totalElementos) }} de {{ totalElementos }} vehículo{{ totalElementos === 1 ? '' : 's' }} · Página {{ paginaActual + 1 }} de {{ Math.max(totalPaginas, 1) }}</span>
        <div class="flex items-center gap-3">
          <button type="button" class="boton-tp-secundario !min-h-[36px]" :disabled="paginaActual === 0" @click="paginaActual--">Anterior</button>
          <span class="font-semibold text-ink">{{ paginaActual + 1 }}</span>
          <button type="button" class="boton-tp-secundario !min-h-[36px]" :disabled="paginaActual >= totalPaginas - 1" @click="paginaActual++">Siguiente</button>
        </div>
      </div>
    </div>
  </div>

  <ModalEditarVehiculo v-if="modalEditarAbierto" :vehiculo="vehiculoEditando" @cerrar="modalEditarAbierto = false" @guardado="alGuardarEdicion" />
  <ModalCambiarClienteVehiculo v-if="modalCambiarClienteAbierto" :vehiculo="vehiculoReasignando" @cerrar="modalCambiarClienteAbierto = false" @guardado="alCambiarCliente" />

  <ModalBase v-if="modalEstatusAbierto" :titulo="accionEstatus === 'desactivar' ? 'Desactivar vehículo' : 'Activar vehículo'"
             ancho="confirmacion" @cerrar="modalEstatusAbierto = false">
    <p class="text-sm text-ink/80">
      ¿Seguro que quieres <strong>{{ accionEstatus === 'desactivar' ? 'desactivar' : 'activar' }}</strong> el vehículo
      <strong>{{ formatearPlaca(vehiculoCambiandoEstatus?.placa) }}</strong>
      ({{ vehiculoCambiandoEstatus?.marca }} {{ vehiculoCambiandoEstatus?.modelo }})?
      Su historial se conserva.
    </p>
    <template #footer>
      <button type="button" class="boton-tp-secundario" @click="modalEstatusAbierto = false">Cancelar</button>
      <button type="button" :class="accionEstatus === 'desactivar' ? 'boton-tp-peligro' : 'boton-tp-exito'"
              :disabled="guardandoEstatus" @click="confirmarCambiarEstatus">
        {{ guardandoEstatus ? 'Guardando...' : (accionEstatus === 'desactivar' ? 'Desactivar' : 'Activar') }}
      </button>
    </template>
  </ModalBase>
</template>
