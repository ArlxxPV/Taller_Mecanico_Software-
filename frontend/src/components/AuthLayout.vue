<script setup>
defineProps({
  titulo: { type: String, required: true },
  subtitulo: { type: String, default: '' }
})
</script>

<template>
  <div class="min-h-screen grid lg:grid-cols-2 bg-paper">
    <!-- Panel izquierdo: identidad de marca, estilo "plano tecnico" -->
    <div class="relative hidden lg:flex flex-col justify-between overflow-hidden bg-deep px-12 py-10 text-white">
      <div class="fondo-plano absolute inset-0"></div>

      <div class="relative z-10">
        <span class="font-display text-xl font-semibold tracking-tight">TallerPro</span>
      </div>

      <div class="relative z-10 max-w-md">
        <!-- Ilustracion de linea, en el mismo espiritu de un plano mecanico -->
        <svg viewBox="0 0 320 180" class="mb-10 w-full max-w-sm text-accent" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M35 120 L55 85 Q65 70 85 70 L210 70 Q228 70 238 85 L258 120"
                stroke-linecap="round" stroke-linejoin="round" />
          <path d="M20 120 H280" stroke-linecap="round" />
          <circle cx="90" cy="120" r="20" />
          <circle cx="230" cy="120" r="20" />
          <path d="M90 120 h140" stroke-opacity="0.4" />
          <path d="M150 55 l14 -22 h20 l-8 22" stroke-linecap="round" stroke-linejoin="round" opacity="0.85" />
        </svg>

        <h1 class="font-display text-3xl font-semibold leading-tight">
          El taller, ordenado de principio a fin.
        </h1>
        <p class="mt-4 text-white/70">
          Clientes, vehiculos y el historial de cada reparacion en un solo lugar,
          con acceso claro segun el rol de cada persona del equipo.
        </p>
      </div>

      <p class="relative z-10 text-sm text-white/40">Modulo 1 · Identidad y acceso</p>
    </div>

    <!-- Panel derecho: formulario -->
    <div class="flex items-center justify-center px-6 py-12 sm:px-10">
      <div class="w-full max-w-sm">
        <div class="mb-8 lg:hidden">
          <span class="font-display text-xl font-semibold text-ink">TallerPro</span>
        </div>

        <h2 class="font-display text-2xl font-semibold text-ink">{{ titulo }}</h2>
        <p v-if="subtitulo" class="mt-1.5 text-sm text-ink/60">{{ subtitulo }}</p>

        <div class="mt-8">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>
