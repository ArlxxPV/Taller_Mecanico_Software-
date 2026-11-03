<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { estatusFacade, normalizarValor, validarEstatus } from '../facades/estatusFacade'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const esEdicion = computed(() => !!route.params.id)

const form = reactive({ valor: '', descripcion: '' })
const estatusOriginal = ref(null)
const cargando = ref(esEdicion.value)
const guardando = ref(false)
const errores = ref({})

const valorNormalizado = computed(() => normalizarValor(form.valor))
const valorBloqueado = computed(() => esEdicion.value && estatusOriginal.value?.esSistema)

function formatearFecha(fechaIso) {
  if (!fechaIso) return '—'
  return new Date(fechaIso).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' })
}

async function cargar() {
  if (!esEdicion.value) return
  cargando.value = true
  const resultado = await estatusFacade.obtener(route.params.id)
  cargando.value = false

  if (!resultado.ok) {
    toast.add({ severity: 'error', summary: 'No se pudo abrir el estatus', detail: resultado.mensaje, life: 4500 })
    router.push({ name: 'catalogos-estatus' })
    return
  }
  estatusOriginal.value = resultado.estatus
  form.valor = resultado.estatus.valor
  form.descripcion = resultado.estatus.descripcion
}
watch(() => route.params.id, cargar, { immediate: true })

function validar() {
  errores.value = validarEstatus(form.valor, form.descripcion)
  return Object.keys(errores.value).length === 0
}

async function guardar() {
  if (!validar()) return
  guardando.value = true
  const resultado = esEdicion.value
    ? await estatusFacade.actualizar(route.params.id, form.valor, form.descripcion)
    : await estatusFacade.crear(form.valor, form.descripcion)
  guardando.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: esEdicion.value ? 'Cambios guardados' : 'Estatus creado', detail: resultado.mensaje, life: 3000 })
    router.push({ name: 'catalogos-estatus' })
  } else {
    toast.add({ severity: 'error', summary: 'No se pudo guardar', detail: resultado.mensaje, life: 4500 })
  }
}
</script>

<template>
  <div class="mx-auto max-w-[680px]">
    <div class="flex items-center gap-2">
      <h1 class="titulo-pagina">{{ esEdicion ? 'Editar estatus' : 'Agregar estatus' }}</h1>
      <span v-if="valorBloqueado" class="pill-tp">Sistema</span>
    </div>
    <p class="subtitulo-pagina">
      {{ valorBloqueado ? 'Los estatus del sistema solo permiten editar la descripción.' : 'El valor se guarda en mayúsculas y no se puede repetir.' }}
    </p>

    <div v-if="cargando" class="mt-10 text-center text-sm text-secondary">Cargando...</div>

    <form v-else class="tarjeta-tp mt-6" @submit.prevent="guardar">
      <div v-if="esEdicion" class="mb-5 grid gap-5 sm:grid-cols-2">
        <div>
          <label class="etiqueta-tp">ID</label>
          <input :value="estatusOriginal?.id" type="text" disabled class="campo-tp" />
        </div>
      </div>

      <div class="mb-5">
        <label class="etiqueta-tp" for="valor">Valor *</label>
        <input id="valor" v-model="form.valor" type="text" maxlength="30" :disabled="valorBloqueado"
               class="campo-tp uppercase" :class="errores.valor && 'campo-tp-error'"
               @blur="form.valor = normalizarValor(form.valor)" />
        <p v-if="valorBloqueado" class="ayuda-tp">Bloqueado: lo usa la lógica del sistema</p>
        <p v-else class="ayuda-tp">Solo letras y guion bajo; los espacios se convierten en _ · {{ valorNormalizado.length }} / 30</p>
        <p v-if="errores.valor" class="error-tp">{{ errores.valor }}</p>
      </div>

      <div class="mb-5">
        <label class="etiqueta-tp" for="descripcion">Descripción *</label>
        <textarea id="descripcion" v-model="form.descripcion" rows="3" maxlength="255"
                  class="campo-tp !h-auto py-2.5" :class="errores.descripcion && 'campo-tp-error'"></textarea>
        <p class="ayuda-tp">De 10 a 255 caracteres · {{ form.descripcion.length }} / 255</p>
        <p v-if="errores.descripcion" class="error-tp">{{ errores.descripcion }}</p>
      </div>

      <div>
        <p class="etiqueta-tp">Vista previa</p>
        <span class="chip-estatus-neutro">{{ valorNormalizado || '—' }}</span>
      </div>

      <p v-if="esEdicion" class="mt-5 text-xs text-secondary">
        En uso por {{ estatusOriginal?.clientesEnUso || 0 }} cliente{{ estatusOriginal?.clientesEnUso === 1 ? '' : 's' }}
        <template v-if="estatusOriginal?.vehiculosEnUso"> y {{ estatusOriginal.vehiculosEnUso }} vehículo{{ estatusOriginal.vehiculosEnUso === 1 ? '' : 's' }}</template>
        · Última actualización: {{ formatearFecha(estatusOriginal?.actualizadoEn) }}
      </p>

      <div class="mt-6 flex gap-3 border-t border-divider pt-5">
        <button type="button" class="boton-tp-secundario" @click="router.push({ name: 'catalogos-estatus' })">Cancelar</button>
        <button type="submit" class="boton-tp-primario" :disabled="guardando">
          {{ guardando ? 'Guardando...' : (esEdicion ? 'Guardar cambios' : 'Guardar estatus') }}
        </button>
      </div>
    </form>
  </div>
</template>
