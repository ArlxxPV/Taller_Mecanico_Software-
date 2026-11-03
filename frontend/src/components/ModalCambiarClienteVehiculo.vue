<script setup>
import { computed, ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import ModalBase from './ModalBase.vue'
import { vehiculoFacade } from '../facades/vehiculoFacade'
import { clienteFacade, validarMotivo } from '../facades/clienteFacade'

const props = defineProps({ vehiculo: { type: Object, required: true } })
const emit = defineEmits(['cerrar', 'guardado'])

const toast = useToast()

const busqueda = ref('')
const resultados = ref([])
const buscando = ref(false)
const clienteElegido = ref(null)
const motivo = ref('')
const guardando = ref(false)
const mensajeError = ref('')

const errorMotivo = computed(() => (motivo.value ? validarMotivo(motivo.value) : ''))
const formularioValido = computed(() => !!clienteElegido.value && validarMotivo(motivo.value) === '')

let temporizador = null
watch(busqueda, (valor) => {
  clearTimeout(temporizador)
  if (!valor || valor.trim().length < 2) {
    resultados.value = []
    return
  }
  temporizador = setTimeout(async () => {
    buscando.value = true
    const resultado = await clienteFacade.listar({ q: valor.trim(), porPagina: 10 })
    buscando.value = false
    if (resultado.ok) {
      resultados.value = resultado.pagina.clientes.filter((c) => c.id !== props.vehiculo.clienteId)
    }
  }, 400)
})

function elegirCliente(cliente) {
  clienteElegido.value = cliente
  resultados.value = []
  busqueda.value = ''
}

async function guardar() {
  if (!formularioValido.value) return
  mensajeError.value = ''
  guardando.value = true
  const resultado = await vehiculoFacade.cambiarCliente(props.vehiculo.id, clienteElegido.value.id, motivo.value)
  guardando.value = false

  if (resultado.ok) {
    toast.add({ severity: 'success', summary: 'Cliente reasignado', detail: resultado.mensaje, life: 4000 })
    emit('guardado', resultado.vehiculo)
  } else {
    mensajeError.value = resultado.mensaje
  }
}
</script>

<template>
  <ModalBase titulo="Cambiar cliente del vehículo" ancho="confirmacion" @cerrar="$emit('cerrar')">
    <template #subtitulo>
      <span>{{ vehiculo.marca }} {{ vehiculo.modelo }} {{ vehiculo.anio }} · {{ vehiculo.placa }}</span>
    </template>

    <div v-if="mensajeError" class="caja-error-tp mb-5">
      <p class="font-semibold">{{ mensajeError }}</p>
    </div>

    <p class="caja-info-tp mb-5">
      Cliente actual: <strong>{{ vehiculo.clienteNombre }}</strong>. Al reasignar, el vehículo quedará SUSPENDIDO.
    </p>

    <div v-if="!clienteElegido">
      <label class="etiqueta-tp" for="buscarCliente">Nuevo cliente</label>
      <input id="buscarCliente" v-model="busqueda" type="text" class="campo-tp" placeholder="Busca por nombre..." />
      <p v-if="buscando" class="ayuda-tp">Buscando...</p>
      <ul v-else-if="resultados.length" class="mt-2 max-h-48 overflow-y-auto rounded-[10px] border border-mist">
        <li v-for="c in resultados" :key="c.id">
          <button type="button" class="block w-full px-3.5 py-2.5 text-left text-sm text-ink hover:bg-paper"
                  @click="elegirCliente(c)">
            {{ c.nombreCompleto }} <span class="text-secondary">· {{ c.email }}</span>
          </button>
        </li>
      </ul>
      <p v-else-if="busqueda.trim().length >= 2" class="ayuda-tp">Sin resultados.</p>
    </div>

    <div v-else class="flex items-center justify-between rounded-[10px] border border-mist bg-paper px-3.5 py-2.5">
      <span class="text-sm font-medium text-ink">{{ clienteElegido.nombreCompleto }}</span>
      <button type="button" class="boton-tp-texto text-primary" @click="clienteElegido = null">Cambiar</button>
    </div>

    <div class="mt-4">
      <label class="etiqueta-tp" for="motivo">Motivo</label>
      <textarea id="motivo" v-model="motivo" rows="3" class="campo-tp !h-auto py-2.5"
                placeholder="Ej. Venta del vehiculo al nuevo propietario"></textarea>
      <p v-if="motivo && errorMotivo" class="error-tp">{{ errorMotivo }}</p>
    </div>

    <template #footer>
      <button type="button" class="boton-tp-secundario" @click="$emit('cerrar')">Cancelar</button>
      <button type="button" class="boton-tp-primario" :disabled="!formularioValido || guardando" @click="guardar">
        {{ guardando ? 'Guardando...' : 'Cambiar cliente' }}
      </button>
    </template>
  </ModalBase>
</template>
