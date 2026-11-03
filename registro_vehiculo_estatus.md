# Registro — Vehículos y Estatus (TallerPro)

Documento de cierre de la parte de **catálogo de estatus** y **vehículos** del trabajo
basado en las maquetas "Pantallas fase 2". La parte de administración de clientes (y el
panel) está en el documento hermano `registro_clientes.md`. Verificado con `curl` contra
el backend real al cierre de cada sub-fase.

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

## 2. Esquema de base de datos

| Migración | Qué hace |
|---|---|
| `V10__crea_catalogo_estatus.sql` | Crea `estatus` (id, valor único, descripción, es_sistema, soft delete). Siembra los 4 estatus de sistema con IDs fijos (1=ACTIVO, 2=SUSPENDIDO, 3=ACTUALIZADO, 4=ELIMINADO). Agrega `clientes.estatus_id` (FK) y lo rellena desde el `estado_cliente` existente. |
| `V11__permiso_estatus_administrar.sql` | Agrega el permiso `ESTATUS_ADMINISTRAR` al catálogo configurable y se lo da a ADMIN. |
| `V14__extiende_vehiculos.sql` | La tabla `vehiculos` ya existía (vacía) desde la Fase 01. Se le agrega `numero_serie`, `version`, `estatus_id` (FK), `marca_api_id`, `modelo_api_id`, `creado_por_id`, `actualizado_en`, `eliminado_en`; `vin` pasa a `NOT NULL`. |

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
| `GET /api/clientes/{id}/vehiculos` | Quien ve clientes | Vehículos de ese cliente (pestaña Vehículos del detalle de cliente). |
| `GET /api/vehiculos`, `/verificar-vin`, `/verificar-placa` | Quien ve clientes | Lista paginada (solo consulta) y chequeos de disponibilidad. |
| `GET /api/clientes/catalogos/vehiculos/{marcas,modelos,vin/{vin}}` | Quien ve clientes | Proxy a NHTSA vPIC, nunca se llama directo desde el navegador. |

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

## 7. Deuda técnica / pendientes

- No hay pantalla para editar/eliminar vehículos todavía (solo registrar y consultar).
- `Vehiculo.actualizadoEn`/`eliminadoEn` quedaron listos en el modelo pero sin ningún flujo
  que los use (no hay "editar" ni "eliminar" vehículo en esta fase).
- El Flyway sigue sin correr solo con `mvn spring-boot:run` (problema heredado de fases
  anteriores, documentado desde `registro_Fase02.md`): toda migración de V10 a V14 se
  aplicó a mano.
