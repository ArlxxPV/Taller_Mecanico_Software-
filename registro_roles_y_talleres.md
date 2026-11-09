# Registro — Roles configurables, Personal y Edición de Talleres (TallerPro)

Documento nuevo (no existía uno para esta área): cubre el sistema de roles y permisos
configurables, la pantalla "Roles y personal", y la edición de talleres. Ninguno de los
dos documentos hermanos (`registro_clientes.md`, `registro_vehiculo_estatus.md`) lo cubre
— es la brecha que quedó registrada al documentar la búsqueda exacta de vehículos.
Verificado contra el código real y con `curl` contra `localhost:8081`.

## 1. Resumen ejecutivo

1. **Roles y permisos configurables**: reemplaza el enum Java fijo `NombreRol` (ya
   eliminado) por un catálogo configurable. Los roles son texto libre (tabla `roles`,
   ya era `VARCHAR` libre desde `V1`); cada uno tiene un conjunto de permisos (`PERM_*`)
   asignable desde la pantalla, puede suspenderse (no borrarse), y el rol de sistema
   (ADMIN) nunca pierde acceso total.
2. **Pantalla "Roles y personal"**: crear roles con permisos elegidos a mano, editar los
   permisos de un rol existente, suspenderlo/reactivarlo, dar de alta cuentas de personal
   con un rol ya creado, cambiarles el rol, y asignar contraseña a clientes que un
   recepcionista registró sin darles acceso.
3. **Edición de talleres**: pantalla nueva para editar un taller ya registrado. El
   endpoint (`PUT /api/talleres/{id}`) ya existía sin usarlo ningún formulario; solo
   faltaba la vista y la ruta.

## 2. Esquema de base de datos

| Migración | Qué hace |
|---|---|
| `V8__permisos_configurables.sql` | Crea `permisos` (11 valores iniciales) y `rol_permisos` (N:M). Agrega `roles.es_sistema` (bypass total, solo ADMIN) y ensancha `roles.nombre` a `VARCHAR(50)`. Backfill: RECEPCIONISTA recibe exactamente lo que ya podía hacer (`CLIENTES_VER/CREAR/EDITAR`, `TALLERES_VER`); ADMIN recibe los 11 permisos explícitos (además del bypass); EMPLEADO y CLIENTE quedan sin permisos. También ensancha `clientes.origen_registro` a `VARCHAR(100)` libre. |
| `V9__suspender_roles.sql` | Agrega `roles.activo` (suspender un rol sin borrarlo; ADMIN nunca se puede suspender). |

Después de `V8`/`V9` se agregaron 2 permisos más al catálogo (`ESTATUS_ADMINISTRAR` en
`V11`, `CLIENTES_ELIMINAR` en `V13`, ver `registro_vehiculo_estatus.md` y
`registro_clientes.md`): hoy son 13 en total (confirmado en vivo, `GET
/api/roles/permisos-disponibles`).

## 3. Backend — catálogo de endpoints

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/roles` | `ROLES_ADMINISTRAR` o `USUARIOS_CREAR` | Lista los roles con sus permisos (quien da de alta personal necesita el catálogo para el select, aunque no administre roles). |
| `GET /api/roles/permisos-disponibles` | `ROLES_ADMINISTRAR` o `USUARIOS_CREAR` | Catálogo fijo de permisos (hoy 13). |
| `POST /api/roles` | `ROLES_ADMINISTRAR` | Crea un rol nuevo; nunca `esSistema`. |
| `PUT /api/roles/{id}/permisos` | `ROLES_ADMINISTRAR` | Reemplaza el set completo de permisos de un rol; 409 si el rol es `esSistema`. |
| `PATCH /api/roles/{id}/estado` | `ROLES_ADMINISTRAR` | Suspende o reactiva un rol; ADMIN nunca se puede suspender. |
| `GET /api/usuarios` | `USUARIOS_VER` o `USUARIOS_CREAR` | Todas las cuentas del sistema, incluidas las de rol CLIENTE. |
| `GET /api/usuarios/yo` | Cualquier sesión autenticada | Perfil propio (nombre, roles, permisos, taller, estatus si es cliente). |
| `POST /api/usuarios` | `USUARIOS_CREAR` | Alta de una cuenta de personal con un rol ya creado. **403 si el rol elegido es `esSistema` y el actor no tiene también `ROLES_ADMINISTRAR`** — evita que alguien con solo `USUARIOS_CREAR` se autoasigne una cuenta ADMIN. |
| `PUT /api/usuarios/{id}/rol` | `USUARIOS_CREAR` | Cambia el rol (y el taller, si el rol nuevo lo requiere) de una cuenta de personal existente. |
| `PUT /api/talleres/{id}` | `TALLERES_CREAR` o `TALLERES_EDITAR` | Edita los datos de un taller (y su foto); ya existía, no lo llamaba ningún formulario hasta que se agregó la vista. |

## 4. Decisiones de diseño relevantes

- **Roles de texto libre, no un enum**: la columna `roles.nombre` ya era `VARCHAR` libre
  desde `V1` ("deja abierta la puerta a roles más finos sin tocar el esquema" dice el
  comentario original); `V8` solo explota esa puerta — no fue necesario un cambio de tipo.
- **Bypass total de ADMIN vía `esSistema`**: tanto `PermisoService.tiene()` como
  `AppUserDetailsService` ignoran lo que haya en `rol_permisos` si el rol tiene
  `esSistema=true` y conceden todo. Así ADMIN nunca pierde acceso por una mala
  configuración de permisos.
- **Anti auto-escalación de privilegios**: crear una cuenta con un rol `esSistema` exige
  que el actor tenga también `ROLES_ADMINISTRAR`, no solo `USUARIOS_CREAR` — si no,
  cualquiera con permiso de dar de alta personal podría crearse una cuenta ADMIN propia.
- **Suspender un rol, no borrarlo**: mismo patrón de baja lógica que clientes, vehículos y
  estatus en el resto del proyecto (`roles.activo`, nunca `DELETE FROM roles`).
- **`origen_registro` del cliente deja de ser un conjunto cerrado**: con roles dinámicos
  ya no existe una lista fija de "quién pudo registrar" (antes era un enum Java
  `ADMIN/RECEPCIONISTA/AUTORREGISTRO`); ahora se guarda el nombre real del rol del actor
  (o `"AUTORREGISTRO"`). Cambio no destructivo: los valores que ya había son strings
  válidos para la columna ensanchada.
- **Edición de talleres sin tocar el backend**: `TallerService.actualizar()` y el
  `PUT /api/talleres/{id}` ya existían, correctamente implementados (valida nombre/RFC
  duplicado excluyendo el propio registro); solo hacía falta la vista y la ruta del lado
  del frontend.

## 5. Archivos

**Backend, nuevos**: `V8`, `V9` (migraciones), `model/{Permiso,ClavePermiso}.java`,
`repository/PermisoRepository.java`, `service/{Permiso,Rol,Usuario}Service.java`,
`controller/RolController.java`, DTOs: `RolRequest/Response`, `PermisoResponse`,
`ActualizarPermisosRolRequest`, `CambiarEstadoRolRequest`, `UsuarioRequest`,
`CambiarRolUsuarioRequest`.

**Backend, modificados**: `model/{Rol,Usuario,Cliente}.java` (permisos, `origenRegistro`
libre), `repository/RolRepository.java`, `security/{AppUserDetailsService,JwtService}.java`
(authorities `PERM_*`), `service/{Cliente,Auth}Service.java`, `config/DataSeeder.java`,
`config/SecurityConfig.java`, `dto/UsuarioResponse.java`, `controller/{ClienteController,
UsuarioController}.java`. **Eliminado**: `model/NombreRol.java` (reemplazado por el
catálogo configurable).

**Frontend, nuevos**: `views/{PersonalView,TallerEditarView}.vue`, repositories/facades de
`rol` y `usuario`.

**Frontend, modificados**: `router/index.js` (rutas `personal` y `talleres/:id/editar`
dentro de `AppLayout`), `layouts/AppLayout.vue` (ítem de navegación "Roles y personal"),
`stores/auth.js` (`permisosUsuario`), `views/TallerListaView.vue` (columna "Acciones" con
el enlace "Editar").

## 6. Verificación

Verificado contra el código real; spot-check en vivo contra `localhost:8081` al momento de
documentar (no es la verificación exhaustiva original de cuando se construyó, que no deja
evidencia en este documento por no haberse registrado entonces).

| # | Punto | Resultado |
|---|---|---|
| 1 | `GET /api/roles/permisos-disponibles` trae el catálogo completo | ✅ probado en vivo, 13 permisos |
| 2 | `GET /api/roles` trae los 4 roles con sus permisos; ADMIN con `esSistema:true` y los 13 | ✅ probado en vivo |
| 3 | RECEPCIONISTA tiene exactamente los permisos del backfill de `V8` | 🟡 por confirmar — en este entorno ya se le agregaron permisos adicionales (`ROLES_ADMINISTRAR`, `TALLERES_EDITAR`) en pruebas manuales de sesiones anteriores; el backfill original (solo `CLIENTES_VER/CREAR/EDITAR` + `TALLERES_VER`) ya no es el estado real de esta cuenta |
| 4 | `GET /api/usuarios` trae las cuentas | ✅ probado en vivo, 6 cuentas |
| 5 | Editar un taller (crear → `GET` precarga → `PUT` → `200` con el cambio) | ✅ probado en vivo (ver `TallerEditarView`) |
| 6 | 403 al crear personal con un rol `esSistema` sin tener `ROLES_ADMINISTRAR` | 🟡 por confirmar ahora — documentado por lectura de código (`UsuarioService.crear`, línea con el chequeo), no se repitió la prueba en esta sesión de documentación |

## 7. Deuda técnica / pendientes

- No hay forma de **eliminar** un rol ni un endpoint `DELETE /api/roles/{id}`: solo
  suspender (`activo=false`). Es congruente con el resto del proyecto (nunca borrar de
  verdad), pero tampoco hay una pantalla de "roles eliminados" como sí existe para
  clientes/estatus.
- `UsuarioController` resuelve el actor autenticado con `findByEmailIgnoreCase` repetido en
  cada método en vez de un helper único (mismo patrón que `ClienteController` sí tiene) —
  brecha ya señalada en `documentacion-backend.md` §8 para `VehiculoController`; aplica
  igual aquí.
- En este entorno, la cuenta `recep@tallerpro.mx` terminó con permisos
  (`ROLES_ADMINISTRAR`, `TALLERES_EDITAR`) que no corresponden a un perfil de
  recepcionista real — quedó así de pruebas manuales de sesiones anteriores. Vale la pena
  revisarla antes de usar este entorno como referencia de "cómo se ve RECEPCIONISTA por
  defecto".
- El Flyway sigue sin correr solo con `mvn spring-boot:run` (problema heredado,
  documentado desde `registro_Fase02.md`): `V8` y `V9` se aplicaron a mano, igual que
  `V10` a `V15`.
