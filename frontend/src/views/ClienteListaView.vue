<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { clienteFacade, validarMotivo } from '../facades/clienteFacade'
import { tallerFacade } from '../facades/tallerFacade'
import { estatusFacade } from '../facades/estatusFacade'
import { useAuthStore } from '../stores/auth'
import ModalBase from '../components/ModalBase.vue'
import ModalAsociarVehiculo from '../components/ModalAsociarVehiculo.vue'
import ClienteFormularioCampos from '../components/ClienteFormularioCampos.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const auth = useAuthStore()

const esAdminTodosTalleres = computed(() => auth.permisosUsuario.includes('CLIENTES_TODOS_TALLERES'))
const puedeEditar = computed(() => auth.permisosUsuario.includes('CLIENTES_EDITAR'))
const puedeSuspender = computed(() => auth.permisosUsuario.includes('CLIENTES_SUSPENDER'))
const puedeEliminar = computed(() => auth.permisosUsuario.includes('CLIENTES_ELIMINAR'))

const clientes = ref([])
const talleres = ref([])
const estatusCatalogo = ref([])
const cargando = ref(true)
const resumen = ref({ total: 0, activos: 0, suspendidos: 0, eliminados: 0 })

const idEstatus = computed(() => {
  const mapa = {}
  for (const e of estatusCatalogo.value) mapa[e.valor] = e.id
  return mapa
})

// Filtros + pagina, sincronizados con la URL (?q=&tallerId=&estatusId=&...)
// para que se puedan compartir/recargar. "busqueda" es lo que escribe el
// usuario; "busquedaAplicada" es la version con debounce de 400ms que de
// verdad dispara la peticion.
const busqueda = ref(route.query.q || '')
const busquedaAplicada = ref(route.query.q || '')
const filtroTaller = ref(route.query.tallerId || '')
const filtroEstatus = ref(route.query.estatusId || '')
const incluirEliminados = ref(route.query.incluirEliminados === 'true')
const ordenarPor = ref(route.query.ordenarPor || 'creadoEn')
const direccion = ref(route.query.direccion || 'desc')
const paginaActual = ref(Number(route.query.pagina) || 0)
const porPagina = ref(Number(route.query.porPagina) || 10)
const totalPaginas = ref(0)
const totalElementos = ref(0)

let temporizadorBusqueda = null
watch(busqueda, (valor) => {
  clearTimeout(temporizadorBusqueda)
  temporizadorBusqueda = setTimeout(() => { busquedaAplicada.value = valor }, 400)
})

const hayClientes = computed(() => clientes.value.length > 0)
const ETIQUETAS_ORIGEN = { ADMIN: 'Administrador', RECEPCIONISTA: 'Recepción', AUTORREGISTRO: 'Autorregistro' }
function etiquetaOrigen(origen) {
  return ETIQUETAS_ORIGEN[origen] || origen || '—'
}

function formatearFecha(fechaIso) {
  if (!fechaIso) return '—'
  return new Date(fechaIso).toLocaleDateString('es-MX', { dateStyle: 'medium' })
}

function claseChip(valor) {
  if (valor === 'ACTIVO') return 'chip-estatus-activo'
  if (valor === 'SUSPENDIDO') return 'chip-estatus-suspendido'
  return 'chip-estatus-neutro'
}

async function cargar() {
  cargando.value = true
  const resultado = await clienteFacade.listar({
    tallerId: esAdminTodosTalleres.value && filtroTaller.value ? filtroTaller.value : undefined,
    estatusId: filtroEstatus.value || undefined,
    q: busquedaAplicada.value || undefined,
    ordenarPor: ordenarPor.value,
    direccion: direccion.value,
    pagina: paginaActual.value,
    porPagina: porPagina.value,
    incluirEliminados: incluirEliminados.value || undefined
  })
  cargando.value = false

  if (resultado.ok) {
    clientes.value = resultado.pagina.clientes
    totalPaginas.value = resultado.pagina.totalPaginas
    totalElementos.value = resultado.pagina.totalElementos
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar la lista', detail: resultado.mensaje, life: 4500 })
  }
}

async function cargarResumen() {
  const resultado = await clienteFacade.resumen(esAdminTodosTalleres.value ? filtroTaller.value : undefined)
  if (resultado.ok) resumen.value = resultado.resumen
}

function sincronizarUrl() {
  router.replace({
    query: {
      q: busquedaAplicada.value || undefined,
      tallerId: filtroTaller.value || undefined,
      estatusId: filtroEstatus.value || undefined,
      incluirEliminados: incluirEliminados.value ? 'true' : undefined,
      ordenarPor: ordenarPor.value,
      direccion: direccion.value,
      pagina: paginaActual.value || undefined,
      porPagina: porPagina.value !== 10 ? porPagina.value : undefined
    }
  })
}

// Cualquier cambio de filtro/orden/tamanio regresa a la pagina 0 (una pagina
// vieja podria ya no existir con el nuevo filtro).
watch([filtroTaller, filtroEstatus, busquedaAplicada, ordenarPor, direccion, incluirEliminados, porPagina], () => {
  paginaActual.value = 0
})
watch([filtroTaller, filtroEstatus, busquedaAplicada, ordenarPor, direccion, paginaActual, porPagina, incluirEliminados], () => {
  sincronizarUrl()
  cargar()
})
watch(filtroTaller, cargarResumen)

function ordenarPorColumna(columna) {
  if (ordenarPor.value === columna) {
    direccion.value = direccion.value === 'asc' ? 'desc' : 'asc'
  } else {
    ordenarPor.value = columna
    direccion.value = 'asc'
  }
}

function limpiarFiltros() {
  busqueda.value = ''
  busquedaAplicada.value = ''
  filtroTaller.value = ''
  filtroEstatus.value = ''
  incluirEliminados.value = false
  ordenarPor.value = 'creadoEn'
  direccion.value = 'desc'
  porPagina.value = 10
}

function verTarjetaTotal() {
  filtroEstatus.value = ''
  incluirEliminados.value = false
}
function verTarjetaActivos() {
  filtroEstatus.value = String(idEstatus.value.ACTIVO || '')
  incluirEliminados.value = false
}
function verTarjetaSuspendidos() {
  filtroEstatus.value = String(idEstatus.value.SUSPENDIDO || '')
  incluirEliminados.value = false
}
function verTarjetaEliminados() {
  filtroEstatus.value = String(idEstatus.value.ELIMINADO || '')
  incluirEliminados.value = true
}

// === Menu de acciones (kebab) ===
const menuAbiertoId = ref(null)
function alternarMenu(id) {
  menuAbiertoId.value = menuAbiertoId.value === id ? null : id
}
function cerrarMenu() {
  menuAbiertoId.value = null
}

function irADetalle(id) {
  router.push({ name: 'clientes-detalle', params: { id } })
}

// === Modal: Editar cliente ===
const modalEditarAbierto = ref(false)
const clienteEditando = ref(null)
const formEditar = reactive({
  nombres: '', apellidoPaterno: '', apellidoMaterno: '', curp: '', contactoAlternativo: '', fechaNacimiento: '',
  telefonoPersonal: '', telefonoTrabajo: '', email: '', emailTrabajo: '',
  calle: '', colonia: '', municipio: '', estado: '', codigoPostal: '', tallerId: ''
})
let snapshotEditar = ''
const fotoArchivoEditar = ref(null)
const fotoActualUrlEditar = ref('')
const guardandoEditar = ref(false)
const mensajeErrorEditar = ref('')
const erroresCamposEditar = ref([])

const hayCambiosEditar = computed(() => JSON.stringify(formEditar) !== snapshotEditar || !!fotoArchivoEditar.value)

async function abrirEditar(cliente) {
  cerrarMenu()
  mensajeErrorEditar.value = ''
  erroresCamposEditar.value = []
  fotoArchivoEditar.value = null
  fotoActualUrlEditar.value = ''
  modalEditarAbierto.value = true
  clienteEditando.value = cliente

  const resultado = await clienteFacade.obtener(cliente.id)
  if (!resultado.ok) {
    toast.add({ severity: 'error', summary: 'No se pudo abrir el cliente', detail: resultado.mensaje, life: 4500 })
    modalEditarAbierto.value = false
    return
  }
  Object.assign(formEditar, resultado.formulario)
  snapshotEditar = JSON.stringify(formEditar)
  if (resultado.cliente.tieneFoto) {
    fotoActualUrlEditar.value = (await clienteFacade.obtenerFotoUrl(cliente.id)) || ''
  }
}

function cerrarEditar() {
  modalEditarAbierto.value = false
  clienteEditando.value = null
}

async function guardarEditar() {
  mensajeErrorEditar.value = ''
  erroresCamposEditar.value = []
  guardandoEditar.value = true
  const resultado = await clienteFacade.actualizar(clienteEditando.value.id, formEditar, fotoArchivoEditar.value)
  guardandoEditar.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente actualizado', detail: 'Al guardar se registro ACTUALIZADO en el historial.', life: 3500 })
    cerrarEditar()
    cargar()
  } else {
    mensajeErrorEditar.value = resultado.mensaje
    erroresCamposEditar.value = resultado.detalles
  }
}

// === Modal: Suspender ===
const modalSuspenderAbierto = ref(false)
const clienteSuspendiendo = ref(null)
const motivoSuspender = ref('')
const guardandoSuspender = ref(false)
const errorMotivoSuspender = computed(() => modalSuspenderAbierto.value ? validarMotivo(motivoSuspender.value) : '')

function abrirSuspender(cliente) {
  cerrarMenu()
  clienteSuspendiendo.value = cliente
  motivoSuspender.value = ''
  modalSuspenderAbierto.value = true
}
async function confirmarSuspender() {
  if (validarMotivo(motivoSuspender.value)) return
  guardandoSuspender.value = true
  const resultado = await clienteFacade.suspender(clienteSuspendiendo.value.id, motivoSuspender.value.trim())
  guardandoSuspender.value = false
  modalSuspenderAbierto.value = false
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente suspendido', detail: resultado.mensaje, life: 3000 })
    cargar(); cargarResumen()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo suspender', detail: resultado.mensaje, life: 4500 })
  }
}

// === Modal: Reactivar ===
const modalReactivarAbierto = ref(false)
const clienteReactivando = ref(null)
const comentarioReactivar = ref('')
const guardandoReactivar = ref(false)

function abrirReactivar(cliente) {
  cerrarMenu()
  clienteReactivando.value = cliente
  comentarioReactivar.value = ''
  modalReactivarAbierto.value = true
}
async function confirmarReactivar() {
  guardandoReactivar.value = true
  const resultado = await clienteFacade.reactivar(clienteReactivando.value.id, comentarioReactivar.value.trim())
  guardandoReactivar.value = false
  modalReactivarAbierto.value = false
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente reactivado', detail: resultado.mensaje, life: 3000 })
    cargar(); cargarResumen()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo reactivar', detail: resultado.mensaje, life: 4500 })
  }
}

// === Modal: Eliminar (solo admin) ===
const modalEliminarAbierto = ref(false)
const clienteEliminando = ref(null)
const motivoEliminar = ref('')
const guardandoEliminar = ref(false)

function abrirEliminar(cliente) {
  cerrarMenu()
  clienteEliminando.value = cliente
  motivoEliminar.value = ''
  modalEliminarAbierto.value = true
}
async function confirmarEliminar() {
  if (validarMotivo(motivoEliminar.value)) return
  guardandoEliminar.value = true
  const resultado = await clienteFacade.eliminar(clienteEliminando.value.id, motivoEliminar.value.trim())
  guardandoEliminar.value = false
  modalEliminarAbierto.value = false
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente eliminado', detail: resultado.mensaje, life: 3000 })
    cargar(); cargarResumen()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo eliminar', detail: resultado.mensaje, life: 4500 })
  }
}

// === Modal: Asociar vehículo ===
const modalVehiculoAbierto = ref(false)
const clienteParaVehiculo = ref(null)
function abrirAsociarVehiculo(cliente) {
  cerrarMenu()
  clienteParaVehiculo.value = cliente
  modalVehiculoAbierto.value = true
}
function alGuardarVehiculo() {
  modalVehiculoAbierto.value = false
}

async function restaurar(cliente) {
  cerrarMenu()
  const resultado = await clienteFacade.restaurar(cliente.id)
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente restaurado', detail: resultado.mensaje, life: 3000 })
    cargar(); cargarResumen()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo restaurar', detail: resultado.mensaje, life: 4500 })
  }
}

onMounted(async () => {
  if (esAdminTodosTalleres.value) {
    const resultadoTalleres = await tallerFacade.listar()
    if (resultadoTalleres.ok) talleres.value = resultadoTalleres.talleres
  }
  const resultadoEstatus = await estatusFacade.listar()
  if (resultadoEstatus.ok) estatusCatalogo.value = resultadoEstatus.estatus

  cargar()
  cargarResumen()
})
</script>

<template>
  <div class="mx-auto max-w-6xl" @click="cerrarMenu">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 class="titulo-pagina">Administración de clientes</h1>
        <p class="subtitulo-pagina">Consulta, edita y gestiona el estatus de los clientes del taller.</p>
      </div>
      <RouterLink :to="{ name: 'clientes-nuevo' }" class="boton-tp-primario">+ Registrar cliente</RouterLink>
    </div>

    <!-- Tarjetas de resumen, clicables -->
    <div class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <button type="button" class="tarjeta-tp text-left transition hover:border-primary/40" @click="verTarjetaTotal">
        <p class="text-sm text-secondary">Clientes totales</p>
        <p class="mt-1 font-display text-[28px] font-bold text-ink">{{ resumen.total }}</p>
      </button>
      <button type="button" class="tarjeta-tp text-left transition hover:border-primary/40" @click="verTarjetaActivos">
        <p class="text-sm text-secondary">Activos</p>
        <p class="mt-1 font-display text-[28px] font-bold text-success">{{ resumen.activos }}</p>
      </button>
      <button type="button" class="tarjeta-tp text-left transition hover:border-primary/40" @click="verTarjetaSuspendidos">
        <p class="text-sm text-secondary">Suspendidos</p>
        <p class="mt-1 font-display text-[28px] font-bold text-danger">{{ resumen.suspendidos }}</p>
      </button>
      <button v-if="puedeEliminar" type="button" class="tarjeta-tp text-left transition hover:border-primary/40" @click="verTarjetaEliminados">
        <p class="text-sm text-secondary">Eliminados <span class="pill-tp">solo admin</span></p>
        <p class="mt-1 font-display text-[28px] font-bold text-[#475569]">{{ resumen.eliminados }}</p>
      </button>
    </div>

    <!-- Filtros -->
    <div class="tarjeta-tp mt-6 flex flex-wrap items-end gap-4">
      <div class="min-w-[220px] flex-1">
        <label class="etiqueta-tp" for="buscar">Buscar</label>
        <input id="buscar" v-model="busqueda" type="text" class="campo-tp" placeholder="Nombre, apellido o teléfono" />
      </div>
      <div v-if="esAdminTodosTalleres" class="w-full sm:w-52">
        <label class="etiqueta-tp" for="filtroTaller">Taller</label>
        <select id="filtroTaller" v-model="filtroTaller" class="campo-tp">
          <option value="">Todos los talleres</option>
          <option v-for="t in talleres" :key="t.id" :value="t.id">{{ t.nombre }}</option>
        </select>
      </div>
      <div class="w-full sm:w-44">
        <label class="etiqueta-tp" for="filtroEstatus">Estatus</label>
        <select id="filtroEstatus" v-model="filtroEstatus" class="campo-tp" :disabled="incluirEliminados">
          <option value="">Todos</option>
          <option :value="String(idEstatus.ACTIVO || '')">Activo</option>
          <option :value="String(idEstatus.SUSPENDIDO || '')">Suspendido</option>
        </select>
      </div>
      <div class="w-full sm:w-48">
        <label class="etiqueta-tp" for="ordenarPor">Ordenar por</label>
        <select id="ordenarPor" v-model="ordenarPor" class="campo-tp">
          <option value="creadoEn">Fecha de registro</option>
          <option value="nombre">Nombre</option>
        </select>
      </div>
      <button type="button" class="boton-tp-secundario"
              @click="direccion = direccion === 'asc' ? 'desc' : 'asc'">
        {{ direccion === 'asc' ? 'Ascendente ↑' : 'Descendente ↓' }}
      </button>
      <button type="button" class="boton-tp-texto text-primary" @click="limpiarFiltros">Limpiar filtros</button>
    </div>

    <div class="mt-6 overflow-hidden rounded-[14px] border border-mist bg-white">
      <div v-if="cargando" class="p-10 text-center text-sm text-secondary">Cargando clientes...</div>
      <div v-else-if="!hayClientes" class="p-10 text-center text-sm text-secondary">No hay clientes con estos filtros.</div>

      <div v-else class="overflow-x-auto">
        <table class="tabla-tp w-full">
          <thead>
            <tr>
              <th class="cursor-pointer select-none" @click="ordenarPorColumna('nombre')">
                Nombre <span v-if="ordenarPor === 'nombre'">{{ direccion === 'asc' ? '↑' : '↓' }}</span>
              </th>
              <th>Teléfono</th>
              <th>Taller</th>
              <th>Registrado por</th>
              <th>Estatus</th>
              <th class="cursor-pointer select-none" @click="ordenarPorColumna('creadoEn')">
                Registrado <span v-if="ordenarPor === 'creadoEn'">{{ direccion === 'asc' ? '↑' : '↓' }}</span>
              </th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cliente in clientes" :key="cliente.id">
              <td>
                <button type="button" class="flex items-center gap-2.5 text-left hover:underline" @click="irADetalle(cliente.id)">
                  <span class="avatar-tp">{{ (cliente.nombreCompleto || '?').charAt(0).toUpperCase() }}</span>
                  <span class="font-medium text-ink">{{ cliente.nombreCompleto }}</span>
                </button>
              </td>
              <td class="text-ink/70">{{ cliente.telefonoPersonal || '—' }}</td>
              <td class="text-ink/70">{{ cliente.tallerNombre || '—' }}</td>
              <td class="text-ink/70">{{ etiquetaOrigen(cliente.origenRegistro) }}</td>
              <td><span :class="claseChip(cliente.estadoCliente)">{{ cliente.estadoCliente }}</span></td>
              <td class="text-secondary">{{ formatearFecha(cliente.creadoEn) }}</td>
              <td>
                <div class="flex items-center gap-3" @click.stop>
                  <button v-if="puedeEditar && cliente.estadoCliente !== 'ELIMINADO'" type="button" class="boton-tp-texto text-primary" @click="abrirEditar(cliente)">Editar</button>
                  <button v-if="puedeSuspender && cliente.estadoCliente === 'ACTIVO'" type="button" class="boton-tp-texto text-danger" @click="abrirSuspender(cliente)">Suspender</button>
                  <button v-if="puedeSuspender && cliente.estadoCliente === 'SUSPENDIDO'" type="button" class="boton-tp-texto text-success" @click="abrirReactivar(cliente)">Reactivar</button>
                  <button v-if="puedeEditar && cliente.estadoCliente === 'ACTIVO'" type="button" class="boton-tp-texto text-primary"
                          @click="abrirAsociarVehiculo(cliente)">
                    Asociar vehículo
                  </button>
                  <button v-else type="button" class="boton-tp-texto text-secondary" disabled title="El cliente debe estar activo para asociar un vehiculo">
                    Asociar vehículo
                  </button>
                  <div class="relative">
                    <button type="button" class="flex h-8 w-8 items-center justify-center rounded-lg text-ink/50 hover:bg-paper hover:text-ink"
                            aria-label="Mas acciones" @click="alternarMenu(cliente.id)">
                      <i class="pi pi-ellipsis-v"></i>
                    </button>
                    <div v-if="menuAbiertoId === cliente.id"
                         class="absolute right-0 z-10 mt-1 w-44 overflow-hidden rounded-xl border border-mist bg-white py-1 shadow-lg">
                      <button type="button" class="block w-full px-4 py-2 text-left text-sm text-ink hover:bg-paper" @click="irADetalle(cliente.id)">
                        Ver detalle
                      </button>
                      <button v-if="puedeEliminar && cliente.estadoCliente !== 'ELIMINADO'" type="button"
                              class="block w-full px-4 py-2 text-left text-sm text-danger hover:bg-paper" @click="abrirEliminar(cliente)">
                        Eliminar
                      </button>
                      <button v-if="puedeEliminar && cliente.estadoCliente === 'ELIMINADO'" type="button"
                              class="block w-full px-4 py-2 text-left text-sm text-success hover:bg-paper" @click="restaurar(cliente)">
                        Restaurar
                      </button>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="hayClientes" class="flex flex-wrap items-center justify-between gap-3 border-t border-mist px-4 py-3 text-sm text-secondary">
        <span>Mostrando {{ paginaActual * porPagina + 1 }}–{{ Math.min((paginaActual + 1) * porPagina, totalElementos) }} de {{ totalElementos }} clientes · Página {{ paginaActual + 1 }} de {{ Math.max(totalPaginas, 1) }}</span>
        <div class="flex items-center gap-3">
          <label class="flex items-center gap-2">
            Filas por página
            <select v-model.number="porPagina" class="campo-tp !min-h-[36px] !w-20 !py-0">
              <option :value="10">10</option>
              <option :value="25">25</option>
              <option :value="50">50</option>
            </select>
          </label>
          <button type="button" class="boton-tp-secundario !min-h-[36px]" :disabled="paginaActual === 0" @click="paginaActual--">Anterior</button>
          <span class="font-semibold text-ink">{{ paginaActual + 1 }}</span>
          <button type="button" class="boton-tp-secundario !min-h-[36px]" :disabled="paginaActual >= totalPaginas - 1" @click="paginaActual++">Siguiente</button>
        </div>
      </div>
    </div>

    <!-- Modal: Editar cliente -->
    <ModalBase v-if="modalEditarAbierto" titulo="Editar cliente" @cerrar="cerrarEditar">
      <template #subtitulo>
        <span>{{ clienteEditando?.nombreCompleto }}</span>
        <span class="ml-2" :class="claseChip(clienteEditando?.estadoCliente)">{{ clienteEditando?.estadoCliente }}</span>
      </template>

      <div v-if="mensajeErrorEditar" class="caja-error-tp mb-5">
        <p class="font-semibold">{{ mensajeErrorEditar }}</p>
        <ul v-if="erroresCamposEditar.length" class="mt-1 list-disc pl-4">
          <li v-for="(d, i) in erroresCamposEditar" :key="i">{{ d }}</li>
        </ul>
      </div>

      <form class="space-y-6" @submit.prevent="guardarEditar">
        <ClienteFormularioCampos :formulario="formEditar" :foto-actual-url="fotoActualUrlEditar"
                                  :bloquear-taller="!esAdminTodosTalleres" v-model:foto-archivo="fotoArchivoEditar" />
      </form>

      <template #footer-izquierda>
        <p class="text-xs text-secondary">Al guardar se registra ACTUALIZADO en el historial.</p>
      </template>
      <template #footer>
        <button type="button" class="boton-tp-secundario" @click="cerrarEditar">Cancelar</button>
        <button type="button" class="boton-tp-primario" :disabled="!hayCambiosEditar || guardandoEditar" @click="guardarEditar">
          {{ guardandoEditar ? 'Guardando...' : 'Guardar cambios' }}
        </button>
      </template>
    </ModalBase>

    <!-- Modal: Suspender -->
    <ModalBase v-if="modalSuspenderAbierto" titulo="Suspender cliente" ancho="confirmacion" @cerrar="modalSuspenderAbierto = false">
      <p class="text-sm text-ink/80">
        <strong>{{ clienteSuspendiendo?.nombreCompleto }}</strong> no podrá asociar vehículos ni abrir órdenes hasta que se reactive. Sus datos no se borran.
      </p>
      <div class="mt-4">
        <label class="etiqueta-tp" for="motivoSuspender">Motivo de la suspensión *</label>
        <textarea id="motivoSuspender" v-model="motivoSuspender" rows="3" maxlength="255" class="campo-tp !h-auto py-2.5"></textarea>
        <p class="ayuda-tp">{{ motivoSuspender.length }} / 255 · mínimo 10</p>
        <p v-if="motivoSuspender && errorMotivoSuspender" class="error-tp">{{ errorMotivoSuspender }}</p>
      </div>
      <template #footer>
        <button type="button" class="boton-tp-secundario" @click="modalSuspenderAbierto = false">Cancelar</button>
        <button type="button" class="boton-tp-peligro" :disabled="!!errorMotivoSuspender || guardandoSuspender" @click="confirmarSuspender">
          {{ guardandoSuspender ? 'Suspendiendo...' : 'Suspender' }}
        </button>
      </template>
    </ModalBase>

    <!-- Modal: Reactivar -->
    <ModalBase v-if="modalReactivarAbierto" titulo="Reactivar cliente" ancho="confirmacion" @cerrar="modalReactivarAbierto = false">
      <p class="text-sm text-ink/80">
        <strong>{{ clienteReactivando?.nombreCompleto }}</strong> volverá a estar ACTIVO y podrá asociar vehículos.
      </p>
      <div class="mt-4">
        <label class="etiqueta-tp" for="comentarioReactivar">Comentario (opcional)</label>
        <textarea id="comentarioReactivar" v-model="comentarioReactivar" rows="3" maxlength="255" class="campo-tp !h-auto py-2.5"
                  placeholder="Por ejemplo: entregó la documentación"></textarea>
        <p class="ayuda-tp">{{ comentarioReactivar.length }} / 255</p>
      </div>
      <template #footer>
        <button type="button" class="boton-tp-secundario" @click="modalReactivarAbierto = false">Cancelar</button>
        <button type="button" class="boton-tp-exito" :disabled="guardandoReactivar" @click="confirmarReactivar">
          {{ guardandoReactivar ? 'Reactivando...' : 'Reactivar' }}
        </button>
      </template>
    </ModalBase>

    <!-- Modal: Eliminar -->
    <ModalBase v-if="modalEliminarAbierto" titulo="Eliminar cliente" ancho="confirmacion" @cerrar="modalEliminarAbierto = false">
      <template #subtitulo><span class="pill-tp">solo admin</span></template>
      <p class="text-sm text-ink/80">
        <strong>{{ clienteEliminando?.nombreCompleto }}</strong> se ocultará de los listados. Sus datos, vehículos e historial se conservan y puedes restaurarla desde el filtro Eliminados.
      </p>
      <div class="mt-4">
        <label class="etiqueta-tp" for="motivoEliminar">Motivo *</label>
        <textarea id="motivoEliminar" v-model="motivoEliminar" rows="3" maxlength="255" class="campo-tp !h-auto py-2.5"></textarea>
        <p class="ayuda-tp">{{ motivoEliminar.length }} / 255 · mínimo 10</p>
      </div>
      <template #footer>
        <button type="button" class="boton-tp-secundario" @click="modalEliminarAbierto = false">Cancelar</button>
        <button type="button" class="boton-tp-peligro" :disabled="!!validarMotivo(motivoEliminar) || guardandoEliminar" @click="confirmarEliminar">
          {{ guardandoEliminar ? 'Eliminando...' : 'Eliminar' }}
        </button>
      </template>
    </ModalBase>

    <!-- Modal: Asociar vehículo -->
    <ModalAsociarVehiculo v-if="modalVehiculoAbierto" :cliente="clienteParaVehiculo"
                           @cerrar="modalVehiculoAbierto = false" @guardado="alGuardarVehiculo" />
  </div>
</template>
