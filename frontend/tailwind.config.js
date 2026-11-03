/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        // Paleta fase 2 (maquetas aprobadas, oct 2026): valores tomados tal
        // cual del documento de diseno. "deep" y "accent" se conservan solo
        // para el panel de autenticacion (login/registro), que mantiene su
        // diseno actual a proposito.
        ink: '#0F172A',
        secondary: '#5B6B82',
        deep: '#0A1F47',
        sidebar: '#16233B',
        primary: {
          DEFAULT: '#1D4ED8',
          hover: '#1E3A8A',
          light: '#3B82F6'
        },
        accent: '#38BDF8',
        accentStrong: '#2563EB',
        mist: '#DCE4F0',
        borderInput: '#C7D2E1',
        divider: '#EEF2F8',
        paper: '#EEF3FB',
        success: '#15803D',
        danger: '#B91C1C'
      },
      fontFamily: {
        display: ['"Plus Jakarta Sans"', 'sans-serif'],
        body: ['"Plus Jakarta Sans"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace']
      }
    }
  },
  plugins: []
}
