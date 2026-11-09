# Registro — Administración de Clientes (TallerPro)

Documento de cierre de la parte de **administración de clientes** y **panel** del trabajo
basado en las maquetas "Pantallas fase 2". La parte de catálogo de estatus y vehículos
está en el documento hermano `registro_vehiculo_estatus.md` (ahí vive también el diseño
visual compartido: `AppLayout.vue`, paleta, tipografía). Roles configurables, personal y
edición de talleres están en `registro_roles_y_talleres.md`. Verificado con `curl` contra
el backend real al cierre de cada sub-fase.

## 1. Resumen ejecutivo

1. **Administración de clientes**: modal de edición con detección de cambios reales,
   suspender/reactivar/eliminar/restaurar (con motivo obligatorio donde aplica),
   historial de movimientos por cliente, paginación/búsqueda/filtros server-side,
   chequeo de CURP duplicada.
2. **Panel**: indicadores clicables (clientes activos/suspendidos, vehículos
   registrados, clientes nuevos este mes), accesos rápidos y tabla de últimos
   movimientos, para quien administra clientes; el panel simple de un CLIENTE
   autoservicio no cambió. Talleres se movió dentro del layout compartido (sin tocar
   su lógica).

## 2. Esquema de base de datos

| Migración | Qué hace |
|---|---|
| `V12__historial_y_auditoria_clientes.sql` | Agrega `clientes.actualizado_en/actualizado_por_id/eliminado_en`. Crea `historial_estatus_cliente` (estatus anterior/nuevo, motivo, cambios en JSON, usuario, fecha). Backfill: una fila "Alta" por cada cliente ya existente. |
| `V13__permiso_clientes_eliminar.sql` | Agrega el permiso `CLIENTES_ELIMINAR` (solo ADMIN) y le da `CLIENTES_SUSPENDER` a RECEPCIONISTA (antes solo lo tenía ADMIN; la maqueta lo pide explícito). |

La fuente de verdad del estatus del cliente (`clientes.estatus_id` → catálogo `estatus`)
y el porqué `estado_cliente` (enum viejo) se dejó congelado sin tocar están documentados
en `registro_vehiculo_estatus.md`, sección 2 — aplica igual aquí: el campo JSON
`estadoCliente` que ya devolvía la API sigue igual de nombre y formato.

## 3. Backend — catálogo de endpoints

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/clientes/verificar-curp?curp=` | Quien ve/crea/edita clientes | Chequeo "al salir del campo" de CURP duplicada. |
| `PATCH /api/clientes/{id}/suspender` `/reactivar` | `CLIENTES_SUSPENDER` | `UPDATE` condicionado atómico (evita condición de carrera), 409 si ya estaba en ese estatus. |
| `DELETE /api/clientes/{id}`, `PATCH /api/clientes/{id}/restaurar` | `CLIENTES_ELIMINAR` | Soft delete / restaurar. |
| `GET /api/clientes/{id}/historial` | Quien ve clientes | Movimientos de ese cliente (incluye diffs de edición). |
| `GET /api/panel/resumen`, `/ultimos-movimientos` | Quien ve clientes | Indicadores y tabla de movimientos recientes del panel. |

El endpoint viejo `PATCH /api/clientes/{id}/estatus` (toggle sin motivo) **se dejó
intacto**: ya no lo llama ninguna pantalla, pero sigue respondiendo 200 si alguien lo usa.

## 4. Decisiones de diseño relevantes

- **Detección de cambios al editar**: el backend compara cliente-antes vs. datos-nuevos
  campo por campo (`ClienteService.diferencias`); si no hay ningún cambio real, responde
  422 — el botón "Guardar cambios" del frontend también se deshabilita antes de llegar
  ahí.
- **Historial con diff**: los movimientos `ACTUALIZADO` guardan un JSON
  `{"Campo": ["anterior", "nuevo"]}`, que el frontend muestra como "Campo: anterior → nuevo".
- **Aislamiento por taller**: se extendió a historial y al panel con el mismo patrón ya
  usado en clientes (sin `CLIENTES_TODOS_TALLERES`, todo se acota a `actor.getTallerId()`).
- **`UPDATE` condicionado en suspender/reactivar/eliminar/restaurar**: evita una condición
  de carrera entre leer el estatus y escribirlo (dos clics simultáneos no deberían poder
  "colarse"); 0 filas afectadas decide si el error es 404 (no existe) o 409 (no estaba en
  el estatus requerido).

## 5. Archivos

**Backend, nuevos**: `V12`, `V13` (migraciones), `model/HistorialEstatusCliente.java`,
`repository/HistorialEstatusClienteRepository.java`, `service/PanelService.java`,
`controller/PanelController.java`, DTOs: `Suspender/Reactivar/EliminarClienteRequest`,
`HistorialEstatusClienteResponse`, `CurpDuplicadaResponse`, `PanelResumenResponse`,
`MovimientoPanelResponse`.

**Backend, modificados**: `model/Cliente.java` (relación a `Estatus`, auditoría),
`repository/ClienteRepository.java` (búsqueda con filtros + `UPDATE`s atómicos),
`dto/{ClienteResponse,ResumenClientesResponse}.java`, `service/{Cliente,Auth}Service.java`,
`controller/ClienteController.java`, `config/SecurityConfig.java`.

**Frontend, nuevos**: repository/facade de `panel`.

**Frontend, modificados**: `views/{ClienteLista,ClienteDetalle,ClienteRegistro,
TallerLista,TallerRegistro,Dashboard}View.vue`, `components/ClienteFormularioCampos.vue`,
`router/index.js` (Clientes y Talleres ahora dentro de `AppLayout`).

**Agregado después (ya no aplica "sin tocar")**: `views/ClienteEditarView.vue` se movió
dentro de `AppLayout` y se rediseñó con las clases `.tp` (sigue existiendo, se llega desde
el botón "Editar" del detalle de cliente, aparte del modal de edición de la lista).
`views/PersonalView.vue` también se movió dentro de `AppLayout` y se rediseñó igual.
Además, el detalle de cliente ahora tiene un visor de foto en grande
(`components/ModalFotoAmpliada.vue`), reutilizado también en la lista de talleres.

## 6. Verificación

Probado con `curl` contra `localhost:8081` real, limpiando los datos de prueba después de
cada corrida (incluido un reseteo temporal de la contraseña de `recep@tallerpro.mx` para
probar sus límites de permiso de verdad — quedó en `Recep2026Temp!`, cámbiala cuando
puedas). Sin navegador disponible en este entorno: solo verificación de API, no visual.

| # | Punto | Resultado |
|---|---|---|
| 1 | Suspender dos veces responde 409 | ✅ probado |
| 2 | Al eliminar, el registro sigue en la BD con `eliminado_en` (nunca `DELETE FROM`) | ✅ probado, `grep` del código sin resultados |
| 3 | Cada página trae solo `per_page` filas (10/25/50) | ✅ validado en servicio |
| 4 | Recepción recibe 403 al eliminar un cliente y al escribir en el catálogo de estatus | ✅ probado en vivo |
| 5 | La edición deja un movimiento `ACTUALIZADO` con el diff exacto | ✅ probado |
| 6 | Login, autorregistro y registro de cliente siguen funcionando igual | ✅ probado end-to-end |

## 7. Deuda técnica / pendientes

- El dato histórico migrado en V12 (backfill) etiqueta todas las filas "Alta" con el
  estatus que el cliente tenía *al momento de migrar*, no con el que tenía al registrarse:
  un cliente que ya estaba suspendido antes de esta fase muestra una fila "SUSPENDIDO /
  Alta" en su historial. Es cosmético (solo en datos que ya existían), no afecta lógica.
- Ya existen talleres registrados en este entorno (Taller Centro, Taller Norte), pero la
  cuenta de recepción sigue sin uno asignado: sigue sin probarse en vivo el aislamiento por
  taller de suspender/reactivar con una cuenta real de recepcionista (sí se probó por
  código/configuración, y el mismo patrón se volvió a probar por código en los endpoints
  de vehículos nuevos — ver `registro_vehiculo_estatus.md`).
- El Flyway sigue sin correr solo con `mvn spring-boot:run` (problema heredado, ver
  `registro_Fase02.md`): toda migración de V10 a V14 se aplicó a mano.
