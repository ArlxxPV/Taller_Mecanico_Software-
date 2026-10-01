import { defineStore } from 'pinia'

// Store: solo guarda el estado de la sesion (token, usuario) y lo persiste
// en localStorage. No hace peticiones HTTP: eso es trabajo del Facade,
// a traves del Repository.
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
    establecerSesion(token, usuario) {
      this.token = token
      this.usuario = usuario
      localStorage.setItem('tallerpro_token', token)
    },

    establecerUsuario(usuario) {
      this.usuario = usuario
    },

    limpiarSesion() {
      this.token = null
      this.usuario = null
      localStorage.removeItem('tallerpro_token')
    }
  }
})
