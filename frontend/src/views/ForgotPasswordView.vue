<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { authFacade } from '../facades/authFacade'
import AuthLayout from '../components/AuthLayout.vue'

const toast = useToast()

const email = ref('')
const cargando = ref(false)
const enviado = ref(false)

async function enviar() {
  cargando.value = true
  const resultado = await authFacade.olvidePassword(email.value)
  cargando.value = false

  if (resultado.ok) {
    enviado.value = true
    toast.add({ severity: 'success', summary: 'Solicitud enviada', detail: resultado.mensaje, life: 3000 })
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo procesar', detail: resultado.mensaje, life: 4000 })
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

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Enviando...' : 'Enviar enlace de recuperacion' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      <RouterLink :to="{ name: 'login' }" class="font-medium text-primary hover:text-primary-hover">Volver a inicio de sesion</RouterLink>
    </p>
  </AuthLayout>
</template>
