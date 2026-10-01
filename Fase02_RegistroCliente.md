# Fase 2 — Registro de clientes

> Documenta lo construido en esta fase: el refuerzo del módulo de cuentas (fase 1) y el nuevo registro de clientes. No se subió nada a GitHub — los comandos de `commit`/`push` quedan para que tú los corras.

---

## 1. Métodos y funciones creados o modificados

### 1.1 Fase 1 (cuentas) — adaptados a la arquitectura Vista → Facade → Repository → API

El módulo de cuentas **ya usaba** una Vista y un Repository, pero las Vistas llamaban unas veces al Store (Pinia) y otras veces directo al Repository — sin una capa Facade intermedia. Se reorganizó para que todo pase por el Facade.

| Archivo | Función | Recibe | Devuelve |
|---|---|---|---|
| `repositories/authRepository.js` *(antes `services/authService.js`, mismo contenido)* | `registrar`, `login`, `olvidePassword`, `restablecerPassword`, `obtenerPerfil` | los datos del formulario correspondiente | la respuesta cruda de Axios (promesa) |
| `facades/authFacade.js` *(nuevo)* | `registrar(datos)` | objeto con nombre, correo, teléfono, contraseña | `{ ok, mensaje, usuario? }` |
| | `login(credenciales)` | `{ email, password }` | `{ ok, mensaje, usuario? }` — si `ok`, ya dejó la sesión guardada en el Store |
| | `cargarPerfil()` | nada | `{ ok, usuario? }` — actualiza el Store con el perfil |
| | `cerrarSesion()` | nada | `{ ok, mensaje }` — limpia el Store |
| | `olvidePassword(email)` | correo | `{ ok, mensaje }` |
| | `restablecerPassword(token, nuevaPassword)` | token del enlace y contraseña nueva | `{ ok, mensaje }` |
| `stores/auth.js` | `establecerSesion(token, usuario)`, `establecerUsuario(usuario)`, `limpiarSesion()` | — | ya no hace peticiones HTTP, solo guarda estado |
| `views/LoginView.vue`, `RegisterView.vue`, `ForgotPasswordView.vue`, `ResetPasswordView.vue`, `DashboardView.vue` | — | — | se cambiaron sus llamadas para usar `authFacade` en vez del Store o el Repository directamente |
| `router/index.js` | `router.beforeEach` | la ruta a la que se navega | ahora es `async`; si la ruta pide roles (`meta.rolesPermitidos`) y el perfil no está cargado, lo carga antes de decidir |

### 1.2 Fase 2 (clientes) — nuevo

**Backend**

| Archivo | Método | Recibe | Devuelve |
|---|---|---|---|
| `ClienteController` | `crear(datos, foto)` — `POST /api/clientes` | `datos` (JSON: `ClienteRequest`) y `foto` (archivo, opcional), como `multipart/form-data` | `ClienteResponse` con estado 201 |
| `ClienteService` | `crear(ClienteRequest, MultipartFile)` | los datos validados y el archivo | `ClienteResponse`; lanza error 409 si el correo ya existe |
| | `guardarFoto(MultipartFile)` *(privado)* | el archivo | el nombre generado del archivo guardado; valida tipo (JPG/PNG) y tamaño (≤ 12 MB) antes de guardar |
| `ClienteRepository` | `existsByEmailIgnoreCase(email)` | correo | `true`/`false` — usado para no duplicar clientes |
| `GlobalExceptionHandler` | `manejarArchivoDemasiadoGrande(...)` | la excepción que lanza Spring si el archivo supera el límite del servidor | respuesta 400 con un mensaje claro |

**Frontend**

| Archivo | Método | Recibe | Devuelve |
|---|---|---|---|
| `repositories/clienteRepository.js` | `crear(datos, foto)` | los datos del cliente y el archivo de foto | la respuesta de Axios (arma el `multipart/form-data`) |
| `facades/clienteFacade.js` | `registrar(formulario, foto)` | los campos sueltos del formulario | `{ ok, mensaje, detalles?, cliente? }` — arma el objeto con la dirección anidada que espera el backend |
| `views/ClienteRegistroView.vue` | `enviar`, `registrarOtro`, `irALista` | eventos del formulario | muestra la ventana de confirmación y permite seguir capturando sin perder lo ya escrito si algo falla |
| `views/ClienteListaView.vue` *(nuevo)* | `cargar()` | nada (se ejecuta al entrar a la pantalla) | pide la lista al Facade y la muestra en una tabla; si falla, avisa con un toast |
| `components/ClienteFormularioCampos.vue` *(nuevo)* | `elegirFoto`, `quitarFoto` | evento del `<input type="file">` | valida la foto en el navegador (tipo/tamaño) antes de subirla; contiene todos los campos del formulario (personales, contacto, dirección, foto) para que Registrar y Editar no dupliquen el HTML |

**Listado de clientes (agregado después de la primera entrega de esta fase):**

| Archivo | Método | Recibe | Devuelve |
|---|---|---|---|
| `ClienteController` | `listar()` — `GET /api/clientes` | nada | lista de `ClienteResponse`, más reciente primero |
| `ClienteService` | `listar()` | nada | `List<ClienteResponse>` |
| `ClienteRepository` | `findAllByOrderByCreadoEnDesc()` | nada | todos los clientes ordenados por fecha de registro |
| `repositories/clienteRepository.js` | `listar()` | nada | respuesta de Axios con el arreglo de clientes |
| `facades/clienteFacade.js` | `listar()` | nada | `{ ok, clientes?, mensaje? }` |

**Ver detalle, editar y ver foto (agregado después):**

| Archivo | Método | Recibe | Devuelve |
|---|---|---|---|
| `ClienteController` | `obtener(id)` — `GET /api/clientes/{id}` | el id en la URL | `ClienteResponse`; 404 si no existe |
| | `actualizar(id, datos, foto)` — `PUT /api/clientes/{id}` | igual que crear, pero `foto` es opcional (si no se manda, se conserva la que ya tenía) | `ClienteResponse` actualizado; 409 si el correo ya lo usa *otro* cliente |
| | `obtenerFoto(id)` — `GET /api/clientes/{id}/foto` | el id en la URL | el archivo de la foto (bytes), con su tipo correcto (`image/jpeg` o `image/png`); 404 si no tiene foto |
| `ClienteService` | `obtener(id)`, `actualizar(id, datos, foto)`, `obtenerRutaFoto(id)` | — | Al reemplazar una foto, borra el archivo anterior del disco para no dejar huérfanos. |
| `ClienteRepository` | `existsByEmailIgnoreCaseAndIdNot(email, id)` | correo e id a excluir | `true`/`false` — para que un cliente pueda guardarse con su propio correo sin marcarse como duplicado |
| `repositories/clienteRepository.js` | `obtener(id)`, `actualizar(id, datos, foto)`, `obtenerFotoBlob(id)` | — | la última pide la foto como *blob* (binario), no como URL directa |
| `facades/clienteFacade.js` | `obtener(id)` | id | `{ ok, cliente?, formulario?, mensaje? }` — `formulario` ya viene listo para precargar la pantalla de editar |
| | `actualizar(id, formulario, foto)` | id, campos del formulario, archivo opcional | `{ ok, mensaje, detalles?, cliente? }` |
| | `obtenerFotoUrl(id)` | id | una URL local (`URL.createObjectURL`) para usar en `<img src>`, o `null` si no hay foto. Pide la foto **con el token de sesión** (por eso no se usa `<img>` directo a la API: la foto no es pública) |
| `views/ClienteDetalleView.vue` *(nuevo)* | — | — | muestra todos los datos del cliente y su foto; solo lectura, con botón "Editar cliente" |
| `views/ClienteEditarView.vue` *(nuevo)* | `cargar()`, `enviar()` | — | precarga el formulario con los datos actuales (y la foto, si tiene) y guarda los cambios; si el usuario no elige una foto nueva, la que ya tenía se mantiene igual |

**Nota sobre la foto:** no es una URL pública — el navegador la pide con el token de sesión (igual que cualquier otro dato) y la convierte en una imagen local. Así, alguien que no inició sesión como ADMIN/RECEPCIONISTA no puede ver fotos de clientes solo adivinando una URL.

---

## 2. Estado de los elementos

| Elemento | Descripción | Estado |
|---|---|---|
| Alertas de éxito/error en cuentas | Aviso visual (toast) en crear cuenta, login, logout, olvidé/restablecer contraseña | Completo |
| Mostrar/ocultar contraseña en login | Botón con icono de ojo | Completo |
| Pantalla de registro con el mismo diseño que login | Mismos colores, tipografía, botones y distribución | Completo *(ya lo cumplía desde fase 1; se conservó igual)* |
| Arquitectura Vista → Facade → Repository → API (cuentas) | Reorganización de fase 1 sin cambiar el comportamiento visible | Completo |
| Formulario de registro de clientes | Todos los campos pedidos (personales, contacto, dirección, foto) | Completo |
| Validación de formato por campo | Indica cuál campo falló y por qué, sin borrar lo ya capturado | Completo |
| Bloqueo de clientes duplicados | Por correo; responde 409 con mensaje claro | Completo |
| Validación de foto (tipo/tamaño) | JPG/PNG, máx. 12 MB; se valida en el navegador y otra vez en el servidor | Completo |
| Ventana de confirmación al guardar | Modal con opción de registrar otro o ver la lista de clientes | Completo |
| Autorización ADMIN/RECEPCIONISTA | Backend (403 si no aplica) y frontend (oculta/redirige) | Completo |
| Rol RECEPCIONISTA | No existía; se agregó porque el registro de clientes lo requería | Completo |
| **Listar clientes ya registrados** | Pantalla `/clientes` con tabla (nombre, teléfono, correo, ubicación, fecha), enlazada desde "Clientes" en el panel | Completo |
| **Ver datos completos de un cliente, incluida su foto** | Pantalla `/clientes/:id`, se abre al hacer clic en una fila de la lista | Completo |
| **Editar los datos de un cliente ya registrado** | Pantalla `/clientes/:id/editar`, formulario precargado; la foto se puede cambiar o dejar igual | Completo |
| Diagrama de componentes | `Fase02_DiagramaArquitectura.html` | Completo *(no incluye todavía listar/ver/editar clientes; ver nota abajo)* |
| Asociar un cliente a varios talleres (Taller A, Taller B) | Un cliente podrá pertenecer a distintos talleres/direcciones de una misma empresa | **No implementado aún** — a propósito; el diseño actual no tiene ninguna referencia fija a "un solo taller" que estorbe agregarlo después |
| Eliminar un cliente | Hoy se puede crear, listar, ver y editar; borrar no existe todavía | Pendiente |
| Migraciones de Flyway no se ejecutan al arrancar | Ver sección 4 — se detectó durante las pruebas de esta fase | Pendiente de investigar |
| SMTP real, panel de roles, vehículos, órdenes, inventario | Heredado de fase 1, sin cambios | Pendiente / no iniciado |

---

## 3. Pendientes para más adelante

1. **Investigar por qué Flyway no corre** al levantar el backend con `mvn spring-boot:run` (no aparece ningún log de Flyway). Mientras tanto, las migraciones `V3` y `V4` de esta fase se aplicaron a mano directamente en MySQL para poder probar — quedan **idénticas** en los archivos `.sql` del proyecto, pero si alguien clona el repo y levanta una base de datos nueva, Flyway debería crear todo el esquema solo y hoy no está claro que lo vaya a hacer. Conviene revisarlo antes de depender de Flyway en otro entorno.
2. Construir "eliminar cliente" (ya se puede crear, listar, ver y editar).
3. Actualizar `Fase02_DiagramaArquitectura.html`: quedó dibujado con solo "crear cliente"; después se agregaron listar, ver detalle (con foto) y editar, y el diagrama no se volvió a tocar.
4. Cuando se diseñe la relación cliente–taller (Taller A, Taller B con distintas direcciones), probablemente convenga una tabla intermedia `cliente_talleres`, sin tocar las columnas que ya existen en `clientes`.
5. Todo lo que ya estaba pendiente de fase 1: SMTP real, panel para asignar roles, vehículos, órdenes, inventario.

---

## 4. Cambios hechos en fase 1 para la arquitectura

Ningún cambio de esta sección altera lo que el usuario ve o puede hacer en cuentas — mismos formularios, mismas validaciones, mismos redireccionamientos. Solo cambia **cómo está organizado el código por dentro**:

- **`services/authService.js` se eliminó** y su contenido pasó a **`repositories/authRepository.js`** (mismo código, nuevo nombre/ubicación para que coincida con el patrón pedido).
- **`stores/auth.js` dejó de hacer peticiones HTTP.** Antes tenía `login()`, `registrar()`, `cargarPerfil()` y `cerrarSesion()` que llamaban directo al Repository. Ahora solo guarda el token y el usuario; esa lógica se movió a `facades/authFacade.js`.
- **Las 5 vistas de cuentas** (`LoginView`, `RegisterView`, `ForgotPasswordView`, `ResetPasswordView`, `DashboardView`) se actualizaron para llamar al Facade en vez del Store o el Repository directamente.
- **`router/index.js`** ahora puede exigir roles específicos en una ruta (se usa para proteger `/clientes/nuevo`), y si el perfil del usuario todavía no se había cargado, lo carga antes de decidir si deja pasar.
- **Un cambio con efecto en base de datos:** la entidad `Cliente` (la tabla `clientes`) ya no depende de que exista una cuenta de usuario (`usuario_id` pasó de obligatorio a opcional), porque ahora un cliente puede registrarse manualmente sin tener cuenta propia. Como consecuencia, `Cliente` ganó su propio campo `nombre_completo` y `email` — antes esos datos siempre se leían de la cuenta (`Usuario`) asociada. En `AuthService.registrar()` (el auto-registro de fase 1) se agregaron dos líneas para seguir llenando esos campos también ahí, y así no perder el dato para los clientes que sí tienen cuenta. El comportamiento del auto-registro no cambió para quien lo usa.
