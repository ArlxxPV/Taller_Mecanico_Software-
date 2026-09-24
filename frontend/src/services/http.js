import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL
})

// Adjunta el JWT guardado (si existe) a cada peticion saliente.
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('tallerpro_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Si el backend responde 401, el token ya no sirve: limpiamos la sesion local.
// (La redireccion a /login la hace el guard del router al detectar que no hay usuario.)
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('tallerpro_token')
    }
    return Promise.reject(error)
  }
)

export default http
