# Registro — Vehículos y Estatus (TallerPro)

Documento de cierre de la parte de **catálogo de estatus** y **vehículos** del trabajo
basado en las maquetas "Pantallas fase 2". La parte de administración de clientes (y el
panel) está en el documento hermano `registro_clientes.md`. Roles configurables, personal
y edición de talleres están en `registro_roles_y_talleres.md`. Verificado con `curl`
contra el backend real al cierre de cada sub-fase.

## 1. Resumen ejecutivo

1. **Diseño visual**: paleta, tipografía (Plus Jakarta Sans) y clases de componentes
   nuevas (`.tarjeta-tp`, `.boton-tp-*`, `.campo-tp`, `.tabla-tp`, `.chip-estatus-*`, etc.)
   en `tailwind.config.js` / `main.css`, sin tocar las clases viejas que ya usan otras
   pantallas. Se extrajo `AppLayout.vue` (sidebar + topbar + breadcrumb) de lo que antes
   era `DashboardView.vue`, para que Panel, Clientes, Vehículos, Talleres y Catálogos
   compartan el mismo layout.
2. **Catálogo de estatus**: tabla `estatus` editable (admin), con soft delete, que
   reemplaza por dentro al antiguo enum `estado_cliente` como fuente de verdad del
   estatus de clientes y vehículos.
3. **Vehículos**: registro de vehículo asociado a un cliente ACTIVO, con VIN/placa
   validados y normalizados, catálogo de marcas/modelos vía la API NHTSA vPIC (con
   caché de 7 días), pantalla de solo consulta.
4. **Edición y cambio de propietario de vehículos** (agregado después, vía el
   orquestador): ya se puede editar un vehículo existente, reasignarlo a otro cliente
   (lo suspende automáticamente y deja un historial completo de a quién perteneció y
   cuándo), y buscarlo por VIN o placa desde "Vehículos" (un solo buscador: el filtro
   difuso original, ver §7 — se agregó y luego se quitó un segundo buscador de
   coincidencia exacta, por pedido explícito de dejar uno solo).
5. **Activar/desactivar vehículo y auditoría de edición** (agregado después, vía el
   orquestador): editar un vehículo y activarlo/desactivarlo ahora quedan registrados en
   un historial de auditoría (`historial_estatus_vehiculo`), igual patrón que ya tenía
   `historial_estatus_cliente`. Desactivar/activar pide confirmación en el frontend antes
   de aplicarse.

## 2. Esquema de base de datos

| Migración | Qué hace |
|---|---|
| `V10__crea_catalogo_estatus.sql` | Crea `estatus` (id, valor único, descripción, es_sistema, soft delete). Siembra los 4 estatus de sistema con IDs fijos (1=ACTIVO, 2=SUSPENDIDO, 3=ACTUALIZADO, 4=ELIMINADO). Agrega `clientes.estatus_id` (FK) y lo rellena desde el `estado_cliente` existente. |
| `V11__permiso_estatus_administrar.sql` | Agrega el permiso `ESTATUS_ADMINISTRAR` al catálogo configurable y se lo da a ADMIN. |
| `V14__extiende_vehiculos.sql` | La tabla `vehiculos` ya existía (vacía) desde la Fase 01. Se le agrega `numero_serie`, `version`, `estatus_id` (FK), `marca_api_id`, `modelo_api_id`, `creado_por_id`, `actualizado_en`, `eliminado_en`; `vin` pasa a `NOT NULL`. |
| `V15__historial_relacion_cliente_vehiculo.sql` | Crea `historial_relacion_cliente_vehiculo` (vehiculo_id, cliente_id, tipo, vigente, fecha_inicio, fecha_fin, motivo, usuario_id): una fila por cada vez que un vehículo estuvo asociado a un cliente. Backfill: una fila vigente por cada vehículo que ya existía, con el cliente que ya tenía. |
| `V16__historial_estatus_vehiculo.sql` | Crea `historial_estatus_vehiculo` (vehiculo_id, estatus_anterior_id, estatus_nuevo_id, motivo, cambios JSON, usuario_id): una fila por cada edición (con el diff) y por cada activar/desactivar. Backfill: una fila "Alta" por cada vehículo que ya existía. Mismo esquema exacto que `historial_estatus_cliente` (V12). |

**Decisión de diseño nueva**: a diferencia de `estado_cliente` (que se congeló, ver abajo),
`vehiculos.cliente_id` **no se congeló** al agregar la tabla de historial: sigue siendo el
puntero rápido al cliente actual (se actualiza en cada reasignación), porque el resto del
código (aislamiento por taller, listados, `VehiculoResponse`) ya dependía de leerlo directo.
`historial_relacion_cliente_vehiculo` es quien guarda el historial completo: ninguna fila se
borra, solo se cierra con `vigente=false` y `fecha_fin` al reasignar.

**Decisión de diseño clave**: `clientes.estado_cliente` (el `ENUM` Java viejo) **no se
borró ni se le sigue escribiendo** — queda congelado como estaba el día de la migración.
La fuente de verdad real desde esta fase es `clientes.estatus_id` → tabla `estatus`. El
campo JSON `estadoCliente` que ya devolvía la API **no cambió de nombre ni de formato**
(sigue siendo texto: "ACTIVO"/"SUSPENDIDO", ahora también puede ser "ELIMINADO"): nadie que
ya consumía esa API tuvo que cambiar nada. Detalle completo de cómo esto afecta a
`Cliente` en `registro_clientes.md`.

## 3. Backend — catálogo de endpoints

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET/POST/PUT /api/estatus`, `DELETE /api/estatus/{id}`, `PATCH /api/estatus/{id}/restaurar` | GET: quien ve clientes. Resto: `ESTATUS_ADMINISTRAR` | CRUD con soft delete del catálogo de estatus. |
| `POST /api/clientes/{id}/vehiculos` | `CLIENTES_CREAR`/`CLIENTES_EDITAR` | Registra y asocia un vehículo; 409 si el cliente no está ACTIVO. |
| `GET /api/clientes/{id}/vehiculos` | Quien ve clientes | Vehículos **vigentes e históricos** de ese cliente (cada fila trae `vigente`, `fechaInicio`, `fechaFin`, `motivo`) — antes solo regresaba los vigentes. |
| `GET /api/vehiculos`, `/verificar-vin`, `/verificar-placa` | Quien ve clientes | Lista paginada (solo consulta, coincidencia parcial de placa/VIN) y chequeos de disponibilidad. |
| `GET /api/clientes/catalogos/vehiculos/{marcas,modelos,vin/{vin}}` | Quien ve clientes | Proxy a NHTSA vPIC, nunca se llama directo desde el navegador. |
| `GET /api/vehiculos/{id}` | Quien ve clientes | Un vehículo por id (precarga de la edición). |
| `PUT /api/vehiculos/{id}` | `CLIENTES_CREAR`/`CLIENTES_EDITAR` | Edita los datos del vehículo; 409 si el VIN o la placa ya los tiene otro vehículo (el mismo que ya tenía no cuenta como duplicado); 422 si no hay ningún cambio real. Deja un movimiento en `historial_estatus_vehiculo` con el diff exacto. |
| `GET /api/vehiculos/{id}/cliente` | Quien ve clientes | Cliente vigente de ese vehículo (reusa `ClienteService.obtener`, mismos permisos y aislamiento). |
| `PATCH /api/vehiculos/{id}/cliente` | `CLIENTES_CREAR`/`CLIENTES_EDITAR` | Reasigna el vehículo a otro cliente: cierra la relación vigente anterior, abre una nueva y **suspende el vehículo**. Exige `motivo` (10-255 caracteres); 409 si el cliente nuevo es el mismo que ya tenía. |
| `PATCH /api/vehiculos/{id}/desactivar`, `/activar` | `CLIENTES_CREAR`/`CLIENTES_EDITAR` | `UPDATE` condicionado atómico (mismo patrón que clientes); 409 si ya estaba en ese estatus. Ambos quedan en `historial_estatus_vehiculo`. |
| `GET /api/vehiculos/{id}/historial` | Quien ve clientes | Auditoría del vehículo: altas (backfill), ediciones (con el diff) y activaciones/desactivaciones. |

## 4. Decisiones de diseño relevantes

- **Dígito verificador del VIN**: se calcula (posición 9, estándar ISO 3779) y se muestra
  como advertencia en el frontend (`utils/vehiculos.js`), nunca bloquea el registro — tal
  como pide la maqueta.
- **Caché de NHTSA vPIC**: en memoria (`ConcurrentHashMap` + TTL de 7 días), mismo patrón
  que ya usaba `DireccionCatalogoService` para Postali. Si vPIC no responde, se regresa la
  caché (aunque esté vencida) o una lista vacía — nunca se rompe el formulario.
- **Autocompletar de marca/modelo**: `<input list>` + `<datalist>` nativos del navegador,
  no un componente de PrimeVue (el proyecto evita `Dropdown`/`Autocomplete` de PrimeVue a
  propósito). Esto también resuelve "No aparece en la lista" gratis: siempre se puede
  escribir libre.
- **Placa**: se guarda sin guiones (`HGA123A`), se muestra con guiones
  (`formatearPlaca()` en el frontend) — nunca se guardan dos formatos distintos en BD.
- **"En uso" del catálogo de estatus** cuenta clientes **y** vehículos (se completó en
  cuanto `vehiculos.estatus_id` existió).
- **Reasignar un vehículo siempre lo suspende**: regla de negocio explícita (no un efecto
  secundario accidental) — fuerza una revisión antes de volver a usarlo con el nuevo dueño.
- **Solo una relación cliente-vehículo vigente a la vez**: antes de abrir la nueva se cierra
  la anterior en la misma transacción; nunca coexisten dos relaciones vigentes del mismo
  vehículo.
- **Un solo buscador de vehículos**: se agregó un segundo buscador (ficha exacta por
  VIN/placa/ID) y luego se quitó por pedido explícito, para no duplicar funcionalidad —
  quedó solo el filtro difuso original (`GET /api/vehiculos?q=`, coincidencia parcial,
  integrado con el filtro de Estatus y la paginación). Se eliminó por completo (endpoint,
  service, repository y UI), no solo se ocultó.
- **Auditoría de vehículo reusa el estatus de sistema `ACTUALIZADO`**: igual que
  `ClienteService.actualizar()`, una edición no cambia `vehiculo.estatus` real; la fila de
  historial usa `ACTUALIZADO` (id 3) como marcador de "esto fue una edición", con
  `estatus_anterior_id` nulo. El javadoc de `EstatusId.ACTUALIZADO` ya anticipaba esto
  ("Vehiculo (fases siguientes)") desde que se creó en V10.
- **Activar/desactivar con `UPDATE` condicionado atómico**: mismo patrón exacto que
  `ClienteRepository.cambiarEstatusSiEsta` (evita una condición de carrera entre leer el
  estatus y escribirlo); 0 filas afectadas decide el 409. No se le agregó `motivo`
  obligatorio (no se pidió): la fila de auditoría se guarda con `motivo=null` para estas
  dos transiciones.
- **No se auditan las órdenes de reparación al editar un vehículo** porque ese módulo no
  existe todavía en el proyecto (se verificó antes de implementar): la regla de "no
  alterar retroactivamente órdenes cerradas" queda como principio para cuando ese módulo
  se construya, no como código que hoy haga algo.

## 5. Archivos

**Backend, nuevos**: `V10`, `V11`, `V14` (migraciones), `model/{Estatus,EstatusId}.java`,
`repository/EstatusRepository.java`, `service/{Estatus,Vehiculo,CatalogoVehiculos}Service.java`,
`controller/{Estatus,Vehiculo,CatalogoVehiculos}Controller.java`, DTOs: `EstatusRequest/
Response`, `Vehiculo{Request,Response}`, `PaginaVehiculosResponse`, `DisponibilidadResponse`,
`VinDecodificadoResponse`.

**Backend, modificados**: `model/{Cliente,Vehiculo,ClavePermiso}.java` (relación a
`Estatus`), `repository/VehiculoRepository.java`, `dto/EstatusResponse.java` (conteo de
vehículos), `controller/ClienteController.java` (endpoints de vehículos del cliente),
`config/SecurityConfig.java`.

**Frontend, nuevos**: `layouts/AppLayout.vue`, `components/{ModalBase,
ModalAsociarVehiculo}.vue`, `utils/vehiculos.js`, `views/{EstatusListaView,
EstatusFormularioView,VehiculosView}.vue`, repositories/facades de `estatus`, `vehiculo` y
`catalogoVehiculos`.

**Frontend, modificados**: `tailwind.config.js`, `index.html`, `assets/main.css`,
`views/DashboardView.vue` (ahora hijo de `AppLayout`), `router/index.js`.

### 5.1 Agregado después: edición, cambio de propietario y búsqueda exacta

**Backend, nuevos**: `V15` (migración), `model/RelacionClienteVehiculo.java`,
`repository/RelacionClienteVehiculoRepository.java`, DTOs: `CambiarClienteVehiculoRequest`,
`VehiculoClienteHistorialResponse`.

**Backend, modificados**: `repository/VehiculoRepository.java` (unicidad de VIN/placa
excluyendo el propio id), `service/VehiculoService.java` (`obtener`, `actualizar`,
`listarPorCliente` con vigente/histórico, `cambiarPropietario`, `obtenerClienteVigente`),
`controller/VehiculoController.java` (`GET/PUT /{id}`, `GET/PATCH /{id}/cliente`),
`controller/ClienteController.java` (tipo de retorno de `GET /{id}/vehiculos`),
`config/SecurityConfig.java` (reglas para `PUT /api/vehiculos/*` y
`PATCH /api/vehiculos/*/cliente`).

**Frontend, nuevos**: `components/{ModalEditarVehiculo,ModalCambiarClienteVehiculo}.vue`.

**Frontend, modificados**: `repositories/vehiculoRepository.js`,
`facades/vehiculoFacade.js`, `views/VehiculosView.vue` (acciones "Editar"/"Cambiar
cliente"), `views/ClienteDetalleView.vue` (la pestaña "Vehículos" distingue vigente de
histórico).

### 5.2 Agregado después: activar/desactivar, auditoría de edición, y un solo buscador

**Backend, nuevos**: `V16` (migración), `model/HistorialEstatusVehiculo.java`,
`repository/HistorialEstatusVehiculoRepository.java`, DTO:
`HistorialEstatusVehiculoResponse`.

**Backend, modificados**: `repository/VehiculoRepository.java` (`cambiarEstatusSiEsta`
condicionado; se quitó `findByVinIgnoreCaseOrPlacaIgnoreCase`, ya no se usaba),
`service/VehiculoService.java` (`actualizar` ahora calcula el diff y audita, nuevos
`activar`/`desactivar`/`historial`; se quitó `buscarExacto`),
`controller/VehiculoController.java` (`PATCH /{id}/activar`, `/desactivar`,
`GET /{id}/historial`; se quitó `GET /buscar`), `config/SecurityConfig.java` (reglas para
`PATCH /api/vehiculos/*/activar` y `/desactivar`).

**Frontend, nuevos**: ninguno (se reutilizó `ModalBase.vue` ya existente para el modal de
confirmación de activar/desactivar, directo en `VehiculosView.vue`).

**Frontend, modificados**: `repositories/vehiculoRepository.js`,
`facades/vehiculoFacade.js` (`activar`, `desactivar`, `historial`; se quitó
`buscarExacto`), `views/VehiculosView.vue` (se quitó el cuadro de búsqueda exacta por
VIN/placa/ID completo —script y template—, se agregó el botón "Activar"/"Desactivar" por
fila con modal de confirmación).

**Sin tocar, a propósito**: no se agregó una pantalla para ver el historial del vehículo
(el endpoint `GET /{id}/historial` existe y funciona, pero ningún componente lo consume
todavía — no se pidió una pestaña de historial para vehículos, solo que la auditoría
existiera y se preservara).

## 6. Verificación

Probado con `curl` contra `localhost:8081` real, limpiando los datos de prueba después de
cada corrida. Sin navegador disponible en este entorno (sin Playwright/Chromium
instalados): solo verificación de API, no visual.

| # | Punto | Resultado |
|---|---|---|
| 1 | La FK `clientes → estatus` existe | ✅ |
| 2 | No se puede asociar un vehículo a un cliente suspendido | ✅ probado (409) |
| 3 | "hga 123 a" se guarda como `HGA123A` | ✅ (normalización + regex) |
| 4 | Los modelos llegan de vPIC | ✅ probado con datos reales (Nissan Versa 2020) |
| 5 | El vehículo nace ACTIVO | ✅ probado |
| 6 | VIN/placa duplicados responden 409 | ✅ probado |
| 7 | Los 4 estatus de sistema no se pueden dar de baja ni cambiar de valor | ✅ probado |
| 8 | Editar un vehículo conservando su propio VIN/placa no da un falso 409 | ✅ probado |
| 9 | Editar con el VIN o la placa de otro vehículo sí da 409 | ✅ probado |
| 10 | Reasignar a otro cliente deja el vehículo SUSPENDIDO; reasignar al mismo cliente que ya tenía da 409 | ✅ probado |
| 11 | El cliente anterior ve el vehículo como histórico (`vigente:false`, con el motivo); el nuevo lo ve vigente | ✅ probado |
| 12 | Editar un vehículo deja un movimiento en `historial_estatus_vehiculo` con el diff exacto (`{"Color": ["Gris","Negro"], ...}`) | ✅ probado |
| 13 | Editar sin cambios responde 422 | ✅ probado |
| 14 | Desactivar deja el vehículo SUSPENDIDO y lo audita; desactivar de nuevo da 409 | ✅ probado |
| 15 | Activar lo regresa a ACTIVO y lo audita | ✅ probado |
| 16 | `GET /{id}/historial` trae los 3 movimientos en orden (más reciente primero) | ✅ probado |
| 17 | El buscador de ficha exacta quedó genuinamente eliminado (`GET /api/vehiculos/buscar` ya no existe como ruta) | ✅ probado (el 500 que da ahora es el mismo que da cualquier id no numérico contra `/api/vehiculos/{id}`, confirmado comparando ambos — no es una regresión nueva) |

## 7. Deuda técnica / pendientes

- Sigue sin existir un "eliminar vehículo" (baja lógica): `Vehiculo.eliminadoEn` existe en
  el modelo pero ningún endpoint lo escribe todavía. Editar, reasignar (cambiar
  propietario) y activar/desactivar ya sí existen.
- `VehiculoService.crear()` no escribe una fila de "Alta" en `historial_estatus_vehiculo`
  (solo el backfill de V16 tiene esas filas, para los vehículos que ya existían antes de
  la migración). Fue una decisión de alcance: no se pidió auditar el registro, solo
  "actualizar" y "activar/desactivar" — un vehículo nuevo arranca sin historial hasta su
  primera edición o cambio de estatus.
- El aislamiento por taller de `activar`/`desactivar`/`cambiarPropietario` no se volvió a
  probar en vivo con una cuenta real de otro taller (mismo patrón de código ya probado
  antes en `obtener`/`actualizar`/`listarPorCliente`); no se quiso mutar una cuenta real
  (`recep@tallerpro.mx`) solo para esta prueba.
- El Flyway sigue sin correr solo con `mvn spring-boot:run` (problema heredado de fases
  anteriores, documentado desde `registro_Fase02.md`): toda migración de V10 a V16 se
  aplicó a mano.
