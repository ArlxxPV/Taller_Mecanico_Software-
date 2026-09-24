<script setup>
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AuthLayout from '../components/AuthLayout.vue'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const cargando = ref(false)
const error = ref('')

async function enviar() {
  error.value = ''
  cargando.value = true
  try {
    await auth.login({ email: email.value, password: password.value })
    router.push({ name: 'panel' })
  } catch (e) {
    error.value = e.response?.data?.mensaje || 'No se pudo iniciar sesion. Intenta de nuevo.'
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <AuthLayout titulo="Inicia sesion" subtitulo="Entra con el correo y la contrasena de tu cuenta.">
    <form class="space-y-5" @submit.prevent="enviar">
      <div>
        <label class="etiqueta-campo" for="email">Correo</label>
        <input id="email" v-model="email" type="email" required autocomplete="email"
               class="campo-tallerpro" placeholder="tu@correo.com" />
      </div>

      <div>
        <div class="flex items-center justify-between">
          <label class="etiqueta-campo" for="password">Contrasena</label>
          <RouterLink :to="{ name: 'olvide-password' }" class="mb-1.5 text-sm font-medium text-primary hover:text-primary-hover">
            Olvide mi contrasena
          </RouterLink>
        </div>
        <input id="password" v-model="password" type="password" required autocomplete="current-password"
               class="campo-tallerpro" placeholder="••••••••" />
      </div>

      <p v-if="error" class="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{{ error }}</p>

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Entrando...' : 'Iniciar sesion' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      ¿Aun no tienes cuenta?
      <RouterLink :to="{ name: 'registro' }" class="font-medium text-primary hover:text-primary-hover">Crea una</RouterLink>
    </p>
  </AuthLayout>
</template>
