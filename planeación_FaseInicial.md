# TallerPro — Planeación Fase Inicial

> Documento generado a partir de la revisión del código y del `README.md` del proyecto.
> Todo lo que aquí aparece fue verificado en los archivos reales; donde no encontré información, lo indico en vez de inventarla.

---

## 1. Resumen de la primera fase

Esta primera fase (el propio proyecto la llama **"Módulo 1: identidad, usuarios y roles"**) construyó la base de **acceso y seguridad** del sistema. En concreto se desarrolló:

**Backend (API, Spring Boot + MySQL)**
- Registro de nuevos usuarios (siempre con rol `CLIENTE`).
- Inicio de sesión con generación de un **JWT** (token de sesión firmado, sin guardar sesiones en el servidor — "sin estado" o *stateless*).
- Bloqueo automático de cuenta tras 5 intentos fallidos de contraseña (15 minutos de bloqueo).
- Bitácora (registro histórico) de cada intento de inicio de sesión, exitoso o fallido.
- Flujo de "olvidé mi contraseña": genera un enlace con token de un solo uso y lo "envía" (por ahora solo se escribe en el log del backend, ver sección 3).
- Endpoint para consultar el perfil del usuario que inició sesión.
- Creación automática de un usuario administrador al levantar el backend por primera vez.
- Esquema de base de datos con 7 tablas, versionado con **Flyway** (herramienta que aplica los cambios de base de datos automáticamente y en orden, como un "control de versiones" para la base de datos).

**Frontend (interfaz visual, Vue 3)**
- Pantalla de inicio de sesión.
- Pantalla de registro de cuenta nueva.
- Pantalla de "olvidé mi contraseña" (pide el correo).
- Pantalla de restablecer contraseña (recibe el token desde el enlace).
- Panel principal (*dashboard*) que muestra el nombre, rol y último acceso del usuario, con un menú de navegación donde solo la sección "Panel" está activa; las demás (Clientes, Vehículos, Órdenes, Inventario) son botones visuales sin funcionalidad todavía.
- Manejo de sesión en el navegador (guardar/borrar el token, proteger rutas que requieren estar autenticado).

**Conexiones**
- El frontend (Vite, puerto `5173`) se comunica con el backend (Spring Boot, puerto `8081`) mediante peticiones HTTP con Axios, usando el token JWT en cada petición.
- El backend se conecta a una base de datos **MySQL** que corre en un contenedor Docker llamado `tallerpro-mysql`.

---

## 2. Datos del proyecto

| Campo | Detalle |
|---|---|
| **Nombre** | TallerPro |
| **Objetivo** | Sistema de gestión de órdenes de reparación para talleres mecánicos (según `README.md`) |
| **Alcance de esta entrega** | Solo el Módulo 1: identidad, usuarios y roles |
| **Backend** | Java 25 (LTS) + Spring Boot 4.1 + Spring Security 7 + Spring Data JPA + Flyway + JWT (librería `jjwt`) + Argon2 (hash de contraseñas) + MySQL |
| **Frontend** | Vue 3 + Vite + Vue Router + Pinia (estado global) + Axios + Tailwind CSS + PrimeVue (componentes visuales) |
| **Base de datos** | MySQL 8.4, corriendo en un contenedor Docker (`tallerpro-mysql`) |

> ✅ Resuelto: el `README.md` decía que el backend queda en `http://localhost:8080`, pero `application.yml` tiene configurado `server.port: 8081`. Ya se corrigió el `README.md` para que diga `8081` en todos los lugares donde aparecía.

### Módulos que lo componen
1. **Identidad / autenticación** (completo en esta fase): registro, login, recuperación de contraseña, roles, bloqueo por intentos fallidos, bitácora de accesos.
2. **Clientes y vehículos** (parcial): existen las tablas y las clases del modelo de datos, pero no hay pantallas ni endpoints para gestionarlos todavía (ver sección 3).
3. **Órdenes de reparación** e **inventario**: no se encontró ningún archivo relacionado; solo aparecen como botones de menú sin funcionalidad en el frontend.

### Estructura de carpetas principal
```
tallerpro/
├── backend/                          # Spring Boot (Java 25)
│   ├── pom.xml                       # Dependencias y configuración de compilación (Maven)
│   └── src/main/
│       ├── java/com/tallerpro/backend/
│       │   ├── BackendApplication.java   # Punto de entrada de la app
│       │   ├── config/                   # Seguridad, CORS, propiedades, datos iniciales
│       │   ├── security/                 # JWT: emisión, validación, filtro de peticiones
│       │   ├── model/                    # Entidades JPA (tablas de la base de datos)
│       │   ├── repository/               # Acceso a datos (Spring Data JPA)
│       │   ├── dto/                      # Datos de entrada/salida de la API
│       │   ├── service/                  # Lógica de negocio
│       │   ├── controller/               # Endpoints REST
│       │   └── exception/                # Manejo centralizado de errores
│       └── resources/
│           ├── application.yml           # Configuración (conexión a BD, JWT, etc.)
│           └── db/migration/             # Scripts Flyway (V1, V2)
│
├── frontend/                         # Vue 3 + Vite + Tailwind + PrimeVue
│   └── src/
│       ├── main.js / App.vue         # Arranque de la aplicación
│       ├── router/index.js           # Rutas y protección de acceso
│       ├── stores/auth.js            # Estado global de sesión (Pinia)
│       ├── services/                 # Conexión con la API (Axios)
│       ├── components/AuthLayout.vue # Layout compartido de las pantallas de acceso
│       └── views/                    # Pantallas (Login, Registro, Recuperación, Panel)
│
└── README.md
```

---

## 3. Estado de los módulos

| Módulo | Descripción | Estado |
|---|---|---|
| Registro de usuarios | Crea una cuenta nueva con rol `CLIENTE`; valida formato de correo y fuerza de contraseña | ✅ Terminado |
| Inicio de sesión (login) | Verifica credenciales, entrega un token JWT válido por 30 minutos | ✅ Terminado |
| Bloqueo por intentos fallidos | Bloquea la cuenta 15 minutos tras 5 intentos fallidos | ✅ Terminado |
| Bitácora de accesos | Registra cada intento de login (éxito o fallo, con IP y fecha) | ✅ Terminado |
| Recuperación de contraseña | Genera un enlace con token de un solo uso, válido 30 minutos | ✅ Terminado *(ver nota abajo)* |
| Envío real de correos | Enviar el enlace de recuperación por correo real (SMTP) | ⚠️ Pendiente — hoy el enlace solo se escribe en el log del backend (`ConsoleMailService`), no se envía ningún correo real |
| Perfil del usuario autenticado | Endpoint y pantalla que muestran nombre, rol y último acceso | ✅ Terminado |
| Gestión de clientes | Tabla `clientes` y clase `Cliente` ya existen en el backend | ⚠️ Pendiente — no hay endpoints (`controller`) ni pantallas para crear/editar/listar clientes |
| Gestión de vehículos | Tabla `vehiculos` y clase `Vehiculo` ya existen en el backend | ⚠️ Pendiente — no hay endpoints ni pantallas |
| Panel administrativo de roles | Asignar el rol `EMPLEADO` a un usuario desde una pantalla | ⚠️ Pendiente (mencionado como trabajo futuro en el `README.md`) |
| Órdenes de reparación | Aparece como botón de menú en el panel | ⚠️ Pendiente — no se encontró ningún archivo de backend relacionado |
| Inventario | Aparece como botón de menú en el panel | ⚠️ Pendiente — no se encontró ningún archivo de backend relacionado |
| Cierre de sesión remoto / refresh token | Invalidar tokens antes de que expiren, o renovarlos sin volver a iniciar sesión | ⚠️ Pendiente (mencionado como trabajo futuro en el `README.md`) |

---

## 4. Documentación del código

> No se modificó ningún archivo de código ni se cambió lógica alguna. Esta sección documenta lo que ya existe.

### 4.1 Backend

#### Arranque y configuración (`config/`)
| Archivo | Qué hace |
|---|---|
| `BackendApplication.java` | Punto de entrada de la aplicación Spring Boot. |
| `SecurityConfig.java` | Define qué rutas de la API son públicas (`/api/auth/**`) y cuáles requieren estar autenticado o tener rol `ADMIN`/`EMPLEADO` (`/api/usuarios/**`). También configura CORS (qué dominios pueden llamar a la API — hoy solo `http://localhost:5173`), desactiva CSRF (protección que no aplica porque la API no usa cookies de sesión) y define que las contraseñas se cifran con **Argon2**. |
| `JwtProperties.java`, `SeguridadProperties.java`, `ResetPasswordProperties.java` | Leen valores desde `application.yml` (clave JWT, minutos de expiración, intentos máximos, etc.) y los exponen como objetos Java listos para usar en el resto del código. |
| `DataSeeder.java` | Al arrancar el backend, si no existe ningún usuario `admin@tallerpro.mx`, lo crea automáticamente con una contraseña inicial fija escrita en el código (decisión consciente: se dejó así, sin generarla al azar; ver sección 5). |

#### Seguridad (`security/`)
| Archivo | Qué hace |
|---|---|
| `JwtService.java` | Genera el token JWT al iniciar sesión (incluye el correo y los roles del usuario) y lo valida en cada petición posterior. |
| `JwtAuthenticationFilter.java` | Se ejecuta en cada petición HTTP: si trae un token válido en el encabezado `Authorization: Bearer ...`, marca al usuario como autenticado para esa petición. |
| `AppUserDetailsService.java` | Busca al usuario por correo en la base de datos y arma el objeto que Spring Security necesita para saber sus roles y si la cuenta está activa o bloqueada. |

#### Endpoints de la API (`controller/`)
| Archivo | Ruta base | Qué expone |
|---|---|---|
| `AuthController.java` | `/api/auth` | `POST /registro`, `POST /login`, `POST /olvide-password`, `POST /restablecer-password`. Todas son públicas. |
| `UsuarioController.java` | `/api/usuarios` | `GET /yo`: devuelve los datos del usuario que envió el token (requiere estar autenticado). |

#### Lógica de negocio (`service/`)
| Archivo | Qué hace |
|---|---|
| `AuthService.java` | Contiene la lógica de registro y de login: valida credenciales, controla el bloqueo por intentos fallidos, guarda la bitácora y genera el token al final de un login exitoso. |
| `PasswordResetService.java` | Genera el token de recuperación (lo guarda **hasheado**, nunca en texto plano), arma el enlace y llama a `MailService` para "enviarlo"; también valida y aplica el cambio de contraseña cuando el usuario usa el enlace. |
| `MailService.java` (interfaz) y `ConsoleMailService.java` | Definen cómo se envía el correo de recuperación. Hoy solo existe una implementación que **escribe el enlace en el log** del backend, porque no se configuraron credenciales SMTP reales. Cambiar esto a un envío real no requiere tocar el resto del código: solo se crea una nueva clase que implemente `MailService`. |

#### Datos y validación (`model/`, `dto/`)
| Archivo | Qué hace |
|---|---|
| `Usuario.java` | Representa la tabla `usuarios`: credenciales, estado (activo/bloqueado/inactivo), intentos fallidos, último acceso, y sus roles. |
| `Rol.java` / `NombreRol.java` | Representan la tabla `roles` y los 3 roles posibles: `ADMIN`, `EMPLEADO`, `CLIENTE`. |
| `Cliente.java` | Datos de negocio del cliente (dirección, documento), ligados 1 a 1 con un `Usuario`. |
| `Vehiculo.java` | Vehículo de un cliente (marca, modelo, placa, etc.). |
| `LoginAudit.java` | Un registro por cada intento de inicio de sesión. |
| `PasswordResetToken.java` | Un token de recuperación de contraseña (guarda el hash, la fecha de expiración y si ya se usó). |
| `RegistroRequest.java`, `LoginRequest.java`, `OlvidePasswordRequest.java`, `RestablecerPasswordRequest.java` | Definen y validan los datos que llegan desde el frontend (por ejemplo, la contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número). |
| `UsuarioResponse.java`, `AuthResponse.java`, `ApiErrorResponse.java` | Definen los datos que la API devuelve al frontend (nunca incluyen la contraseña ni su hash). |

#### Manejo de errores (`exception/`)
| Archivo | Qué hace |
|---|---|
| `GlobalExceptionHandler.java` | Centraliza cómo se convierten los errores (credenciales inválidas, datos de formulario inválidos, errores inesperados) en respuestas HTTP con un formato consistente (`ApiErrorResponse`). |

#### Base de datos (`resources/db/migration/`)
| Archivo | Qué hace |
|---|---|
| `V1__crear_esquema_identidad.sql` | Crea las 7 tablas del módulo (`roles`, `usuarios`, `usuario_roles`, `password_reset_tokens`, `login_audit`, `clientes`, `vehiculos`), con sus llaves foráneas e índices. |
| `V2__datos_iniciales_roles.sql` | Inserta los 3 roles base (`ADMIN`, `EMPLEADO`, `CLIENTE`). |

Estos scripts los ejecuta **Flyway** automáticamente al arrancar el backend; no hay que correr SQL a mano.

### 4.2 Frontend

| Archivo | Qué hace |
|---|---|
| `main.js` | Arranca la aplicación Vue, registra Pinia (estado global), el router y PrimeVue (con un tema azul personalizado). |
| `App.vue` | Componente raíz: muestra las notificaciones tipo *toast* y el contenido de la ruta actual. |
| `router/index.js` | Define las 5 pantallas y sus rutas. Antes de entrar a cada una, revisa si requiere estar autenticado (`/panel`) o si es solo para invitados (login, registro, etc. — si ya hay sesión, redirige al panel). |
| `stores/auth.js` | Guarda el token y los datos del usuario en memoria y en `localStorage` (para no perder la sesión al recargar la página). Expone acciones: `login`, `registrar`, `cargarPerfil`, `cerrarSesion`. |
| `services/http.js` | Configura Axios: agrega el token guardado a cada petición saliente, y si el backend responde 401 (no autorizado), borra el token local. |
| `services/authService.js` | Junta las llamadas HTTP a los endpoints de autenticación (`/auth/...`, `/usuarios/yo`). |
| `components/AuthLayout.vue` | Diseño compartido (dos paneles: marca a la izquierda, formulario a la derecha) que usan todas las pantallas de acceso. |
| `views/LoginView.vue` | Formulario de inicio de sesión. |
| `views/RegisterView.vue` | Formulario de registro (nombre, correo, teléfono opcional, contraseña). |
| `views/ForgotPasswordView.vue` | Pide el correo para iniciar la recuperación de contraseña. |
| `views/ResetPasswordView.vue` | Toma el token desde el enlace (parámetro de la URL) y permite definir una nueva contraseña. |
| `views/DashboardView.vue` | Panel principal: muestra nombre, rol y último acceso del usuario. Incluye un menú con 5 opciones, de las cuales solo "Panel" está activa; las demás son visuales, sin pantalla ni funcionalidad todavía. |

---

## 5. Credenciales de la base de datos

**No se incluye ningún valor real de usuario, contraseña o clave en este documento**, tal como se pidió. Solo la ubicación y los nombres de las variables.

### ⚠️ Hallazgo original (ya corregido, se documenta para que quede el registro)

Al revisar el proyecto encontré que `backend/src/main/resources/application.yml` y `frontend/.env` tenían las credenciales reales escritas en texto plano, que ninguno de los tres `.gitignore` del proyecto los excluía, y que **ambos ya estaban subidos a GitHub** — en un repositorio **público**. Es decir, la contraseña de MySQL, la clave con la que se firman los tokens de sesión (JWT) y la contraseña inicial del usuario admin (`Admin123!`, escrita también en el `README.md`) quedaron expuestas.

### ✅ Qué se hizo para resolverlo

1. **Se rotaron las credenciales expuestas** (dejan de servir las que estaban en GitHub):
   - Nueva contraseña para el usuario `tallerpro` de MySQL (cambiada directo en el contenedor, verificada con una conexión real).
   - Nueva clave JWT (32+ caracteres aleatorios).
   - Nueva contraseña para `admin@tallerpro.mx` (usando el propio flujo de "olvidé mi contraseña" del sistema; se verificó con un login real).
2. **Se sacaron los valores del archivo que se sube a GitHub.** `application.yml` ya no tiene usuario, contraseña ni clave JWT escritos: ahora los importa desde un archivo nuevo, `application-secrets.yml`, agregado a `.gitignore` (nunca se sube). `frontend/.env` también quedó fuera del control de versiones (se usó `git rm --cached`, así que sigue en el disco pero ya no viaja con el repositorio).
3. **Se dejaron plantillas sin datos reales** para que el proyecto lo pueda levantar cualquier otra persona: `backend/src/main/resources/application-secrets.yml.example` y `frontend/.env.example`, ambas sí incluidas en el repositorio.
4. **Se limpió el `README.md`**: ya no menciona la contraseña del admin ni la de MySQL; en su lugar explica cómo copiar los archivos `.example` y llenarlos con datos propios.

### Cómo queda la configuración de aquí en adelante

| Archivo | Ruta | ¿Se sube a GitHub? | Contiene |
|---|---|---|---|
| `application.yml` | `backend/src/main/resources/application.yml` | Sí | Configuración general; ya sin credenciales. Importa `application-secrets.yml` con `spring.config.import`. |
| `application-secrets.yml` | `backend/src/main/resources/application-secrets.yml` | **No** (en `.gitignore`) | `spring.datasource.username`, `spring.datasource.password`, `tallerpro.jwt.secret` — los valores reales. |
| `application-secrets.yml.example` | `backend/src/main/resources/` | Sí | Misma estructura, con valores de ejemplo (`tu_usuario_aqui`, etc.). |
| `.env` | `frontend/.env` | **No** (en `.gitignore`) | `VITE_API_BASE_URL` con el valor real. |
| `.env.example` | `frontend/.env.example` | Sí | Misma variable, con un valor de ejemplo. |

### Pendiente, por decisión propia (no es un error, es una aceptación de riesgo)

`DataSeeder.java` sigue creando al usuario admin con una contraseña inicial **fija**, escrita en el código fuente (que es público). Se evaluó cambiarla por una generada al azar en cada arranque, pero se decidió **dejarla como está**. Mientras el proyecto siga así, cualquiera que lea el código puede ver cuál es esa contraseña inicial — el riesgo se mitiga cambiándola manualmente apenas se inicia sesión por primera vez (como ya se hizo en la instancia actual).

---

## 6. Dónde publicar

### Backend (Spring Boot + MySQL)

El backend necesita: (a) un lugar que ejecute Java de forma continua, y (b) una base de datos MySQL accesible por internet (el contenedor Docker local no sirve para esto).

**Opción recomendada — Railway** (tiene plan gratuito con créditos de uso mensual):
1. Crea una cuenta en Railway con tu usuario de GitHub (`ArlxxPV`).
2. "New Project" → "Deploy from GitHub repo" → selecciona tu repositorio.
3. Railway detecta el `pom.xml` y compila el backend automáticamente (usa Maven).
4. Agrega un servicio de base de datos: "New" → "Database" → "Add MySQL". Railway te da automáticamente una URL, usuario y contraseña de conexión.
5. En la pestaña **Variables** del servicio del backend, agrega (sin escribir estos valores en ningún archivo del repositorio):
   - `SPRING_DATASOURCE_URL` (la URL que te dio el MySQL de Railway)
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `TALLERPRO_JWT_SECRET` (una clave nueva, de al menos 32 caracteres)
   - `SERVER_PORT` normalmente no hace falta: Railway inyecta su propio `PORT`.
6. Railway te da una URL pública para el backend (por ejemplo `tuapp.up.railway.app`); esa es la que usarás en el frontend.

**Alternativas gratuitas:**
- **Render**: buen soporte para Spring Boot, pero su base de datos gratuita es PostgreSQL, no MySQL. Tendrías que usar una MySQL externa (por ejemplo Railway o **Aiven**, que ofrece un plan MySQL gratuito) y solo desplegar el backend en Render.
- **Aiven**: ofrece un plan gratuito de MySQL administrado, útil si prefieres separar backend y base de datos en distintos proveedores.

### Frontend (Vue + Vite)

El frontend se compila a archivos estáticos (`npm run build`), así que puede publicarse en cualquier servicio de hosting estático.

**Opción recomendada — Vercel** (gratuito):
1. Crea una cuenta en Vercel con tu usuario de GitHub.
2. "Add New Project" → selecciona el repositorio → en "Root Directory" indica `frontend`.
3. Vercel detecta Vite automáticamente (`npm run build`, carpeta de salida `dist`).
4. En "Environment Variables" agrega `VITE_API_BASE_URL` apuntando a la URL pública de tu backend (la de Railway del paso anterior), por ejemplo `https://tuapp.up.railway.app/api`.
5. Al desplegar, Vercel te da una URL pública para el frontend.

**Alternativa gratuita:** Netlify funciona de forma muy similar (mismo tipo de configuración: carpeta `frontend`, comando `npm run build`, carpeta `dist`, variable `VITE_API_BASE_URL`).

**Importante después de publicar:** en `SecurityConfig.java`, la lista de orígenes permitidos (CORS) hoy solo incluye `http://localhost:5173`. Cuando publiques el frontend, deberás agregar ahí la URL pública de Vercel/Netlify para que el backend acepte peticiones desde ella (esto sí requiere un cambio de código, no solo de configuración).

---

## Próximos pasos recomendados (Fase 2)

1. Subir a GitHub los cambios de seguridad ya hechos localmente (`.gitignore`, `application.yml`, `README.md`, los `.example`) — quedan listos, solo falta el `commit`/`push`.
2. Construir los endpoints y pantallas de **clientes y vehículos** (el modelo de datos ya existe).
3. Conectar un proveedor SMTP real para el correo de recuperación de contraseña.
4. Agregar la pantalla administrativa para asignar el rol `EMPLEADO`.
5. Definir y construir el módulo de **órdenes de reparación** (no existe código todavía).
6. Evaluar si vale la pena cambiar `DataSeeder.java` para que la contraseña inicial del admin se genere al azar (ver sección 5) — quedó pendiente por decisión propia, no técnica.
