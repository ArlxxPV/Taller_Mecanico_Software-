<script setup>
// Modal persistente (no se cierra con clic afuera, ver DISEÑO VISUAL de la
// maqueta): el fondo oscuro no tiene @click de cierre a proposito. Solo se
// cierra con el boton "x" o con los botones del footer (que decide quien usa
// este componente, via el slot "footer").
defineProps({
  titulo: { type: String, required: true },
  subtitulo: { type: String, default: '' },
  ancho: { type: String, default: 'formulario' } // 'formulario' (680px) | 'confirmacion' (445px)
})
defineEmits(['cerrar'])
</script>

<template>
  <div class="modal-fondo-tp">
    <div class="modal-caja-tp" :class="ancho === 'confirmacion' ? 'max-w-[445px]' : 'max-w-[680px]'">
      <div class="flex items-start justify-between gap-4 border-b border-divider px-7 py-6">
        <div class="min-w-0">
          <h2 class="font-display text-xl font-bold text-ink">{{ titulo }}</h2>
          <div v-if="subtitulo || $slots.subtitulo" class="mt-1 text-sm text-secondary">
            <slot name="subtitulo">{{ subtitulo }}</slot>
          </div>
        </div>
        <button type="button" class="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-[#F1F5F9] text-ink/60 hover:text-ink"
                aria-label="Cerrar" @click="$emit('cerrar')">
          <i class="pi pi-times"></i>
        </button>
      </div>

      <div class="overflow-y-auto px-7 py-6">
        <slot />
      </div>

      <div v-if="$slots.footer" class="flex items-center justify-between gap-3 rounded-b-2xl border-t border-divider bg-[#F8FAFC] px-7 py-5">
        <slot name="footer-izquierda" />
        <div class="ml-auto flex gap-3">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </div>
</template>
