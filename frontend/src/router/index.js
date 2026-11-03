import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { authFacade } from '../facades/authFacade'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { soloInvitado: true }
  },
  {
    path: '/registro',
    name: 'registro',
    component: () => import('../views/RegisterView.vue'),
    meta: { soloInvitado: true }
  },
  {
    path: '/olvide-password',
    name: 'olvide-password',
    component: () => import('../views/ForgotPasswordView.vue'),
    meta: { soloInvitado: true }
  },
  {
    path: '/restablecer-password',
    name: 'restablecer-password',
    component: () => import('../views/ResetPasswordView.vue'),
    meta: { soloInvitado: true }
  },
  {
    path: '/panel',
    component: () => import('../layouts/AppLayout.vue'),
    meta: { requiereAuth: true },
    children: [
      {
        path: '',
        name: 'panel',
        component: () => import('../views/DashboardView.vue')
      },
      {
        path: 'catalogos/estatus',
        name: 'catalogos-estatus',
        component: () => import('../views/EstatusListaView.vue'),
        meta: { permisosPermitidos: ['ESTATUS_ADMINISTRAR'], breadcrumb: ['Catálogos', 'Estatus'] }
      },
      {
        path: 'catalogos/estatus/nuevo',
        name: 'catalogos-estatus-nuevo',
        component: () => import('../views/EstatusFormularioView.vue'),
        meta: { permisosPermitidos: ['ESTATUS_ADMINISTRAR'], breadcrumb: ['Catálogos', 'Estatus', 'Agregar'] }
      },
      {
        path: 'catalogos/estatus/:id/editar',
        name: 'catalogos-estatus-editar',
        component: () => import('../views/EstatusFormularioView.vue'),
        meta: { permisosPermitidos: ['ESTATUS_ADMINISTRAR'], breadcrumb: ['Catálogos', 'Estatus', 'Editar'] }
      },
      {
        path: 'clientes',
        name: 'clientes-lista',
        component: () => import('../views/ClienteListaView.vue'),
        meta: { rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'], breadcrumb: ['Clientes'] }
      },
      {
        path: 'clientes/nuevo',
        name: 'clientes-nuevo',
        component: () => import('../views/ClienteRegistroView.vue'),
        meta: { rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'], breadcrumb: ['Clientes', 'Registrar'] }
      },
      {
        path: 'clientes/:id',
        name: 'clientes-detalle',
        component: () => import('../views/ClienteDetalleView.vue'),
        meta: { rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'], breadcrumb: ['Clientes'] }
      },
      {
        path: 'clientes/:id/editar',
        name: 'clientes-editar',
        component: () => import('../views/ClienteEditarView.vue'),
        meta: { rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'], breadcrumb: ['Clientes', 'Editar'] }
      },
      {
        path: 'vehiculos',
        name: 'vehiculos-lista',
        component: () => import('../views/VehiculosView.vue'),
        meta: { rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'], breadcrumb: ['Vehículos'] }
      },
      {
        path: 'talleres',
        name: 'talleres-lista',
        component: () => import('../views/TallerListaView.vue'),
        meta: { rolesPermitidos: ['ADMIN'], breadcrumb: ['Talleres'] }
      },
      {
        path: 'talleres/nuevo',
        name: 'talleres-nuevo',
        component: () => import('../views/TallerRegistroView.vue'),
        meta: { rolesPermitidos: ['ADMIN'], breadcrumb: ['Talleres', 'Registrar'] }
      },
      {
        path: 'talleres/:id/editar',
        name: 'talleres-editar',
        component: () => import('../views/TallerEditarView.vue'),
        meta: { rolesPermitidos: ['ADMIN'], breadcrumb: ['Talleres', 'Editar'] }
      },
      {
        path: 'personal',
        name: 'personal',
        component: () => import('../views/PersonalView.vue'),
        meta: { permisosPermitidos: ['ROLES_ADMINISTRAR', 'USUARIOS_VER', 'USUARIOS_CREAR'], breadcrumb: ['Roles y personal'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.requiereAuth && !auth.estaAutenticado) {
    return { name: 'login' }
  }
  if (to.meta.soloInvitado && auth.estaAutenticado) {
    return { name: 'panel' }
  }

  if (to.meta.rolesPermitidos) {
    // El rol viaja en el perfil (auth.usuario), que solo se carga al entrar
    // al panel. Si se navega directo a una ruta con roles (URL escrita a
    // mano, recargar la pagina), lo pedimos aqui para poder autorizar.
    if (!auth.usuario) {
      await authFacade.cargarPerfil()
    }
    const autorizado = auth.rolesUsuario.some((rol) => to.meta.rolesPermitidos.includes(rol))
    if (!autorizado) {
      return { name: 'panel' }
    }
  }

  // Igual que rolesPermitidos, pero por permiso configurable (ver Roles/Personal).
  if (to.meta.permisosPermitidos) {
    if (!auth.usuario) {
      await authFacade.cargarPerfil()
    }
    const autorizado = auth.permisosUsuario.some((permiso) => to.meta.permisosPermitidos.includes(permiso))
    if (!autorizado) {
      return { name: 'panel' }
    }
  }

  return true
})

export default router
