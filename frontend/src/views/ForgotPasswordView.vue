<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { authService } from '../services/authService'
import AuthLayout from '../components/AuthLayout.vue'

const email = ref('')
const cargando = ref(false)
const enviado = ref(false)
const error = ref('')

async function enviar() {
  error.value = ''
  cargando.value = true
  try {
    await authService.olvidePassword(email.value)
    enviado.value = true
  } catch {
    error.value = 'No se pudo procesar la solicitud. Intenta de nuevo en unos minutos.'
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <AuthLayout
    titulo="Recupera tu acceso"
    subtitulo="Escribe el correo de tu cuenta y te enviaremos un enlace para crear una contrasena nueva."
  >
    <div v-if="enviado" class="rounded-lg bg-mist px-4 py-4 text-sm text-ink/80">
      Si <strong>{{ email }}</strong> esta registrado, recibiras un correo con el enlace de recuperacion
      en los proximos minutos. Revisa tambien la carpeta de spam.
    </div>

    <form v-else class="space-y-5" @submit.prevent="enviar">
      <div>
        <label class="etiqueta-campo" for="email">Correo</label>
        <input id="email" v-model="email" type="email" required autocomplete="email"
               class="campo-tallerpro" placeholder="tu@correo.com" />
      </div>

      <p v-if="error" class="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{{ error }}</p>

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Enviando...' : 'Enviar enlace de recuperacion' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      <RouterLink :to="{ name: 'login' }" class="font-medium text-primary hover:text-primary-hover">Volver a inicio de sesion</RouterLink>
    </p>
  </AuthLayout>
</template>
