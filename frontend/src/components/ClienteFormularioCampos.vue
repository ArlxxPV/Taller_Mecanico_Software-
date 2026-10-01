<script setup>
import { computed, ref } from 'vue'
import { useToast } from 'primevue/usetoast'

// `formulario` es el objeto reactivo del padre (Registrar o Editar): se
// muta directo aqui adentro via v-model, no hace falta emitir nada para eso.
// `fotoActualUrl` solo aplica en Editar: la foto que el cliente ya tenia
// guardada, para mostrarla mientras no se elija una nueva.
const props = defineProps({
  formulario: { type: Object, required: true },
  fotoActualUrl: { type: String, default: '' }
})

const fotoArchivo = defineModel('fotoArchivo', { default: null })

const toast = useToast()
const TIPOS_FOTO_PERMITIDOS = ['image/jpeg', 'image/png']
const FOTO_MAX_BYTES = 12 * 1024 * 1024

const fotoPreviewNueva = ref('')
const fotoMostrada = computed(() => fotoPreviewNueva.value || props.fotoActualUrl)

function elegirFoto(evento) {
  const archivo = evento.target.files?.[0]
  if (!archivo) return

  if (!TIPOS_FOTO_PERMITIDOS.includes(archivo.type)) {
    toast.add({ severity: 'warn', summary: 'Formato no permitido', detail: 'La foto debe ser un archivo JPG o PNG.', life: 4000 })
    evento.target.value = ''
    return
  }
  if (archivo.size > FOTO_MAX_BYTES) {
    toast.add({ severity: 'warn', summary: 'Archivo muy pesado', detail: 'La foto no debe superar 12 MB.', life: 4000 })
    evento.target.value = ''
    return
  }

  if (fotoPreviewNueva.value) URL.revokeObjectURL(fotoPreviewNueva.value)
  fotoArchivo.value = archivo
  fotoPreviewNueva.value = URL.createObjectURL(archivo)
}

function quitarFoto() {
  if (fotoPreviewNueva.value) URL.revokeObjectURL(fotoPreviewNueva.value)
  fotoArchivo.value = null
  fotoPreviewNueva.value = ''
}

defineExpose({ quitarFoto })
</script>

<template>
  <!-- Datos personales -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Datos personales</h2>
    <div class="mt-4 grid gap-5 sm:grid-cols-2">
      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="nombreCompleto">Nombre completo</label>
        <input id="nombreCompleto" v-model="formulario.nombreCompleto" type="text" required
               class="campo-tallerpro" placeholder="Ej. Luis Fernando Perez Diaz" />
      </div>

      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="contactoAlternativo">Contacto alternativo (opcional)</label>
        <input id="contactoAlternativo" v-model="formulario.contactoAlternativo" type="text"
               class="campo-tallerpro" placeholder="Nombre y telefono de otra persona de contacto" />
      </div>

      <div>
        <label class="etiqueta-campo" for="edad">Edad</label>
        <input id="edad" v-model="formulario.edad" type="number" min="0" max="120" required
               class="campo-tallerpro" placeholder="Ej. 34" />
      </div>

      <div>
        <label class="etiqueta-campo" for="fechaNacimiento">Fecha de nacimiento</label>
        <input id="fechaNacimiento" v-model="formulario.fechaNacimiento" type="date" required
               class="campo-tallerpro" />
      </div>
    </div>
  </section>

  <!-- Contacto -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Contacto</h2>
    <div class="mt-4 grid gap-5 sm:grid-cols-2">
      <div>
        <label class="etiqueta-campo" for="telefonoPersonal">Telefono personal</label>
        <input id="telefonoPersonal" v-model="formulario.telefonoPersonal" type="tel" required
               class="campo-tallerpro" placeholder="55 1234 5678" />
      </div>

      <div>
        <label class="etiqueta-campo" for="telefonoTrabajo">Telefono de trabajo (opcional)</label>
        <input id="telefonoTrabajo" v-model="formulario.telefonoTrabajo" type="tel"
               class="campo-tallerpro" placeholder="55 8765 4321" />
      </div>

      <div>
        <label class="etiqueta-campo" for="email">Correo</label>
        <input id="email" v-model="formulario.email" type="email" required
               class="campo-tallerpro" placeholder="cliente@correo.com" />
      </div>

      <div>
        <label class="etiqueta-campo" for="emailTrabajo">Correo de trabajo (opcional)</label>
        <input id="emailTrabajo" v-model="formulario.emailTrabajo" type="email"
               class="campo-tallerpro" placeholder="cliente@trabajo.com" />
      </div>
    </div>
  </section>

  <!-- Direccion -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Dirección</h2>
    <div class="mt-4 grid gap-5 sm:grid-cols-2">
      <div class="sm:col-span-2">
        <label class="etiqueta-campo" for="calle">Calle y número</label>
        <input id="calle" v-model="formulario.calle" type="text" required class="campo-tallerpro" />
      </div>
      <div>
        <label class="etiqueta-campo" for="colonia">Colonia</label>
        <input id="colonia" v-model="formulario.colonia" type="text" required class="campo-tallerpro" />
      </div>
      <div>
        <label class="etiqueta-campo" for="municipio">Municipio</label>
        <input id="municipio" v-model="formulario.municipio" type="text" required class="campo-tallerpro" />
      </div>
      <div>
        <label class="etiqueta-campo" for="estado">Estado</label>
        <input id="estado" v-model="formulario.estado" type="text" required class="campo-tallerpro" />
      </div>
      <div>
        <label class="etiqueta-campo" for="codigoPostal">Código postal</label>
        <input id="codigoPostal" v-model="formulario.codigoPostal" type="text" inputmode="numeric" maxlength="5" required
               class="campo-tallerpro" placeholder="Ej. 06600" />
      </div>
    </div>
  </section>

  <!-- Foto -->
  <section class="rounded-2xl border border-mist bg-white p-6">
    <h2 class="font-display text-lg font-semibold text-ink">Foto (opcional)</h2>
    <p class="mt-1 text-xs text-ink/50">Formatos JPG o PNG, máximo 12 MB.</p>

    <div class="mt-4 flex items-center gap-4">
      <img v-if="fotoMostrada" :src="fotoMostrada" alt="Vista previa de la foto del cliente"
           class="h-20 w-20 rounded-full border border-mist object-cover" />
      <div v-else class="flex h-20 w-20 items-center justify-center rounded-full border border-dashed border-mist text-ink/30">
        <i class="pi pi-user text-2xl"></i>
      </div>

      <div class="flex flex-col gap-2">
        <label class="cursor-pointer rounded-lg border border-mist px-4 py-2 text-sm font-medium text-ink hover:bg-mist/60">
          {{ fotoMostrada ? 'Cambiar foto' : 'Elegir foto' }}
          <input type="file" accept="image/png,image/jpeg" class="hidden" @change="elegirFoto" />
        </label>
        <button v-if="fotoArchivo" type="button" class="text-sm text-red-600 hover:underline" @click="quitarFoto">
          Quitar foto nueva
        </button>
      </div>
    </div>
  </section>
</template>
