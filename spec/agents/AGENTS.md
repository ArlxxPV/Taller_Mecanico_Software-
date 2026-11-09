# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

TallerPro: gestión de talleres mecánicos (clientes, vehículos, personal, roles). Backend Spring Boot 4.1 (Java 25) + MySQL 8.4 vía Docker; frontend Vue 3 + Vite + Tailwind + PrimeVue.

## Commands

**Prerequisito (una sola vez):** copiar `backend/src/main/resources/application-secrets.yml.example` → `application-secrets.yml` (credenciales MySQL + JWT secret) y `frontend/.env.example` → `.env`. Ambos están en `.gitignore`, nunca se suben.

```bash
# MySQL (contenedor Docker, debe estar healthy antes de levantar el backend)
docker ps --filter "name=tallerpro-mysql"
docker start tallerpro-mysql   # si no está corriendo

# Backend — puerto 8081
cd backend
mvn -q compile          # recompilar tras cualquier cambio de código Java
mvn spring-boot:run     # arrancar

# Frontend — puerto 5173
cd frontend
npm install
npm run dev
npm run build            # build de producción (vite build), úsalo para validar que compila sin errores
```

No existe suite de pruebas automatizadas todavía: el backend trae `spring-boot-starter-test`/`spring-security-test` en el `pom.xml` pero no hay ninguna clase de test; el frontend no tiene ningún runner de tests configurado. Validar cambios con `mvn -q compile` / `npm run build` y pruebas manuales (`curl` contra la API, navegador contra Vite).

**Tras cualquier cambio en código Java, hay que recompilar y reiniciar el proceso del backend** — `mvn spring-boot:run` no hace hot-reload de clases.

**Migraciones Flyway no se aplican solas**: aunque `spring.flyway.enabled: true`, en este entorno `mvn spring-boot:run` no corre Flyway al arrancar (no aparece ningún log `Flyway`). Cada migración nueva (`backend/src/main/resources/db/migration/V*.sql`) se debe aplicar a mano:

```bash
docker exec -i tallerpro-mysql mysql -u tallerpro -p tallerpro < backend/src/main/resources/db/migration/V15__xxx.sql
```

Verificar con `docker exec tallerpro-mysql mysql -u tallerpro -p tallerpro -e "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"` que la versión quedó registrada.

## Architecture

### Backend — capas

`controller/` → `service/` → `repository/` (Spring Data JPA) → `model/` (entidades JPA). Los DTOs de entrada/salida son `record`s en `dto/`. Cada controller resuelve el actor autenticado desde el `Authentication` de Spring Security con un helper privado `actorAutenticado(authentication)` que busca el `Usuario` completo por email (el principal de Spring Security solo tiene credenciales/authorities, no la entidad de negocio) — toda la lógica de permisos y aislamiento por taller necesita ese `Usuario`.

**Jackson**: Spring Boot 4.1 usa Jackson 3, cuyo paquete es `tools.jackson.databind.*`, no `com.fasterxml.jackson.databind.*` (ese sigue en el classpath transitivamente por jjwt, pero no es el bean que gestiona Spring). Si necesitas un `ObjectMapper` inyectado, importa `tools.jackson.databind.ObjectMapper`.

### Permisos configurables (no roles fijos)

Los roles (`Rol`, tabla `roles`) son texto libre, no un enum — se crean desde la pantalla de Roles. Lo que protege cada endpoint es un **permiso** (`ClavePermiso`, enum fijo en `model/ClavePermiso.java`, respaldado por la tabla `permisos` y la relación `rol_permisos`), nunca el nombre del rol directamente:

- `AppUserDetailsService` construye, por cada login, las `GrantedAuthority` reales: `ROLE_<nombre>` por cada rol, más `PERM_<CLAVE>` por cada permiso de sus roles activos. Un rol con `esSistema=true` (solo ADMIN) recibe automáticamente **todas** las `PERM_*` posibles, sin leer `rol_permisos` — es el bypass que garantiza que ADMIN nunca pierda acceso por una mala configuración.
- `SecurityConfig` protege rutas con `hasAuthority("PERM_...")`/`hasAnyAuthority(...)`, nunca con `hasRole`. El orden de las reglas importa: las más específicas (un método+path concreto) van antes que la regla general de ese mismo prefijo.
- Dentro de un service, la misma pregunta se hace con `PermisoService.tiene(actor, ClavePermiso.X)` (mismo bypass por `esSistema`) — se usa para lógica que no es solo "permitir/rechazar la ruta", como decidir si un filtro de taller aplica.
- **Al agregar un endpoint protegido nuevo**: si ningún permiso existente le queda bien, agrega un valor a `ClavePermiso`, una fila en el seed de `permisos` (migración), el grant correspondiente en `rol_permisos` para los roles que deban tenerlo, y la regla en `SecurityConfig`. Reusar un permiso existente es válido cuando el endpoint nuevo pertenece al mismo actor/alcance que ya protegía ese permiso (p. ej. "asociar vehículo a un cliente" reusa `CLIENTES_CREAR`/`CLIENTES_EDITAR`).

### Aislamiento por taller

Un actor sin el permiso `CLIENTES_TODOS_TALLERES` solo puede ver/operar clientes (y, por extensión, vehículos, historial, panel) de su propio `actor.getTallerId()`; el filtro se ignora/fuerza en el service, no en el controller. Ver el patrón en `ClienteService` (métodos `listar`, `resumen`, `obtener`/`buscarAccesibleOFallar`).

### Baja lógica (soft delete), nunca DELETE físico

Clientes, vehículos y estatus nunca se borran de verdad: "eliminar" llena una columna `eliminado_en`. Las transiciones de estatus (suspender/reactivar/eliminar/restaurar) usan `UPDATE`s nativos condicionados (`@Modifying(clearAutomatically = true, flushAutomatically = true)` en el repository, con `@Query(nativeQuery = true)`) que solo afectan la fila si todavía está en el estatus esperado — evita condiciones de carrera entre leer y escribir el estatus, y decide 404 vs 409 según si la fila existía pero no cumplía la condición. `clearAutomatically = true` es necesario: sin él, Hibernate devuelve el estado cacheado de primer nivel después del `UPDATE` nativo.

Cada cambio de estatus de cliente queda además registrado en `historial_estatus_cliente` (estatus anterior/nuevo, motivo, usuario, y para ediciones un diff en JSON `{"Campo": ["anterior","nuevo"]}` calculado comparando entidad-antes vs. datos-nuevos — ver `ClienteService.diferencias()`).

`clientes.estatus_id` (FK a la tabla configurable `estatus`) es la fuente de verdad real del estatus de un cliente; el enum Java `EstadoCliente`/columna `estado_cliente` sigue existiendo pero está **congelado** (nunca se le vuelve a escribir) porque borrarlo violaría la regla de no romper columnas que ya funcionan. El campo JSON `estadoCliente` que devuelve la API mantiene su nombre/formato de siempre por compatibilidad.

### Frontend — repository/facade

Cada recurso tiene un `repository` (`repositories/*.js`, única capa que arma la petición Axios/`FormData`, sin lógica) y un `facade` (`facades/*.js`, que llama al repository y traduce la respuesta a un contrato uniforme `{ ok, mensaje, detalles }` o `{ ok, ...datos }`, nunca deja que un componente vea un error de Axios crudo). `services/http.js` es la instancia única de Axios: adjunta el JWT guardado en `localStorage` a cada request y limpia la sesión local si el backend responde 401.

Formularios con archivo (cliente, taller) van por `multipart/form-data` con dos partes: `datos` (Blob JSON) y `foto` (opcional) — ver `clienteRepository.crear`/`actualizar` como referencia.

### Rutas y layout

`router/index.js`: todo lo que vive dentro del panel (`/panel/...`) es hijo de `AppLayout.vue` (sidebar + topbar + breadcrumb compartidos), con el breadcrumb declarado por ruta en `meta.breadcrumb`. El guard `beforeEach` autoriza por `meta.rolesPermitidos` (contra `auth.rolesUsuario`) o `meta.permisosPermitidos` (contra `auth.permisosUsuario`, el equivalente en frontend del `PERM_*` del backend) — si el perfil no se cargó todavía (navegación directa a una URL), lo pide con `authFacade.cargarPerfil()` antes de decidir.

**Convención del proyecto**: nunca usar `Dropdown`/`Autocomplete` de PrimeVue; selección simple va con `<select>` nativo, autocompletado libre (marca/modelo de vehículo) con `<input list>` + `<datalist>` nativos. PrimeVue se usa solo para `Toast` y `Dialog`.

### Integración externa: NHTSA vPIC

`CatalogoVehiculosService` consulta la API pública NHTSA vPIC (marcas/modelos/decodificación de VIN) con `RestClient`, cacheada en memoria (`ConcurrentHashMap`, TTL de 7 días) para no depender de su disponibilidad en cada request; si la API falla, se devuelve la caché aunque esté vencida, o vacío — nunca se rompe el formulario. Mismo patrón que `DireccionCatalogoService` usa para el catálogo de colonias/CP (Postali).
