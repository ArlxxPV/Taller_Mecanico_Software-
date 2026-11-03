<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { clienteFacade, validarMotivo } from '../facades/clienteFacade'
import { vehiculoFacade } from '../facades/vehiculoFacade'
import { useAuthStore } from '../stores/auth'
import ModalBase from '../components/ModalBase.vue'
import ModalAsociarVehiculo from '../components/ModalAsociarVehiculo.vue'
import ModalFotoAmpliada from '../components/ModalFotoAmpliada.vue'
import { formatearPlaca } from '../utils/vehiculos'

const route = useRoute()
const toast = useToast()
const auth = useAuthStore()

const puedeSuspender = computed(() => auth.permisosUsuario.includes('CLIENTES_SUSPENDER'))
const puedeAsociarVehiculo = computed(() =>
  auth.permisosUsuario.includes('CLIENTES_CREAR') || auth.permisosUsuario.includes('CLIENTES_EDITAR'))

const cliente = ref(null)
const fotoUrl = ref('')
const cargando = ref(true)
const noEncontrado = ref(false)
const pestana = ref('datos')

const historial = ref([])
const cargandoHistorial = ref(false)
let historialCargado = false

const vehiculos = ref([])
const cargandoVehiculos = ref(false)
let vehiculosCargados = false
const modalVehiculoAbierto = ref(false)
const modalFotoAbierto = ref(false)

async function cargarVehiculosSiFalta() {
  if (vehiculosCargados || !cliente.value) return
  cargandoVehiculos.value = true
  const resultado = await vehiculoFacade.listarPorCliente(cliente.value.id)
  cargandoVehiculos.value = false
  if (resultado.ok) {
    vehiculos.value = resultado.vehiculos
    vehiculosCargados = true
  }
}
watch(pestana, (p) => { if (p === 'vehiculos') cargarVehiculosSiFalta() })

function alGuardarVehiculo() {
  modalVehiculoAbierto.value = false
  vehiculosCargados = false
  cargarVehiculosSiFalta()
}

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

function formatearFecha(fechaIso, conHora = false) {
  if (!fechaIso) return '—'
  const fecha = new Date(fechaIso)
  return conHora
    ? fecha.toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' })
    : fecha.toLocaleDateString('es-MX', { dateStyle: 'long' })
}

async function cargarHistorialSiFalta() {
  if (historialCargado || !cliente.value) return
  cargandoHistorial.value = true
  const resultado = await clienteFacade.historial(cliente.value.id)
  cargandoHistorial.value = false
  if (resultado.ok) {
    historial.value = resultado.movimientos
    historialCargado = true
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar el historial', detail: resultado.mensaje, life: 4500 })
  }
}
watch(pestana, (p) => { if (p === 'historial') cargarHistorialSiFalta() })

function cambiosDe(fila) {
  if (!fila.cambios) return []
  try {
    return Object.entries(JSON.parse(fila.cambios)).map(([campo, valores]) => ({ campo, anterior: valores[0], nuevo: valores[1] }))
  } catch {
    return []
  }
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
    historial.value = []
    historialCargado = false
    vehiculos.value = []
    vehiculosCargados = false
    pestana.value = 'datos'

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

// === Modal: Suspender / Reactivar (mismo patron que ClienteListaView) ===
const modalSuspenderAbierto = ref(false)
const motivoSuspender = ref('')
const guardandoSuspender = ref(false)
function abrirSuspender() { motivoSuspender.value = ''; modalSuspenderAbierto.value = true }
async function confirmarSuspender() {
  if (validarMotivo(motivoSuspender.value)) return
  guardandoSuspender.value = true
  const resultado = await clienteFacade.suspender(cliente.value.id, motivoSuspender.value.trim())
  guardandoSuspender.value = false
  modalSuspenderAbierto.value = false
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente suspendido', detail: resultado.mensaje, life: 3000 })
    cliente.value = resultado.cliente
    historialCargado = false
    if (pestana.value === 'historial') cargarHistorialSiFalta()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo suspender', detail: resultado.mensaje, life: 4500 })
  }
}

const modalReactivarAbierto = ref(false)
const comentarioReactivar = ref('')
const guardandoReactivar = ref(false)
function abrirReactivar() { comentarioReactivar.value = ''; modalReactivarAbierto.value = true }
async function confirmarReactivar() {
  guardandoReactivar.value = true
  const resultado = await clienteFacade.reactivar(cliente.value.id, comentarioReactivar.value.trim())
  guardandoReactivar.value = false
  modalReactivarAbierto.value = false
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente reactivado', detail: resultado.mensaje, life: 3000 })
    cliente.value = resultado.cliente
    historialCargado = false
    if (pestana.value === 'historial') cargarHistorialSiFalta()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo reactivar', detail: resultado.mensaje, life: 4500 })
  }
}
</script>

<template>
  <div class="mx-auto max-w-5xl">
    <div v-if="cargando" class="mt-10 text-center text-sm text-secondary">Cargando...</div>
    <div v-else-if="noEncontrado" class="mt-10 text-center text-sm text-secondary">No se encontró este cliente.</div>

    <template v-else>
      <div class="tarjeta-tp flex flex-wrap items-center justify-between gap-4">
        <div class="flex items-center gap-4">
          <button v-if="fotoUrl" type="button" class="shrink-0" title="Ver foto en grande" @click="modalFotoAbierto = true">
            <img :src="fotoUrl" :alt="`Foto de ${cliente.nombreCompleto}`" class="h-16 w-16 rounded-full border border-mist object-cover transition hover:opacity-80" />
          </button>
          <div v-else class="flex h-16 w-16 items-center justify-center rounded-full border border-dashed border-mist text-ink/30">
            <i class="pi pi-user text-2xl"></i>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h1 class="font-display text-2xl font-bold text-ink">{{ cliente.nombreCompleto }}</h1>
              <span :class="claseChip(cliente.estadoCliente)">{{ cliente.estadoCliente }}</span>
            </div>
            <p class="text-sm text-secondary">
              {{ cliente.tallerNombre || 'Sin taller' }} · Registrado por {{ etiquetaOrigen(cliente.origenRegistro) }} el {{ formatearFecha(cliente.creadoEn) }}
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <RouterLink v-if="cliente.estadoCliente !== 'ELIMINADO'" :to="{ name: 'clientes-editar', params: { id: cliente.id } }" class="boton-tp-secundario">
            Editar
          </RouterLink>
          <button v-if="puedeSuspender && cliente.estadoCliente === 'ACTIVO'" type="button" class="boton-tp-peligro" @click="abrirSuspender">Suspender</button>
          <button v-if="puedeSuspender && cliente.estadoCliente === 'SUSPENDIDO'" type="button" class="boton-tp-exito" @click="abrirReactivar">Reactivar</button>
          <button v-if="puedeAsociarVehiculo && cliente.estadoCliente === 'ACTIVO'" type="button" class="boton-tp-primario"
                  @click="modalVehiculoAbierto = true">
            Asociar vehículo
          </button>
          <button v-else type="button" class="boton-tp-primario" disabled title="El cliente debe estar activo para asociar un vehiculo">
            Asociar vehículo
          </button>
        </div>
      </div>

      <div class="mt-6 flex gap-6 border-b border-mist">
        <button type="button" class="border-b-[3px] px-1 pb-3 text-sm"
                :class="pestana === 'datos' ? 'border-primary font-bold text-primary' : 'border-transparent text-secondary'"
                @click="pestana = 'datos'">Datos</button>
        <button type="button" class="border-b-[3px] px-1 pb-3 text-sm"
                :class="pestana === 'vehiculos' ? 'border-primary font-bold text-primary' : 'border-transparent text-secondary'"
                @click="pestana = 'vehiculos'">Vehículos ({{ vehiculos.length }})</button>
        <button type="button" class="border-b-[3px] px-1 pb-3 text-sm"
                :class="pestana === 'historial' ? 'border-primary font-bold text-primary' : 'border-transparent text-secondary'"
                @click="pestana = 'historial'">Historial</button>
      </div>

      <!-- Datos -->
      <div v-if="pestana === 'datos'" class="mt-6 grid gap-5 sm:grid-cols-2">
        <section class="tarjeta-tp">
          <h2 class="font-display text-base font-semibold text-ink">Datos personales</h2>
          <dl class="mt-3 space-y-2 text-sm">
            <div class="flex justify-between"><dt class="text-secondary">Nombre(s)</dt><dd class="text-ink">{{ cliente.nombres || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Apellido paterno</dt><dd class="text-ink">{{ cliente.apellidoPaterno || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Apellido materno</dt><dd class="text-ink">{{ cliente.apellidoMaterno || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">CURP</dt><dd class="text-ink">{{ cliente.curp || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Contacto alternativo</dt><dd class="text-ink">{{ cliente.contactoAlternativo || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Fecha de nacimiento</dt><dd class="text-ink">{{ formatearFecha(cliente.fechaNacimiento) }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Edad</dt><dd class="text-ink">{{ cliente.edad ?? '—' }}</dd></div>
          </dl>
        </section>

        <section class="tarjeta-tp">
          <h2 class="font-display text-base font-semibold text-ink">Contacto</h2>
          <dl class="mt-3 space-y-2 text-sm">
            <div class="flex justify-between"><dt class="text-secondary">Teléfono personal</dt><dd class="text-ink">{{ cliente.telefonoPersonal || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Teléfono de trabajo</dt><dd class="text-ink">{{ cliente.telefonoTrabajo || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Correo</dt><dd class="break-all text-ink">{{ cliente.email || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Correo de trabajo</dt><dd class="break-all text-ink">{{ cliente.emailTrabajo || '—' }}</dd></div>
          </dl>
        </section>

        <section class="tarjeta-tp sm:col-span-2">
          <h2 class="font-display text-base font-semibold text-ink">Dirección</h2>
          <dl class="mt-3 grid gap-2 text-sm sm:grid-cols-2">
            <div class="flex justify-between"><dt class="text-secondary">Calle</dt><dd class="text-ink">{{ cliente.calle || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Colonia</dt><dd class="text-ink">{{ cliente.colonia || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Municipio</dt><dd class="text-ink">{{ cliente.municipio || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Estado</dt><dd class="text-ink">{{ cliente.estado || '—' }}</dd></div>
            <div class="flex justify-between"><dt class="text-secondary">Código postal</dt><dd class="text-ink">{{ cliente.codigoPostal || '—' }}</dd></div>
          </dl>
        </section>

        <p class="text-xs text-secondary sm:col-span-2">
          Alta: {{ formatearFecha(cliente.creadoEn) }}
          <template v-if="cliente.actualizadoEn"> · Última edición: {{ formatearFecha(cliente.actualizadoEn) }} por {{ cliente.actualizadoPorNombre || '—' }}</template>
        </p>
      </div>

      <!-- Vehiculos -->
      <div v-else-if="pestana === 'vehiculos'" class="mt-6">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="font-display text-base font-semibold text-ink">Vehículos asociados</h2>
          <button v-if="puedeAsociarVehiculo && cliente.estadoCliente === 'ACTIVO'" type="button" class="boton-tp-primario"
                  @click="modalVehiculoAbierto = true">
            + Asociar vehículo
          </button>
        </div>

        <div class="overflow-hidden rounded-[14px] border border-mist bg-white">
          <div v-if="cargandoVehiculos" class="p-8 text-center text-sm text-secondary">Cargando...</div>
          <div v-else-if="!vehiculos.length" class="p-8 text-center text-sm text-secondary">Sin vehículos asociados.</div>
          <table v-else class="tabla-tp w-full">
            <thead>
              <tr>
                <th>Placa</th><th>Vehículo</th><th>Versión</th><th>Color</th><th>Kilometraje</th><th>Estatus</th><th>Relación</th><th>Registrado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in vehiculos" :key="r.vehiculo.id" :class="!r.vigente && 'opacity-60'">
                <td class="font-semibold text-ink">{{ formatearPlaca(r.vehiculo.placa) }}</td>
                <td class="text-ink/80">{{ r.vehiculo.marca }} {{ r.vehiculo.modelo }} {{ r.vehiculo.anio }}</td>
                <td class="text-ink/70">{{ r.vehiculo.version }}</td>
                <td class="text-ink/70">{{ r.vehiculo.color }}</td>
                <td class="text-ink/70">{{ r.vehiculo.kilometraje ?? '—' }}</td>
                <td><span :class="claseChip(r.vehiculo.estatus)">{{ r.vehiculo.estatus }}</span></td>
                <td>
                  <span v-if="r.vigente" class="pill-tp">Actual</span>
                  <span v-else class="pill-tp" :title="r.motivo || ''">Anterior</span>
                </td>
                <td class="text-secondary">{{ formatearFecha(r.fechaInicio) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Historial -->
      <div v-else class="tarjeta-tp mt-6">
        <h2 class="mb-4 font-display text-base font-semibold text-ink">Historial de estatus</h2>
        <div v-if="cargandoHistorial" class="py-6 text-center text-sm text-secondary">Cargando...</div>
        <div v-else-if="!historial.length" class="py-6 text-center text-sm text-secondary">Sin movimientos.</div>
        <div v-else class="divide-y divide-divider">
          <div v-for="fila in historial" :key="fila.id" class="py-3">
            <div class="flex flex-wrap items-center gap-3">
              <span class="text-xs text-secondary">{{ formatearFecha(fila.creadoEn, true) }}</span>
              <span :class="claseChip(fila.estatusNuevo)">{{ fila.estatusNuevo }}</span>
              <span class="text-sm text-ink/80">{{ fila.motivo }}</span>
              <span class="ml-auto text-xs text-secondary">{{ fila.usuarioNombre || etiquetaOrigen(cliente.origenRegistro) }}</span>
            </div>
            <ul v-if="cambiosDe(fila).length" class="mt-2 space-y-1 pl-1 text-xs text-ink/70">
              <li v-for="c in cambiosDe(fila)" :key="c.campo">
                <span class="font-medium">{{ c.campo }}:</span> {{ c.anterior }} → {{ c.nuevo }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </template>

    <!-- Modal: Suspender -->
    <ModalBase v-if="modalSuspenderAbierto" titulo="Suspender cliente" ancho="confirmacion" @cerrar="modalSuspenderAbierto = false">
      <p class="text-sm text-ink/80">
        <strong>{{ cliente.nombreCompleto }}</strong> no podrá asociar vehículos ni abrir órdenes hasta que se reactive. Sus datos no se borran.
      </p>
      <div class="mt-4">
        <label class="etiqueta-tp" for="motivoSuspenderDet">Motivo de la suspensión *</label>
        <textarea id="motivoSuspenderDet" v-model="motivoSuspender" rows="3" maxlength="255" class="campo-tp !h-auto py-2.5"></textarea>
        <p class="ayuda-tp">{{ motivoSuspender.length }} / 255 · mínimo 10</p>
      </div>
      <template #footer>
        <button type="button" class="boton-tp-secundario" @click="modalSuspenderAbierto = false">Cancelar</button>
        <button type="button" class="boton-tp-peligro" :disabled="!!validarMotivo(motivoSuspender) || guardandoSuspender" @click="confirmarSuspender">
          {{ guardandoSuspender ? 'Suspendiendo...' : 'Suspender' }}
        </button>
      </template>
    </ModalBase>

    <!-- Modal: Reactivar -->
    <ModalBase v-if="modalReactivarAbierto" titulo="Reactivar cliente" ancho="confirmacion" @cerrar="modalReactivarAbierto = false">
      <p class="text-sm text-ink/80"><strong>{{ cliente.nombreCompleto }}</strong> volverá a estar ACTIVO y podrá asociar vehículos.</p>
      <div class="mt-4">
        <label class="etiqueta-tp" for="comentarioReactivarDet">Comentario (opcional)</label>
        <textarea id="comentarioReactivarDet" v-model="comentarioReactivar" rows="3" maxlength="255" class="campo-tp !h-auto py-2.5"></textarea>
        <p class="ayuda-tp">{{ comentarioReactivar.length }} / 255</p>
      </div>
      <template #footer>
        <button type="button" class="boton-tp-secundario" @click="modalReactivarAbierto = false">Cancelar</button>
        <button type="button" class="boton-tp-exito" :disabled="guardandoReactivar" @click="confirmarReactivar">
          {{ guardandoReactivar ? 'Reactivando...' : 'Reactivar' }}
        </button>
      </template>
    </ModalBase>

    <!-- Modal: Asociar vehículo -->
    <ModalAsociarVehiculo v-if="modalVehiculoAbierto" :cliente="cliente" @cerrar="modalVehiculoAbierto = false" @guardado="alGuardarVehiculo" />

    <ModalFotoAmpliada v-if="modalFotoAbierto" :src="fotoUrl" :titulo="cliente.nombreCompleto" @cerrar="modalFotoAbierto = false" />
  </div>
</template>
