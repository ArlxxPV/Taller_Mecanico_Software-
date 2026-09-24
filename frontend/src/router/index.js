import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiereAuth && !auth.estaAutenticado) {
    return { name: 'login' }
  }
  if (to.meta.soloInvitado && auth.estaAutenticado) {
    return { name: 'panel' }
  }
  return true
})

export default router
