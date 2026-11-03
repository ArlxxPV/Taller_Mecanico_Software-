<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import { rolFacade } from '../facades/rolFacade'
import { usuarioFacade } from '../facades/usuarioFacade'
import { clienteFacade } from '../facades/clienteFacade'
import { tallerFacade } from '../facades/tallerFacade'
import { useAuthStore } from '../stores/auth'

const toast = useToast()
const auth = useAuthStore()

const puedeAdministrarRoles = computed(() => auth.permisosUsuario.includes('ROLES_ADMINISTRAR'))
const puedeCrearPersonal = computed(() => auth.permisosUsuario.includes('USUARIOS_CREAR'))

const roles = ref([])
const permisosDisponibles = ref([])
const usuarios = ref([])
const clientesSinAcceso = ref([])
const talleres = ref([])

const cargandoRoles = ref(true)
const cargandoUsuarios = ref(true)
const cargandoClientesSinAcceso = ref(true)

function rolNecesitaTaller(rolId) {
  const rol = roles.value.find((r) => r.id === Number(rolId))
  return rol ? !rol.permisos.includes('CLIENTES_TODOS_TALLERES') : false
}

async function cargarRoles() {
  cargandoRoles.value = true
  const [resultadoRoles, resultadoPermisos] = await Promise.all([rolFacade.listar(), rolFacade.permisosDisponibles()])
  cargandoRoles.value = false

  if (resultadoPermisos.ok) permisosDisponibles.value = resultadoPermisos.permisos
  else toast.add({ severity: 'error', summary: 'No se pudo cargar el catálogo de permisos', detail: resultadoPermisos.mensaje, life: 4500 })

  if (resultadoRoles.ok) {
    roles.value = resultadoRoles.roles
    roles.value.forEach((rol) => { seleccionPermisos[rol.id] = new Set(rol.permisos) })
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cargar la lista de roles', detail: resultadoRoles.mensaje, life: 4500 })
  }
}

async function cargarUsuarios() {
  cargandoUsuarios.value = true
  const resultado = await usuarioFacade.listar()
  cargandoUsuarios.value = false
  if (resultado.ok) usuarios.value = resultado.usuarios
  else toast.add({ severity: 'error', summary: 'No se pudo cargar la lista de personal', detail: resultado.mensaje, life: 4500 })
}

async function cargarClientesSinAcceso() {
  cargandoClientesSinAcceso.value = true
  const resultado = await clienteFacade.listarSinAcceso()
  cargandoClientesSinAcceso.value = false
  if (resultado.ok) clientesSinAcceso.value = resultado.clientes
  else toast.add({ severity: 'error', summary: 'No se pudo cargar la lista', detail: resultado.mensaje, life: 4500 })
}

onMounted(async () => {
  await cargarRoles()
  if (puedeCrearPersonal.value) {
    cargarUsuarios()
    cargarClientesSinAcceso()
    const resultadoTalleres = await tallerFacade.listar()
    if (resultadoTalleres.ok) talleres.value = resultadoTalleres.talleres
  }
})

// ---------- Roles: editar permisos existentes ----------
const seleccionPermisos = reactive({})
const editandoPermisos = reactive({})
const guardandoPermisos = reactive({})
const cambiandoEstadoRol = reactive({})

function marcado(rolId, clave) {
  return seleccionPermisos[rolId]?.has(clave) ?? false
}

function alternarPermiso(rolId, clave) {
  if (!seleccionPermisos[rolId].has(clave)) seleccionPermisos[rolId].add(clave)
  else seleccionPermisos[rolId].delete(clave)
}

function etiquetaPermiso(clave) {
  return permisosDisponibles.value.find((p) => p.clave === clave)?.descripcion || clave
}

function iniciarEdicionPermisos(rol) {
  seleccionPermisos[rol.id] = new Set(rol.permisos)
  editandoPermisos[rol.id] = true
}

function cancelarEdicionPermisos(rolId) {
  editandoPermisos[rolId] = false
}

async function guardarPermisos(rol) {
  guardandoPermisos[rol.id] = true
  const resultado = await rolFacade.actualizarPermisos(rol.id, [...seleccionPermisos[rol.id]])
  guardandoPermisos[rol.id] = false
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Permisos actualizados', detail: resultado.mensaje, life: 3000 })
    editandoPermisos[rol.id] = false
    await cargarRoles()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudieron guardar los permisos', detail: resultado.mensaje, life: 4500 })
  }
}

async function alternarEstadoRol(rol) {
  cambiandoEstadoRol[rol.id] = true
  const resultado = await rolFacade.cambiarEstado(rol.id, !rol.activo)
  cambiandoEstadoRol[rol.id] = false
  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Estado actualizado', detail: resultado.mensaje, life: 3000 })
    await cargarRoles()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo cambiar el estado del rol', detail: resultado.mensaje, life: 4500 })
  }
}

// ---------- Roles: crear uno nuevo ----------
const mostrarFormRol = ref(false)
const formRol = reactive({ nombre: '', descripcion: '' })
const permisosNuevoRol = ref(new Set())
const guardandoRol = ref(false)

function alternarPermisoNuevoRol(clave) {
  if (!permisosNuevoRol.value.has(clave)) permisosNuevoRol.value.add(clave)
  else permisosNuevoRol.value.delete(clave)
}

async function crearRol() {
  if (permisosNuevoRol.value.size === 0) {
    toast.add({ severity: 'warn', summary: 'Selecciona al menos un permiso', life: 3500 })
    return
  }
  guardandoRol.value = true
  const resultado = await rolFacade.crear(formRol.nombre, formRol.descripcion, [...permisosNuevoRol.value])
  guardandoRol.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Rol creado', detail: resultado.mensaje, life: 3000 })
    formRol.nombre = ''
    formRol.descripcion = ''
    permisosNuevoRol.value = new Set()
    mostrarFormRol.value = false
    await cargarRoles()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo crear el rol', detail: resultado.mensaje, life: 4500 })
  }
}

// ---------- Personal: agrupado por rol, con total por grupo ----------
const PALETA_ROLES = [
  'bg-sky-100 text-sky-700',
  'bg-violet-100 text-violet-700',
  'bg-amber-100 text-amber-700',
  'bg-emerald-100 text-emerald-700',
  'bg-rose-100 text-rose-700',
  'bg-indigo-100 text-indigo-700'
]

function colorRol(rol) {
  if (rol.esSistema) return 'bg-mist text-primary'
  const rolesNormales = roles.value.filter((r) => !r.esSistema)
  const indice = rolesNormales.findIndex((r) => r.id === rol.id)
  return PALETA_ROLES[indice % PALETA_ROLES.length]
}

const gruposPersonal = computed(() =>
  roles.value.map((rol) => ({
    rol,
    usuarios: usuarios.value.filter((u) => u.roles.includes(rol.nombre))
  }))
)

// ---------- Personal: cambiar rol de una cuenta existente ----------
const edicionRol = reactive({})
const guardandoRolUsuario = reactive({})

function iniciarEdicionRol(usuario) {
  const rolActual = roles.value.find((r) => r.nombre === usuario.roles[0])
  edicionRol[usuario.id] = { rolId: rolActual?.id ?? '', tallerId: usuario.tallerId ?? '' }
}

function cancelarEdicionRol(usuarioId) {
  delete edicionRol[usuarioId]
}

async function guardarRolUsuario(usuario) {
  const cambio = edicionRol[usuario.id]
  guardandoRolUsuario[usuario.id] = true
  const resultado = await usuarioFacade.cambiarRol(usuario.id, cambio.rolId, cambio.tallerId)
  guardandoRolUsuario[usuario.id] = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Rol actualizado', detail: resultado.mensaje, life: 3000 })
    delete edicionRol[usuario.id]
    await cargarUsuarios()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo actualizar el rol', detail: resultado.mensaje, life: 4500 })
  }
}

// ---------- Personal: registrar cuenta nueva ----------
const mostrarFormPersonal = ref(false)
const formPersonal = reactive({
  nombres: '', apellidoPaterno: '', apellidoMaterno: '', email: '', password: '', rolId: '', tallerId: ''
})
const guardandoPersonal = ref(false)
const necesitaTallerNuevo = computed(() => formPersonal.rolId !== '' && rolNecesitaTaller(formPersonal.rolId))

async function crearPersonal() {
  guardandoPersonal.value = true
  const resultado = await usuarioFacade.crear(formPersonal)
  guardandoPersonal.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cuenta creada', detail: resultado.mensaje, life: 3000 })
    Object.keys(formPersonal).forEach((campo) => { formPersonal[campo] = '' })
    mostrarFormPersonal.value = false
    await cargarUsuarios()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo crear la cuenta', detail: resultado.mensaje, life: 4500 })
  }
}

// ---------- Clientes sin acceso: asignar contraseña ----------
const passwordPorCliente = reactive({})
const asignandoAcceso = reactive({})

async function asignarAcceso(cliente) {
  const password = passwordPorCliente[cliente.id]
  if (!password) {
    toast.add({ severity: 'warn', summary: 'Escribe una contraseña', life: 3500 })
    return
  }
  asignandoAcceso[cliente.id] = true
  const resultado = await clienteFacade.asignarAcceso(cliente.id, password)
  asignandoAcceso[cliente.id] = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Acceso asignado', detail: resultado.mensaje, life: 4000 })
    delete passwordPorCliente[cliente.id]
    await cargarClientesSinAcceso()
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo asignar la contraseña', detail: resultado.mensaje, life: 4500 })
  }
}
</script>

<template>
  <div class="mx-auto max-w-6xl">
    <h1 class="titulo-pagina">Roles y personal</h1>
    <p class="subtitulo-pagina">Qué puede hacer cada rol, quién tiene cada cuenta, y a quién le falta acceso.</p>

    <!-- Roles -->
    <section v-if="puedeAdministrarRoles" class="mt-8">
      <div class="flex items-center justify-between">
        <h2 class="font-display text-base font-semibold text-ink">Roles</h2>
        <button type="button" class="boton-tp-texto text-primary" @click="mostrarFormRol = !mostrarFormRol">
          {{ mostrarFormRol ? 'Cancelar' : '+ Crear rol' }}
        </button>
      </div>

      <div v-if="mostrarFormRol" class="mt-4 tarjeta-tp">
        <div class="grid gap-5 sm:grid-cols-2">
          <div>
            <label class="etiqueta-tp" for="nombreRol">Nombre del rol</label>
            <input id="nombreRol" v-model="formRol.nombre" type="text" required class="campo-tp" placeholder="Ej. Supervisor" />
          </div>
          <div>
            <label class="etiqueta-tp" for="descripcionRol">Descripción</label>
            <input id="descripcionRol" v-model="formRol.descripcion" type="text" required class="campo-tp" placeholder="Para qué sirve" />
          </div>
        </div>
        <div class="mt-4 grid gap-2.5 sm:grid-cols-2">
          <label v-for="permiso in permisosDisponibles" :key="permiso.clave" class="flex items-start gap-2.5 text-sm text-ink">
            <input type="checkbox" class="mt-0.5" :checked="permisosNuevoRol.has(permiso.clave)" @change="alternarPermisoNuevoRol(permiso.clave)" />
            <span>{{ permiso.descripcion }}</span>
          </label>
        </div>
        <button type="button" class="boton-tp-primario mt-5" :disabled="guardandoRol" @click="crearRol">
          {{ guardandoRol ? 'Guardando...' : 'Crear rol' }}
        </button>
      </div>

      <div v-if="cargandoRoles" class="mt-4 text-sm text-secondary">Cargando roles...</div>
      <div v-else class="mt-4 space-y-4">
        <div v-for="rol in roles" :key="rol.id" class="tarjeta-tp">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 class="font-display text-base font-semibold text-ink">{{ rol.nombre }}</h3>
              <p class="mt-0.5 text-sm text-secondary">{{ rol.descripcion }}</p>
            </div>
            <div class="flex items-center gap-3">
              <span v-if="rol.esSistema" class="pill-tp">Acceso total</span>
              <span v-else :class="rol.activo ? 'chip-estatus-activo' : 'chip-estatus-suspendido'">
                {{ rol.activo ? 'Activo' : 'Suspendido' }}
              </span>
              <button v-if="!rol.esSistema" type="button" class="boton-tp-texto" :class="rol.activo ? 'text-danger' : 'text-success'"
                      :disabled="cambiandoEstadoRol[rol.id]" @click="alternarEstadoRol(rol)">
                {{ rol.activo ? 'Suspender' : 'Reactivar' }}
              </button>
              <button v-if="!rol.esSistema && !editandoPermisos[rol.id]" type="button"
                      class="boton-tp-texto text-primary" @click="iniciarEdicionPermisos(rol)">
                Editar permisos
              </button>
            </div>
          </div>

          <!-- Permisos: solo lectura hasta que se de "Editar permisos" (o siempre, si es ADMIN) -->
          <div v-if="rol.esSistema || !editandoPermisos[rol.id]" class="mt-4">
            <p v-if="rol.esSistema" class="text-sm text-secondary">Todos los permisos (no se puede editar).</p>
            <p v-else-if="rol.permisos.length === 0" class="text-sm text-secondary">Este rol todavía no tiene ningún permiso.</p>
            <div v-else class="flex flex-wrap gap-2">
              <span v-for="clave in rol.permisos" :key="clave" class="pill-tp">
                {{ etiquetaPermiso(clave) }}
              </span>
            </div>
          </div>

          <template v-else>
            <div class="mt-4 grid gap-2.5 sm:grid-cols-2">
              <label v-for="permiso in permisosDisponibles" :key="permiso.clave" class="flex items-start gap-2.5 text-sm text-ink">
                <input type="checkbox" class="mt-0.5" :checked="marcado(rol.id, permiso.clave)" @change="alternarPermiso(rol.id, permiso.clave)" />
                <span>{{ permiso.descripcion }}</span>
              </label>
            </div>

            <div class="mt-5 flex gap-3">
              <button type="button" class="boton-tp-primario"
                      :disabled="guardandoPermisos[rol.id]" @click="guardarPermisos(rol)">
                {{ guardandoPermisos[rol.id] ? 'Guardando...' : 'Guardar cambios' }}
              </button>
              <button type="button" class="boton-tp-secundario" @click="cancelarEdicionPermisos(rol.id)">
                Cancelar
              </button>
            </div>
          </template>
        </div>
      </div>
    </section>

    <!-- Personal -->
    <section v-if="puedeCrearPersonal" class="mt-10">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="font-display text-base font-semibold text-ink">Personal</h2>
          <p class="mt-0.5 text-sm text-secondary">{{ usuarios.length }} cuenta{{ usuarios.length === 1 ? '' : 's' }} en total.</p>
        </div>
        <button type="button" class="boton-tp-texto text-primary" @click="mostrarFormPersonal = !mostrarFormPersonal">
          {{ mostrarFormPersonal ? 'Cancelar' : '+ Registrar personal' }}
        </button>
      </div>

      <form v-if="mostrarFormPersonal" class="mt-4 tarjeta-tp" @submit.prevent="crearPersonal">
        <div class="grid gap-5 sm:grid-cols-2">
          <div>
            <label class="etiqueta-tp" for="nombresP">Nombre(s)</label>
            <input id="nombresP" v-model="formPersonal.nombres" type="text" required class="campo-tp" />
          </div>
          <div>
            <label class="etiqueta-tp" for="apellidoPaternoP">Apellido paterno</label>
            <input id="apellidoPaternoP" v-model="formPersonal.apellidoPaterno" type="text" required class="campo-tp" />
          </div>
          <div>
            <label class="etiqueta-tp" for="apellidoMaternoP">Apellido materno</label>
            <input id="apellidoMaternoP" v-model="formPersonal.apellidoMaterno" type="text" required class="campo-tp" />
          </div>
          <div>
            <label class="etiqueta-tp" for="emailP">Correo</label>
            <input id="emailP" v-model="formPersonal.email" type="email" required class="campo-tp" />
          </div>
          <div class="sm:col-span-2">
            <label class="etiqueta-tp" for="passwordP">Contraseña</label>
            <input id="passwordP" v-model="formPersonal.password" type="password" required class="campo-tp"
                   placeholder="Mínimo 8 caracteres, con mayúscula, minúscula y número" />
          </div>
          <div>
            <label class="etiqueta-tp" for="rolP">Rol</label>
            <select id="rolP" v-model="formPersonal.rolId" required class="campo-tp">
              <option value="" disabled>Selecciona un rol</option>
              <option v-for="rol in roles.filter((r) => r.activo)" :key="rol.id" :value="rol.id">{{ rol.nombre }}</option>
            </select>
          </div>
          <div v-if="necesitaTallerNuevo">
            <label class="etiqueta-tp" for="tallerP">Taller</label>
            <select id="tallerP" v-model="formPersonal.tallerId" required class="campo-tp">
              <option value="" disabled>Selecciona un taller</option>
              <option v-for="t in talleres" :key="t.id" :value="t.id">{{ t.nombre }}</option>
            </select>
          </div>
        </div>
        <button type="submit" class="boton-tp-primario mt-5" :disabled="guardandoPersonal">
          {{ guardandoPersonal ? 'Guardando...' : 'Registrar personal' }}
        </button>
      </form>

      <div v-if="cargandoUsuarios" class="mt-4 text-sm text-secondary">Cargando...</div>
      <template v-else>
        <!-- Total por rol -->
        <div class="mt-4 grid gap-4 sm:grid-cols-3 lg:grid-cols-4">
          <div v-for="grupo in gruposPersonal" :key="'tarjeta-' + grupo.rol.id" class="tarjeta-tp">
            <span class="insignia-estado" :class="colorRol(grupo.rol)">{{ grupo.rol.nombre }}</span>
            <p class="mt-2 font-display text-2xl font-bold text-ink">{{ grupo.usuarios.length }}</p>
            <p class="text-xs text-secondary">cuenta{{ grupo.usuarios.length === 1 ? '' : 's' }}</p>
          </div>
        </div>

        <!-- Un apartado por rol -->
        <div class="mt-6 space-y-6">
          <div v-for="grupo in gruposPersonal" :key="'grupo-' + grupo.rol.id">
            <div v-if="grupo.usuarios.length > 0">
              <div class="flex items-center gap-2">
                <span class="insignia-estado" :class="colorRol(grupo.rol)">{{ grupo.rol.nombre }}</span>
                <span class="text-sm text-secondary">{{ grupo.usuarios.length }} cuenta{{ grupo.usuarios.length === 1 ? '' : 's' }}</span>
              </div>

              <div class="mt-3 overflow-hidden rounded-[14px] border border-mist bg-white">
                <div class="overflow-x-auto">
                  <table class="tabla-tp w-full">
                    <thead>
                      <tr>
                        <th>Nombre</th>
                        <th>Correo</th>
                        <th>Rol</th>
                        <th>Taller</th>
                        <th>Acciones</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="usuario in grupo.usuarios" :key="usuario.id">
                        <td class="font-medium text-ink">{{ usuario.nombreCompleto }}</td>
                        <td class="text-ink/70">{{ usuario.email }}</td>
                        <td class="text-ink/70">
                          <template v-if="!edicionRol[usuario.id]">{{ usuario.roles.join(', ') }}</template>
                          <select v-else v-model="edicionRol[usuario.id].rolId" class="campo-tp">
                            <option v-for="rol in roles.filter((r) => r.activo)" :key="rol.id" :value="rol.id">{{ rol.nombre }}</option>
                          </select>
                        </td>
                        <td class="text-ink/70">
                          <template v-if="!edicionRol[usuario.id]">{{ usuario.tallerNombre || '—' }}</template>
                          <select v-else-if="rolNecesitaTaller(edicionRol[usuario.id].rolId)" v-model="edicionRol[usuario.id].tallerId" class="campo-tp">
                            <option value="" disabled>Selecciona un taller</option>
                            <option v-for="t in talleres" :key="t.id" :value="t.id">{{ t.nombre }}</option>
                          </select>
                          <span v-else class="text-secondary">—</span>
                        </td>
                        <td>
                          <div v-if="!edicionRol[usuario.id]">
                            <button type="button" class="boton-tp-texto text-primary" @click="iniciarEdicionRol(usuario)">
                              Cambiar rol
                            </button>
                          </div>
                          <div v-else class="flex gap-3">
                            <button type="button" class="boton-tp-texto text-primary"
                                    :disabled="guardandoRolUsuario[usuario.id]" @click="guardarRolUsuario(usuario)">
                              {{ guardandoRolUsuario[usuario.id] ? 'Guardando...' : 'Guardar' }}
                            </button>
                            <button type="button" class="boton-tp-texto text-secondary" @click="cancelarEdicionRol(usuario.id)">
                              Cancelar
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </section>

    <!-- Clientes sin acceso -->
    <section v-if="puedeCrearPersonal" class="mt-10">
      <h2 class="font-display text-base font-semibold text-ink">Clientes sin acceso</h2>
      <p class="mt-1 text-sm text-secondary">
        Clientes registrados por personal (no se registraron ellos mismos): asígnales una contraseña para que puedan iniciar sesión con su correo.
      </p>

      <div v-if="cargandoClientesSinAcceso" class="mt-4 text-sm text-secondary">Cargando...</div>
      <div v-else-if="clientesSinAcceso.length === 0" class="mt-4 tarjeta-tp text-center text-sm text-secondary">
        Todos los clientes ya tienen con qué iniciar sesión.
      </div>
      <div v-else class="mt-4 overflow-hidden rounded-[14px] border border-mist bg-white">
        <div class="overflow-x-auto">
          <table class="tabla-tp w-full">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Correo</th>
                <th>Asignar contraseña</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cliente in clientesSinAcceso" :key="cliente.id">
                <td class="font-medium text-ink">{{ cliente.nombreCompleto }}</td>
                <td class="text-ink/70">{{ cliente.email }}</td>
                <td>
                  <div class="flex gap-2">
                    <input v-model="passwordPorCliente[cliente.id]" type="password" class="campo-tp"
                           placeholder="Mínimo 8, mayúscula, minúscula y número" />
                    <button type="button" class="boton-tp-primario whitespace-nowrap"
                            :disabled="asignandoAcceso[cliente.id]" @click="asignarAcceso(cliente)">
                      {{ asignandoAcceso[cliente.id] ? 'Guardando...' : 'Asignar' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>
