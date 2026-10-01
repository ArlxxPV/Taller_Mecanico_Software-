<script setup>
import { computed, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { authFacade } from '../facades/authFacade'
import AuthLayout from '../components/AuthLayout.vue'

const router = useRouter()
const toast = useToast()

const nombreCompleto = ref('')
const email = ref('')
const telefono = ref('')
const password = ref('')
const mostrarPassword = ref(false)
const cargando = ref(false)
const errores = ref([])

// Barra de fuerza: puramente visual, no cambia la validacion real (esa la
// hace el backend igual que antes). 0 = vacio, 1 = debil, 2 = media, 3 = fuerte.
const fuerzaPassword = computed(() => {
  const valor = password.value
  if (!valor) return 0
  if (valor.length < 8) return 1
  const variedad = [/[A-Z]/, /[a-z]/, /\d/].filter((patron) => patron.test(valor)).length
  return variedad < 3 ? 2 : 3
})

async function enviar() {
  errores.value = []
  cargando.value = true
  const resultado = await authFacade.registrar({
    nombreCompleto: nombreCompleto.value,
    email: email.value,
    telefono: telefono.value || null,
    password: password.value
  })
  cargando.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cuenta creada', detail: resultado.mensaje, life: 3000 })
    router.push({ name: 'login', query: { registrado: '1' } })
  } else {
    errores.value = resultado.detalles
    toast.add({ severity: 'error', summary: 'No se pudo crear la cuenta', detail: resultado.mensaje, life: 4000 })
  }
}
</script>

<template>
  <AuthLayout
    titulo="Crear cuenta"
    subtitulo="Completa tus datos para registrarte."
    titulo-panel="Únete al equipo del taller."
    descripcion-panel="Crea tu cuenta en un minuto y empieza a trabajar con clientes, vehículos y órdenes de servicio."
    :caracteristicas="[
      { icono: 'pi-shield', texto: 'Contraseñas cifradas' },
      { icono: 'pi-verified', texto: 'Permisos según tu rol' },
      { icono: 'pi-key', texto: 'Recuperación de cuenta' }
    ]"
  >
    <form class="space-y-5" @submit.prevent="enviar">
      <div>
        <label class="etiqueta-campo" for="nombre">Nombre completo</label>
        <div class="relative">
          <i class="pi pi-user pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-primary/70"></i>
          <input id="nombre" v-model="nombreCompleto" type="text" required
                 class="campo-tallerpro pl-10" placeholder="Tu nombre y apellidos" />
        </div>
      </div>

      <div>
        <label class="etiqueta-campo" for="email">Correo electrónico</label>
        <div class="relative">
          <i class="pi pi-envelope pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-primary/70"></i>
          <input id="email" v-model="email" type="email" required autocomplete="email"
                 class="campo-tallerpro pl-10" placeholder="tucorreo@taller.com" />
        </div>
      </div>

      <div>
        <label class="etiqueta-campo" for="telefono">Teléfono (opcional)</label>
        <div class="relative">
          <i class="pi pi-phone pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-primary/70"></i>
          <input id="telefono" v-model="telefono" type="tel"
                 class="campo-tallerpro pl-10" placeholder="55 1234 5678" />
        </div>
      </div>

      <div>
        <label class="etiqueta-campo" for="password">Contraseña</label>
        <div class="relative">
          <i class="pi pi-lock pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-primary/70"></i>
          <input id="password" v-model="password" :type="mostrarPassword ? 'text' : 'password'" required
                 autocomplete="new-password" class="campo-tallerpro pl-10 pr-11" placeholder="Crea una contraseña" />
          <button
            type="button"
            class="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-ink/40 hover:text-ink/70"
            :aria-label="mostrarPassword ? 'Ocultar contrasena' : 'Mostrar contrasena'"
            @click="mostrarPassword = !mostrarPassword"
          >
            <i class="pi" :class="mostrarPassword ? 'pi-eye-slash' : 'pi-eye'"></i>
          </button>
        </div>

        <div class="mt-2 flex gap-1.5">
          <span class="h-1.5 flex-1 rounded-full" :class="fuerzaPassword >= 1 ? 'bg-deep' : 'bg-mist'"></span>
          <span class="h-1.5 flex-1 rounded-full" :class="fuerzaPassword >= 2 ? 'bg-primary' : 'bg-mist'"></span>
          <span class="h-1.5 flex-1 rounded-full" :class="fuerzaPassword >= 3 ? 'bg-accent' : 'bg-mist'"></span>
          <span class="h-1.5 flex-1 rounded-full bg-mist"></span>
        </div>
        <p class="mt-1.5 text-xs text-ink/50">Usa mínimo 8 caracteres, con mayúscula, minúscula y un número.</p>
      </div>

      <div v-if="errores.length" class="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        <ul class="list-disc pl-4">
          <li v-for="(detalle, i) in errores" :key="i">{{ detalle }}</li>
        </ul>
      </div>

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Creando cuenta...' : 'Crear cuenta' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      ¿Ya tienes cuenta?
      <RouterLink :to="{ name: 'login' }" class="font-medium text-primary hover:text-primary-hover">Iniciar sesión</RouterLink>
    </p>
  </AuthLayout>
</template>
