import http from 'k6/http';
import { check, group, sleep } from 'k6';

const API = 'https://taller-mecanico-back-production.up.railway.app/api';

// Escenarios: se elige con -e ESCENARIO=humo|carga|estres|picos
const ESCENARIOS = {
  humo:   [{ duration: '30s', target: 1 }],
  carga:  [{ duration: '1m', target: 20 }, { duration: '3m', target: 20 }, { duration: '1m', target: 0 }],
  estres: [{ duration: '1m', target: 20 }, { duration: '2m', target: 50 }, { duration: '2m', target: 100 },
           { duration: '2m', target: 150 }, { duration: '1m', target: 0 }],
  picos:  [{ duration: '30s', target: 5 }, { duration: '10s', target: 150 }, { duration: '1m', target: 150 },
           { duration: '10s', target: 5 }, { duration: '1m', target: 5 }],
};

export const options = {
  stages: ESCENARIOS[__ENV.ESCENARIO || 'humo'],
  thresholds: {
    http_req_failed: ['rate<0.01'],      // menos de 1% de errores
    http_req_duration: ['p(95)<800'],    // 95% de respuestas en menos de 800 ms
  },
};

// Inicia sesión UNA sola vez y comparte el token con todos los usuarios virtuales
export function setup() {
  const res = http.post(`${API}/auth/login`,
    JSON.stringify({ email: __ENV.EMAIL, password: __ENV.PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } });
  if (res.status !== 200) {
    throw new Error(`Login falló con status ${res.status}: ${res.body}`);
  }
  return { token: res.json('token') };
}

// Lo que hace cada usuario virtual: simula a un recepcionista navegando la app
export default function (data) {
  const headers = { headers: { Authorization: `Bearer ${data.token}` } };

  group('Panel', () => {
    check(http.get(`${API}/panel/resumen`, headers), { 'panel 200': (r) => r.status === 200 });
  });
  sleep(1);

  group('Clientes', () => {
    check(http.get(`${API}/clientes?pagina=0&porPagina=10`, headers), { 'lista clientes 200': (r) => r.status === 200 });
    check(http.get(`${API}/clientes/resumen`, headers), { 'resumen clientes 200': (r) => r.status === 200 });
  });
  sleep(2);

  group('Vehiculos', () => {
    check(http.get(`${API}/vehiculos`, headers), { 'lista vehiculos 200': (r) => r.status === 200 });
  });
  sleep(2);
}
