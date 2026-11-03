<script setup>
defineProps({
  src: { type: String, required: true },
  titulo: { type: String, default: '' }
})
defineEmits(['cerrar'])
</script>

<template>
  <div class="modal-fondo-tp" @click.self="$emit('cerrar')">
    <div class="relative max-h-[90vh] max-w-[90vw]">
      <button type="button"
              class="absolute -right-3 -top-3 flex h-10 w-10 items-center justify-center rounded-full bg-white text-ink/70 shadow-lg hover:text-ink"
              aria-label="Cerrar" @click="$emit('cerrar')">
        <i class="pi pi-times"></i>
      </button>
      <img :src="src" :alt="titulo" class="max-h-[90vh] max-w-[90vw] rounded-2xl object-contain shadow-[0_20px_40px_rgba(15,23,42,0.35)]" />
      <p v-if="titulo" class="mt-3 text-center text-sm font-medium text-white">{{ titulo }}</p>
    </div>
  </div>
</template>
