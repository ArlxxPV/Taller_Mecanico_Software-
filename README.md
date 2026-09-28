# TallerPro — Módulo 1: Identidad, usuarios y roles

Sistema de gestión de órdenes de reparación para talleres mecánicos.
Este entregable cubre **solo el Módulo 1** (identidad/autenticación), tal como se acordó.

## 1. Estructura de carpetas

```
tallerpro/
├── backend/                         # Spring Boot 4.1 (Java 25)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/tallerpro/backend/
│       │   ├── BackendApplication.java
│       │   ├── config/               # Seguridad, CORS, JWT props, seed de datos
│       │   ├── security/             # JwtService, filtro JWT, UserDetailsService
│       │   ├── model/                # Entidades JPA (Usuario, Rol, Cliente, Vehiculo...)
│       │   ├── repository/           # Interfaces Spring Data JPA
│       │   ├── dto/                  # Records de entrada/salida de la API
│       │   ├── service/              # AuthService, PasswordResetService, MailService
│       │   ├── controller/           # AuthController, UsuarioController
│       │   └── exception/            # Manejo global de errores
│       └── resources/
│           ├── application.yml       # Conexión a tallerpro-mysql
│           └── db/migration/         # Scripts Flyway (V1, V2)
│
├── frontend/                        # Vue 3 + Vite + Tailwind + PrimeVue
│   ├── package.json
│   ├── tailwind.config.js            # Paleta azul "blueprint"
│   ├── index.html
│   └── src/
│       ├── main.js / App.vue
│       ├── router/index.js           # Rutas + guard de autenticación
│       ├── stores/auth.js            # Estado global (Pinia)
│       ├── services/                 # Axios + llamadas a la API
│       ├── components/AuthLayout.vue # Layout compartido de las pantallas de acceso
│       └── views/                    # Login, Registro, Recuperación, Panel
│
└── README.md                        # Este archivo
```

## 2. Tablas creadas en la base `tallerpro`

Las crea Flyway automáticamente al arrancar el backend (no hay que ejecutar SQL a mano):

| Tabla | Para qué sirve |
|---|---|
| `roles` | Catálogo de roles (`ADMIN`, `EMPLEADO`, `CLIENTE`) |
| `usuarios` | Identidad: credenciales, estado, intentos fallidos, último login |
| `usuario_roles` | Relación N:M usuario ↔ rol |
| `password_reset_tokens` | Tokens (hasheados) para el flujo de "olvidé mi contraseña" |
| `login_audit` | Bitácora de cada intento de inicio de sesión (éxito/fallo, IP, fecha) |
| `clientes` | Datos de negocio del cliente (1:1 con un usuario rol `CLIENTE`) |
| `vehiculos` | Vehículos de cada cliente |

Ver el detalle completo en `backend/src/main/resources/db/migration/V1__crear_esquema_identidad.sql`.

## 3. Decisiones que tomé por ti (no especificadas en el pedido)

- **Java de compilación = 25 (LTS), no 27.** Java 27 se lanzó apenas el 15-sep-2026 y Spring
  Boot 4.1 aún no lo certifica oficialmente (su matriz de soporte llega a Java 26). El bytecode
  es compatible: puedes correr el `.jar` igual con tu JDK 27 instalado. Cuando Spring Boot
  publique soporte formal para 27, solo cambias `<java.version>` en `pom.xml`.
- **Roles base**: `ADMIN`, `EMPLEADO`, `CLIENTE`. El registro público solo crea usuarios `CLIENTE`;
  roles de staff se asignarían desde una pantalla administrativa en una fase futura. Puedes
  agregar roles más finos (`MECANICO`, `RECEPCIONISTA`) con un solo `INSERT` en `roles`.
- **Sesiones = JWT + bitácora, no sesiones de servidor.** Como el frontend (Vue) y el backend
  (API REST) son aplicaciones separadas, uso tokens JWT sin estado (30 min de vigencia) en vez
  de sesiones de servidor. Para responder "quién entró y cuándo" agregué la tabla `login_audit`,
  que registra cada intento con fecha, IP y si fue exitoso.
- **Contraseñas con Argon2**, no BCrypt. Argon2id es la recomendación actual de Spring Security 7
  (más resistente a ataques por GPU que BCrypt). Requiere BouncyCastle en el classpath, ya incluido
  en el `pom.xml`.
- **Bloqueo por intentos fallidos**: 5 intentos fallidos bloquean la cuenta 15 minutos
  (configurable en `application.yml` bajo `tallerpro.seguridad`).
- **Recuperación de contraseña sin SMTP real todavía**: como no me diste credenciales de correo,
  el "envío" del enlace de recuperación por ahora solo se registra en el log del backend
  (`ConsoleMailService`). El diseño ya está pensado para que conectar un SMTP real sea
  crear una clase nueva que implemente `MailService` — no hay que tocar el resto del código.
- **Usuario admin de arranque**: al levantar el backend por primera vez se crea automáticamente
  `admin@tallerpro.mx` / `Admin123!` (ver sección 6). Cámbiala apenas inicies sesión.
- **Componentes de UI**: Vue + Tailwind + **PrimeVue** (tema "Aura", reconfigurado en tono azul)
  para tener componentes ya pulidos (inputs, botones, toasts) sin perder la personalización de Tailwind.

## 4. Verificar que el contenedor de MySQL está corriendo

```bash
docker ps --filter "name=tallerpro-mysql"
```

Si no aparece corriendo:

```bash
docker start tallerpro-mysql
```

## 5. Levantar el backend

Desde VS Code, abre la carpeta `backend/` (con la extensión "Extension Pack for Java" instalada),
o desde una terminal:

Antes de arrancar, copia `backend/src/main/resources/application-secrets.yml.example` como
`application-secrets.yml` (en esa misma carpeta) y pon ahí tus credenciales reales de MySQL y
una clave JWT propia — ese archivo está en `.gitignore`, nunca se sube al repositorio.

```bash
cd backend
mvn spring-boot:run
```

Al arrancar, Flyway crea las tablas y en el log verás el bloque `USUARIO ADMIN CREADO` con las
credenciales iniciales. El backend queda escuchando en `http://localhost:8081`.

## 6. Levantar el frontend

Copia también `frontend/.env.example` como `frontend/.env` (tampoco se sube al repositorio).

```bash
cd frontend
npm install
npm run dev
```

Se abre en `http://localhost:5173`. Ya viene apuntando al backend en `localhost:8081`
(ver `frontend/.env`).

**Para probarlo de inmediato:** entra a `http://localhost:5173/login` con el correo
`admin@tallerpro.mx`. La contraseña inicial está definida en
`backend/src/main/java/com/tallerpro/backend/config/DataSeeder.java` (no se repite aquí a
propósito, para no dejarla en texto plano en el repositorio). **Cámbiala de inmediato** desde
"Olvidé mi contraseña" apenas inicies sesión la primera vez.

O crea una cuenta nueva desde "Crea una cuenta" (queda con rol `CLIENTE`).

## 7. Comprobar que el registro y el login sí escriben/leen en MySQL

Entra al cliente de MySQL dentro del contenedor (te pedirá la contraseña que hayas puesto en
`application-secrets.yml`):

```bash
docker exec -it tallerpro-mysql mysql -u tallerpro -p tallerpro
```

Y dentro de la sesión de MySQL:

```sql
-- Ver todos los usuarios registrados (sin exponer el hash completo)
SELECT id, nombre_completo, email, estado, intentos_fallidos, ultimo_login_en, ultimo_login_ip
FROM usuarios;

-- Ver los roles asignados a cada usuario
SELECT u.email, r.nombre AS rol
FROM usuarios u
JOIN usuario_roles ur ON ur.usuario_id = u.id
JOIN roles r ON r.id = ur.rol_id;

-- Ver la bitácora de inicios de sesión (éxitos y fallos)
SELECT email_intento, exitoso, motivo_fallo, ip_origen, creado_en
FROM login_audit
ORDER BY creado_en DESC
LIMIT 20;
```

Cada vez que registres un usuario o inicies sesión desde el frontend, deberías ver una fila
nueva reflejada de inmediato en estas consultas.

También puedes probar la API directamente sin el frontend:

```bash
# Registro
curl -X POST http://localhost:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombreCompleto":"Ana Torres","email":"ana@example.com","password":"Clave123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@example.com","password":"Clave123"}'
```

## 8. Próximos módulos (fuera de este alcance)

- Gestión completa de clientes y vehículos (pantallas CRUD)
- Pantalla administrativa para asignar roles `EMPLEADO`
- Refresh tokens / cierre de sesión remoto
- SMTP real para los correos de recuperación
