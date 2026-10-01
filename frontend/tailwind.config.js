/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        // Paleta "blueprint": todo en tonos de azul, del mas oscuro (ink) al
        // mas claro (paper). Valores tomados con muestreo de pixeles exacto
        // de las imagenes de referencia (sistema-gestion-talleres), no a ojo.
        ink: '#0B1E36',
        deep: '#0A1F47',
        primary: {
          DEFAULT: '#1D4ED8',
          hover: '#1E40AF',
          light: '#3B82F6'
        },
        accent: '#38BDF8',
        mist: '#DBEAFE',
        paper: '#EFF6FF'
      },
      fontFamily: {
        display: ['"Plus Jakarta Sans"', 'sans-serif'],
        body: ['"IBM Plex Sans"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace']
      }
    }
  },
  plugins: []
}
