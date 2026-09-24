<script setup>
import { ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { authService } from '../services/authService'
import AuthLayout from '../components/AuthLayout.vue'

const route = useRoute()
const router = useRouter()

const token = ref(route.query.token || '')
const nuevaPassword = ref('')
const confirmarPassword = ref('')
const cargando = ref(false)
const listo = ref(false)
const error = ref('')

async function enviar() {
  error.value = ''

  if (nuevaPassword.value !== confirmarPassword.value) {
    error.value = 'Las dos contrasenas no coinciden.'
    return
  }
  if (!token.value) {
    error.value = 'Falta el token del enlace. Copia el enlace completo que recibiste por correo.'
    return
  }

  cargando.value = true
  try {
    await authService.restablecerPassword(token.value, nuevaPassword.value)
    listo.value = true
    setTimeout(() => router.push({ name: 'login' }), 2500)
  } catch (e) {
    error.value = e.response?.data?.mensaje || 'El enlace no es valido o ya expiro.'
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <AuthLayout titulo="Crea una nueva contrasena" subtitulo="Elige una contrasena que no hayas usado antes en TallerPro.">
    <div v-if="listo" class="rounded-lg bg-mist px-4 py-4 text-sm text-ink/80">
      Tu contrasena se actualizo. Te llevamos a inicio de sesion...
    </div>

    <form v-else class="space-y-5" @submit.prevent="enviar">
      <div v-if="!route.query.token">
        <label class="etiqueta-campo" for="token">Token del enlace</label>
        <input id="token" v-model="token" type="text" required
               class="campo-tallerpro font-mono text-sm" placeholder="Pega aqui el token recibido" />
      </div>

      <div>
        <label class="etiqueta-campo" for="nueva">Nueva contrasena</label>
        <input id="nueva" v-model="nuevaPassword" type="password" required autocomplete="new-password"
               class="campo-tallerpro" placeholder="Minimo 8 caracteres" />
      </div>

      <div>
        <label class="etiqueta-campo" for="confirmar">Confirma la contrasena</label>
        <input id="confirmar" v-model="confirmarPassword" type="password" required autocomplete="new-password"
               class="campo-tallerpro" placeholder="Repite la contrasena" />
      </div>

      <p v-if="error" class="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{{ error }}</p>

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Guardando...' : 'Guardar nueva contrasena' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      <RouterLink :to="{ name: 'login' }" class="font-medium text-primary hover:text-primary-hover">Volver a inicio de sesion</RouterLink>
    </p>
  </AuthLayout>
</template>
