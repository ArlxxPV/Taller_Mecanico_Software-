<script setup>
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AuthLayout from '../components/AuthLayout.vue'

const router = useRouter()
const auth = useAuthStore()

const nombreCompleto = ref('')
const email = ref('')
const telefono = ref('')
const password = ref('')
const cargando = ref(false)
const error = ref('')
const errores = ref([])

async function enviar() {
  error.value = ''
  errores.value = []
  cargando.value = true
  try {
    await auth.registrar({
      nombreCompleto: nombreCompleto.value,
      email: email.value,
      telefono: telefono.value || null,
      password: password.value
    })
    router.push({ name: 'login', query: { registrado: '1' } })
  } catch (e) {
    error.value = e.response?.data?.mensaje || 'No se pudo crear la cuenta.'
    errores.value = e.response?.data?.detalles || []
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <AuthLayout titulo="Crea tu cuenta" subtitulo="Registra tus datos para dar seguimiento a tus vehiculos con el taller.">
    <form class="space-y-5" @submit.prevent="enviar">
      <div>
        <label class="etiqueta-campo" for="nombre">Nombre completo</label>
        <input id="nombre" v-model="nombreCompleto" type="text" required
               class="campo-tallerpro" placeholder="Ej. Ana Torres Ramirez" />
      </div>

      <div>
        <label class="etiqueta-campo" for="email">Correo</label>
        <input id="email" v-model="email" type="email" required autocomplete="email"
               class="campo-tallerpro" placeholder="tu@correo.com" />
      </div>

      <div>
        <label class="etiqueta-campo" for="telefono">Telefono (opcional)</label>
        <input id="telefono" v-model="telefono" type="tel"
               class="campo-tallerpro" placeholder="55 1234 5678" />
      </div>

      <div>
        <label class="etiqueta-campo" for="password">Contrasena</label>
        <input id="password" v-model="password" type="password" required autocomplete="new-password"
               class="campo-tallerpro" placeholder="Minimo 8 caracteres" />
        <p class="mt-1.5 text-xs text-ink/50">Usa mayuscula, minuscula y al menos un numero.</p>
      </div>

      <div v-if="error" class="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        <p>{{ error }}</p>
        <ul v-if="errores.length" class="mt-1 list-disc pl-4">
          <li v-for="(detalle, i) in errores" :key="i">{{ detalle }}</li>
        </ul>
      </div>

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Creando cuenta...' : 'Crear cuenta' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      ¿Ya tienes cuenta?
      <RouterLink :to="{ name: 'login' }" class="font-medium text-primary hover:text-primary-hover">Inicia sesion</RouterLink>
    </p>
  </AuthLayout>
</template>
