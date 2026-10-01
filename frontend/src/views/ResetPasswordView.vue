<script setup>
import { ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { authFacade } from '../facades/authFacade'
import AuthLayout from '../components/AuthLayout.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const token = ref(route.query.token || '')
const nuevaPassword = ref('')
const confirmarPassword = ref('')
const cargando = ref(false)
const listo = ref(false)

async function enviar() {
  if (nuevaPassword.value !== confirmarPassword.value) {
    toast.add({ severity: 'warn', summary: 'Revisa la contrasena', detail: 'Las dos contrasenas no coinciden.', life: 4000 })
    return
  }
  if (!token.value) {
    toast.add({ severity: 'warn', summary: 'Falta el token', detail: 'Copia el enlace completo que recibiste por correo.', life: 4000 })
    return
  }

  cargando.value = true
  const resultado = await authFacade.restablecerPassword(token.value, nuevaPassword.value)
  cargando.value = false

  if (resultado.ok) {
    listo.value = true
    toast.add({ severity: 'success', summary: 'Contrasena actualizada', detail: resultado.mensaje, life: 3000 })
    setTimeout(() => router.push({ name: 'login' }), 2500)
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo actualizar', detail: resultado.mensaje, life: 4000 })
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

      <button type="submit" class="boton-primario" :disabled="cargando">
        {{ cargando ? 'Guardando...' : 'Guardar nueva contrasena' }}
      </button>
    </form>

    <p class="mt-6 text-center text-sm text-ink/60">
      <RouterLink :to="{ name: 'login' }" class="font-medium text-primary hover:text-primary-hover">Volver a inicio de sesion</RouterLink>
    </p>
  </AuthLayout>
</template>
