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
    name: 'panel',
    component: () => import('../views/DashboardView.vue'),
    meta: { requiereAuth: true }
  },
  {
    path: '/clientes',
    name: 'clientes-lista',
    component: () => import('../views/ClienteListaView.vue'),
    meta: { requiereAuth: true, rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'] }
  },
  {
    path: '/clientes/nuevo',
    name: 'clientes-nuevo',
    component: () => import('../views/ClienteRegistroView.vue'),
    meta: { requiereAuth: true, rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'] }
  },
  {
    path: '/clientes/:id',
    name: 'clientes-detalle',
    component: () => import('../views/ClienteDetalleView.vue'),
    meta: { requiereAuth: true, rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'] }
  },
  {
    path: '/clientes/:id/editar',
    name: 'clientes-editar',
    component: () => import('../views/ClienteEditarView.vue'),
    meta: { requiereAuth: true, rolesPermitidos: ['ADMIN', 'RECEPCIONISTA'] }
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

  return true
})

export default router
