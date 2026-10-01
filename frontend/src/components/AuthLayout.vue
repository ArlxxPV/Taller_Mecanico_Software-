<script setup>
defineProps({
  titulo: { type: String, required: true },
  subtitulo: { type: String, default: '' },
  tituloPanel: { type: String, default: 'El taller, ordenado de principio a fin.' },
  descripcionPanel: {
    type: String,
    default: 'Clientes, vehiculos y el historial de cada reparacion en un solo lugar, con acceso claro segun el rol de cada persona del equipo.'
  },
  // Si se manda, reemplaza la ilustracion de linea por una lista de
  // caracteristicas con icono: [{ icono: 'pi-shield', texto: '...' }, ...]
  caracteristicas: { type: Array, default: null }
})

// Dientes del engrane decorativo: cuadrados pequeños repartidos en circulo,
// cada uno girado para "apuntar" hacia afuera (como en la marca de agua de
// referencia). Se genera con codigo en vez de escribir 16 <rect> a mano.
const CENTRO = 160
const RADIO_DIENTES = 120
const N_DIENTES = 16
const dientesEngrane = Array.from({ length: N_DIENTES }, (_, i) => {
  const angulo = (360 / N_DIENTES) * i
  const rad = (angulo * Math.PI) / 180
  return {
    x: CENTRO + RADIO_DIENTES * Math.cos(rad),
    y: CENTRO + RADIO_DIENTES * Math.sin(rad),
    rot: angulo + 45,
    op: i % 3 === 0 ? 0.55 : i % 2 === 0 ? 0.35 : 0.22
  }
})
</script>

<template>
  <div class="min-h-screen grid lg:grid-cols-2 bg-paper">
    <!-- Panel izquierdo: identidad de marca, estilo "plano tecnico" -->
    <div class="relative hidden lg:flex flex-col justify-between overflow-hidden bg-gradient-to-br from-deep via-deep to-primary px-12 py-10 text-white">
      <div class="fondo-plano absolute inset-0"></div>

      <!-- Marca de agua: engrane decorativo, estilo "plano tecnico" de un
           taller (guino al oficio, no una decoracion generica). Se repite en
           todas las pantallas de acceso. -->
      <svg viewBox="0 0 320 320" class="pointer-events-none absolute -bottom-10 -right-10 h-[420px] w-[420px] text-white">
        <circle cx="160" cy="160" r="95" fill="none" stroke="currentColor" stroke-opacity="0.18" stroke-width="1.5" />
        <circle cx="160" cy="160" r="28" fill="currentColor" fill-opacity="0.22" />
        <rect
          v-for="(d, i) in dientesEngrane" :key="i"
          x="-7" y="-7" width="14" height="14" rx="2.5"
          fill="currentColor" :fill-opacity="d.op"
          :transform="`translate(${d.x} ${d.y}) rotate(${d.rot})`"
        />
      </svg>
      <!-- Silueta de auto, simbolo del taller mecanico -->
      <i class="pi pi-car pointer-events-none absolute bottom-16 right-16 text-[88px] text-white/10"></i>

      <div class="relative z-10 flex items-center gap-2.5">
        <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-accent text-white">
          <i class="pi pi-wrench text-base"></i>
        </span>
        <span class="font-display text-xl font-bold tracking-tight">TallerPro</span>
      </div>

      <div class="relative z-10 max-w-md">
        <h1 class="font-display text-4xl font-extrabold leading-[1.1]">
          {{ tituloPanel }}
        </h1>
        <p class="mt-4 text-white/70">{{ descripcionPanel }}</p>

        <!-- Lista de caracteristicas (pantallas ya rediseñadas) -->
        <div v-if="caracteristicas" class="mt-8 space-y-3">
          <div v-for="item in caracteristicas" :key="item.texto"
               class="flex items-center gap-3 rounded-xl border border-white/15 bg-white/10 px-4 py-3.5 text-sm font-medium text-white">
            <i class="pi text-lg text-accent" :class="item.icono"></i>
            <span>{{ item.texto }}</span>
          </div>
        </div>
      </div>

      <p class="relative z-10 text-sm text-white/40">Sistema para talleres mecánicos</p>
    </div>

    <!-- Panel derecho: formulario, dentro de una tarjeta flotante -->
    <div class="flex items-center justify-center bg-paper px-6 py-12 sm:px-10">
      <div class="w-full max-w-sm rounded-2xl bg-white p-8 shadow-xl shadow-ink/[0.07] sm:p-10">
        <div class="mb-8 flex items-center gap-2 lg:hidden">
          <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-accent text-white">
            <i class="pi pi-wrench text-sm"></i>
          </span>
          <span class="font-display text-xl font-bold text-ink">TallerPro</span>
        </div>

        <h2 class="font-display text-[28px] font-extrabold text-ink">{{ titulo }}</h2>
        <p v-if="subtitulo" class="mt-1.5 text-sm text-ink/60">{{ subtitulo }}</p>

        <div class="mt-8">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>
