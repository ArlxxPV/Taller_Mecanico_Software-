/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        // Paleta "blueprint": todo en tonos de azul, del mas oscuro (ink) al
        // mas claro (paper). Elegida a proposito para que combine con el
        // rubro del taller: los planos tecnicos/mecanicos son azules.
        ink: '#0B1E36',
        deep: '#123A6B',
        primary: {
          DEFAULT: '#2A5CDB',
          hover: '#2450BE',
          light: '#5B86EA'
        },
        accent: '#3FA9F5',
        mist: '#DCEBFB',
        paper: '#F5F9FF'
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'sans-serif'],
        body: ['"IBM Plex Sans"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace']
      }
    }
  },
  plugins: []
}
