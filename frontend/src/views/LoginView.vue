<script setup>
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { authFacade } from '../facades/authFacade'
import AuthLayout from '../components/AuthLayout.vue'

const router = useRouter()
const toast = useToast()

const email = ref('')
const password = ref('')
const mostrarPassword = ref(false)
const cargando = ref(false)

async function enviar() {
  cargando.value = true
  const resultado = await authFacade.login({ email: email.value, password: password.value })
  cargando.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Bienvenido', detail: resultado.mensaje, life: 3000 })
    router.push({ name: 'panel' })
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo iniciar sesion', detail: resultado.mensaje, life: 4000 })
  }
}
</script>

<template>
  <AuthLayout
    titulo="Iniciar sesión"
    subtitulo="Ingresa con tu cuenta para entrar al taller."
    titulo-panel="Cada reparación, bajo control."
    descripcion-panel="Gestiona clientes, vehículos y órdenes de servicio desde un solo lugar, rápido y sin complicaciones."
    :caracteristicas="[
      { icono: 'pi-check-square', texto: 'Órdenes de reparación al día' },
      { icono: 'pi-users', texto: 'Clientes y vehículos ordenados' },
      { icono: 'pi-shield', texto: 'Accesos seguros por rol' }
    ]"
  >
    <form class="space-y-5" @submit.prevent="enviar">
      <div>
        <label class="etiqueta-campo" for="email">Correo electrónico</label>
        <div class="relative">
          <i class="pi pi-envelope pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-primary/70"></i>
          <input id="email" v-model="email" type="email" required autocomplete="email"
                 class="campo-tallerpro pl-10" placeholder="tucorreo@taller.com" />
        </div>
      </div>

      <div>
        <label class="etiqueta-campo" for="password">Contraseña</label>
        <div class="relative">
          <i class="pi pi-lock pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-primary/70"></i>
          <input id="password" v-model="password" :type="mostrarPassword ? 'text' : 'password'" required
                 autocomplete="current-password" class="campo-tallerpro pl-10 pr-11" placeholder="Tu contraseña" />
          <button
            type="button"
            class="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-ink/40 hover:text-ink/70"
            :aria-label="mostrarPassword ? 'Ocultar contrasena' : 'Mostrar contrasena'"
            @click="mostrarPassword = !mostrarPassword"
          >
            <i class="pi" :class="mostrarPassword ? 'pi-eye-slash' : 'pi-eye'"></i>
          </button>
        </div>
        <div class="mt-1.5 text-right">
          <RouterLink :to="{ name: 'olvide-password' }" class="text-sm font-medium text-primary hover:text-primary-hover">
            ¿Olvidaste tu contraseña?
          </RouterLink>
        </div>
      </div>

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Entrando...' : 'Entrar' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      ¿Aún no tienes cuenta?
      <RouterLink :to="{ name: 'registro' }" class="font-medium text-primary hover:text-primary-hover">Crear cuenta</RouterLink>
    </p>
  </AuthLayout>
</template>
