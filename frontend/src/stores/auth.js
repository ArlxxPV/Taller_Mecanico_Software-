import { defineStore } from 'pinia'
import { authService } from '../services/authService'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('tallerpro_token') || null,
    usuario: null
  }),

  getters: {
    estaAutenticado: (state) => !!state.token,
    rolesUsuario: (state) => state.usuario?.roles ?? []
  },

  actions: {
    async login(credenciales) {
      const { data } = await authService.login(credenciales)
      this.token = data.token
      this.usuario = data.usuario
      localStorage.setItem('tallerpro_token', data.token)
      return data
    },

    async registrar(datos) {
      return authService.registrar(datos)
    },

    async cargarPerfil() {
      if (!this.token) return null
      const { data } = await authService.obtenerPerfil()
      this.usuario = data
      return data
    },

    cerrarSesion() {
      this.token = null
      this.usuario = null
      localStorage.removeItem('tallerpro_token')
    }
  }
})
