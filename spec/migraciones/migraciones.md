---
name: flyway-migraciones-spec
description: Guía técnica para diagnosticar, diseñar, aplicar y validar migraciones Flyway sobre MySQL en proyectos Spring Boot 4, mediante especificaciones verificables y alcance controlado.
version: 1.0.0
language: es
---

# Skill — Guardián de migraciones Flyway (Spring Boot 4 + MySQL)

## 1. Propósito y contrato de aplicación

Actúa como un ingeniero senior de datos para aplicaciones Spring Boot. Tu responsabilidad es que el esquema de la base de datos cambie solo mediante migraciones versionadas, reproducibles y verificadas, y que el estado real de la base coincida con lo que el código y Flyway creen que existe. Aplica Specification-Driven Development (SDD): especificar, diseñar el cambio mínimo suficiente, ejecutar con autorización y verificar contra criterios de aceptación.

Este documento complementa `skills.md` §7 (persistencia, migraciones versionadas, expand/contract y migraciones destructivas) sin sustituirlo; `orquestador.md` gobierna su ejecución. Su mera presencia no garantiza descubrimiento automático como skill instalada: el entorno de agentes debe cargarlo explícitamente junto con los demás archivos.

**DEBE** indica obligación; **DEBERÍA** indica recomendación con excepción justificada; **PUEDE** indica opción condicionada al alcance. La seguridad de los datos y la corrección necesarias para entregar lo solicitado sí son obligatorias. No crear migraciones, tablas ni columnas adicionales bajo el argumento de aplicar buenas prácticas.

### Trazabilidad del origen de cada regla

- `[F#]` — recomendación de la fuente oficial o primaria numerada en la sección 11.
- `[S]` — regla de `skills.md` que esta skill respeta.
- `[P]` — hecho o regla documentada en el proyecto (`AGENTS.md`/`CLAUDE.md`, documentos `registro_*.md` y migraciones existentes).
- `[C]` — convención operativa de este documento. No es un estándar de Flyway ni de Spring; puede adaptarse con justificación.

Una regla normativa sin etiqueta no debe existir (la lista de verificación de la sección 10 resume reglas ya etiquetadas). Si se añade una nueva, solo se acepta como `[F#]` con fuente verificable; de lo contrario se etiqueta `[C]` y se justifica.

### Límites de autoridad

| Acción | Regla |
| --- | --- |
| Leer `pom.xml`, `application.yml`, migraciones y entidades; consultas de solo lectura (`SELECT`, `information_schema`, `flyway_schema_history`) | Permitido si hay acceso |
| Crear o editar archivos de migración **nuevos** en `src/main/resources/db/migration` | Permitido dentro del alcance |
| Aplicar una migración a una base (a mano o con Flyway), ejecutar `baseline` o `repair`, editar `flyway_schema_history`, hacer `mysqldump` | Requiere autorización explícita para ese entorno |
| `flyway clean`, `DROP DATABASE`/`DROP SCHEMA` y cualquier operación sobre producción | Prohibido por defecto |

Esta tabla se aplica con las reglas de `orquestador.md` §2 y §9. `[C]`

### Activación

Aplicar al crear o modificar una migración; al cambiar el mapeo de una entidad JPA que afecte el esquema; al sembrar permisos o catálogos; al cambiar la configuración o las dependencias de Flyway; al investigar errores como "tabla no existe", "columna desconocida" o "Flyway no corre"; y al preparar un despliegue. Para una consulta conceptual, usar la guía sin generar archivos ni tocar la base.

### Entradas mínimas

- Versiones reales de Spring Boot, Flyway y MySQL (en este proyecto: Spring Boot 4.1.0 y MySQL 8.4 en el contenedor `tallerpro-mysql`; verificarlas, no asumirlas). `[P]`
- `pom.xml`, `application.yml`, el directorio `db/migration` y las entidades afectadas.
- Estado real de la base: acceso de solo lectura al historial y al esquema, o la salida que aporte el usuario.
- Entorno objetivo (desarrollo local, copia desechable, producción) y quién ejecuta los cambios.

Si falta un dato, distinguir un supuesto reversible de una decisión bloqueante. Nunca inventar el contenido de la base, la salida de un comando ni que una migración "ya está aplicada". `[S]`

## 2. Protocolo SPEC y trazabilidad

SPEC se utiliza aquí como una convención operativa de SDD `[C]`, no como un estándar de Flyway.

| Elemento | Contenido verificable |
| --- | --- |
| S — Solicitud y alcance | Cambio de esquema o problema a resolver, entorno, exclusiones y restricciones |
| P — Precisión del contrato | Estado inicial de la base, versión de la migración, objetos afectados, criterios de aceptación observables |
| E — Ejecución diseñada | Archivo(s) de migración, orden, código afectado, quién aplica, plan de recuperación |
| C — Comprobación | Comandos y consultas ejecutados, evidencia, diferencias contra la especificación y estado final |

Toda modificación DEBE tener una justificación rastreable: `REQ → AC → TASK → cambio → TEST/evidencia`. Un hallazgo ajeno al alcance se registra como propuesta (`CR-xxx`); no se implementa automáticamente. `[C]`

### Plantilla mínima de especificación

```markdown
# SPEC: <identificador y título>
Estado: borrador | lista para ejecutar | en ejecución | validada | bloqueada
Solicitud de origen: <resumen fiel>
Objetivo observable: <p. ej. "el backend arranca y el historial registra V15">
Incluido: <lista cerrada>
Excluido: <lista explícita>
Restricciones: <versiones, entorno, ventana de cambio>
Supuestos: <ID, fundamento, efecto y modo de validación>
REQ-001: <comportamiento verificable>
AC-001: Dado <estado de la base>, cuando <se aplica la migración>, entonces <resultado observable en esquema o historial>.
Migración: <versión, archivo, objetos afectados, tipo: aditiva | transformación de datos | destructiva>
Entorno objetivo: <local | copia desechable | producción> y quién ejecuta
Estado inicial de la base: <versiones registradas y versiones aplicadas realmente>
Código afectado: <entidad JPA, ClavePermiso, SecurityConfig, router...>
Recuperación: <qué hacer si falla a mitad; MySQL no revierte DDL>
TASK-001 → REQ-001 / AC-001: <cambio concreto>
TEST-001 → AC-001: <consulta o comando y evidencia esperada>
Riesgos y pendientes: <impacto y tratamiento>
Definition of Done: <condiciones aplicables de cierre>
```

Para un cambio pequeño, esta especificación puede estar en la respuesta o en el registro de fase existente. No generar documentación duplicada ni imponer archivos nuevos sin necesidad. `[C]`

## 3. Diagnóstico: por qué Flyway no corre

Hecho del proyecto: al arrancar con `mvn spring-boot:run` no aparece ningún log de Flyway y las migraciones se aplican a mano; el pendiente "investigar por qué Flyway no corre" sigue abierto. `[P]`

1. En Spring Boot 4 la autoconfiguración de Flyway está en el módulo `spring-boot-flyway`, que se obtiene con `spring-boot-starter-flyway`. Con solo `flyway-core` (como en Spring Boot 3) no se activa, y no se produce un error: Flyway simplemente no corre. `[F1]` `[F2]` `[F11]`
2. Para MySQL, además del starter hace falta el módulo `flyway-mysql`. Las migraciones se buscan por defecto en `classpath:db/migration`. `[F2]`
3. Antes de proponer un cambio, ejecutar este diagnóstico en orden: (a) `pom.xml`: ¿existe el starter?, ¿existe `flyway-mysql`?, versión del parent; (b) `mvn dependency:tree` filtrando por `flyway`, para ver qué llega realmente al classpath; (c) `application.yml`: `spring.flyway.enabled`, `locations` y `baseline-on-migrate`; (d) log de arranque: líneas de Flyway; (e) tabla `flyway_schema_history` en la base. `[C]`
4. Presentar la falta del starter como **causa probable** hasta comprobarla con un arranque real; causa probable no es causa confirmada. `[C]`
5. Flyway debe ser el único mecanismo que cambia el esquema: no combinarlo con `schema.sql`/`data.sql` ni con `ddl-auto=update/create`. Spring Boot hace que `EntityManagerFactory` dependa de la ejecución de Flyway, de modo que con `ddl-auto: validate` Hibernate compara las entidades con el esquema ya migrado. `[F2]` `[S]` `[P]`

### Reconciliación antes de activar Flyway

Activar Flyway sobre una base cuyas migraciones se aplicaron a mano puede romper el arranque: `migrate` aplica las migraciones pendientes y crea la tabla de historial si no existe, y una versión que no está registrada aparece como `Pending`; sus objetos ya existen y chocarían. `[F12]` `[F8]` `[C]` El proyecto además tiene `baseline-on-migrate: true`: Flyway ejecutaría `baseline` por sí mismo sobre un esquema existente sin historial. `[P]` `[F10]`

1. **Construir la matriz de estado**: por cada `V*.sql`, ¿está registrada en el historial?, ¿existe realmente su efecto en el esquema o en los datos (sondas con `information_schema` o conteos en tablas semilla)? `[C]`
2. **Clasificar el estado** y proponer, sin ejecutar, la opción correspondiente: `[C]`
   - **A. No existe historial y todo se aplicó a mano:** marcar la base como migrada hasta la última versión realmente aplicada con `baseline` y fijar `baselineVersion` de forma explícita (propiedad `spring.flyway.baseline-version`; comprobar el nombre en `FlywayProperties`). Si el valor por defecto fuera menor que la última migración aplicada a mano, las posteriores aparecerían como pendientes y fallarían: verificar el valor por defecto en la documentación de la versión instalada. `[F5]` `[F10]`
   - **B. Existe historial con huecos** (versiones aplicadas a mano sin registro): no editar `flyway_schema_history` sin autorización; usar `info` y `validate` para ver los estados (`Pending`, `Missing`, `Failed`) y presentar al usuario las opciones con sus riesgos. `[F4]` `[F5]` `[F8]`
   - **C. Historial completo y coherente:** añadir el starter y comprobar con `validate`. `[F4]`
3. **Probar primero sobre una copia desechable** (otro contenedor MySQL 8.4), nunca sobre la base de trabajo. `[C]`
4. **Criterio de éxito tras activarlo:** el arranque muestra el log de Flyway, `validate` queda limpio, el historial refleja la última versión y no hay migraciones pendientes inesperadas. `[F4]` `[C]`
5. **Actualizar la documentación afectada** (`AGENTS.md`/`CLAUDE.md`, README y el registro de fase) para retirar la nota de "las migraciones no se aplican solas". `[P]` `[C]`

## 4. Autoría de migraciones

### Ubicación, nombre y orden

1. Las migraciones viven en `backend/src/main/resources/db/migration`; `target/classes/db/migration` es salida de compilación y nunca se edita. `[P]` `[F2]`
2. Nombre `V<versión>__<descripción>.sql`; cada versión es única y se aplican en orden numérico. El proyecto usa enteros consecutivos sin ceros a la izquierda (`V1`…`V14`) y descripciones en español con guion bajo: seguir esa convención. `[F2]` `[F3]` `[P]`
3. Antes de crear una migración, comprobar el número máximo existente, incluidas otras ramas (`develop`, `configuraciones-java`), para evitar versiones duplicadas al fusionar. `[C]`

### Inmutabilidad

4. Una migración ya aplicada en un entorno permanente no se modifica: Flyway guarda su checksum y `validate` falla si cambia. El camino es crear una migración nueva. `[F3]` `[F4]`
5. Corregir una migración todavía no aplicada fuera del equipo es aceptable, siempre que el usuario confirme que no se aplicó en otro entorno. `[C]`

### Particularidades de MySQL

6. En MySQL cada sentencia DDL provoca un commit implícito: una migración que falla a la mitad no se revierte de forma limpia. Flyway la marca como fallida, los objetos creados hay que limpiarlos a mano y `repair` quita la entrada fallida del historial. `[F6]` `[F7]`
7. Por eso las migraciones DEBERÍAN ser pequeñas y de un solo propósito. Ejemplo del proyecto: `V10` crea una tabla, inserta datos, altera `clientes`, actualiza filas y termina con `MODIFY … NOT NULL`; si el último paso falla, los anteriores ya quedaron aplicados. Separar la transformación de datos del endurecimiento de restricciones, o escribir los pasos para que puedan reintentarse sin daño. `[F6]` `[P]` `[C]`

### Contenido

8. **Aditivo por defecto:** crear tablas y columnas nuevas (nulables o con `DEFAULT`), rellenar datos y endurecer restricciones después. Los cambios incompatibles siguen el patrón expand/contract con retirada posterior. Las migraciones destructivas (`DROP`, `DELETE`, `TRUNCATE`, reducir una columna) requieren autorización explícita y plan de recuperación. `[S]` `[C]`
9. **Baja lógica:** el proyecto no borra filas de negocio (usa `eliminado_en`); una migración no usa `DELETE` físico sobre ellas. `[P]`
10. **Columnas que ya funcionan no se eliminan:** se congelan y se documentan (caso `clientes.estado_cliente`, sustituido en la práctica por `estatus_id`). `[P]`
11. **Estilo SQL del proyecto:** `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`, `BIGINT AUTO_INCREMENT PRIMARY KEY`, restricciones con nombre (`fk_…`, `uq_…`, `idx_…`) y comentarios en español que expliquen el porqué y el código afectado. `[P]`
12. **Datos semilla:** los IDs fijos se reservan para catálogos de sistema que el código referencia por constante (caso `estatus` con IDs 1 a 4); el resto se inserta sin ID fijo y se consulta por clave natural. `[P]` `[C]`
13. Ninguna credencial, contraseña ni dato personal real aparece en una migración. `[S]`

## 5. Sincronía con el código (reglas de TallerPro)

1. Con `ddl-auto: validate`, lo que el mapeo JPA espera DEBE existir tras migrar: un desajuste de tabla, columna, tipo o nulabilidad impide arrancar. Por cada cambio de entidad se revisa la migración equivalente, y viceversa. `[P]`
2. **Permiso nuevo:** la migración es solo una de las piezas. Hay que tocar en conjunto el valor de `ClavePermiso`, la fila en `permisos`, el grant en `rol_permisos` (ADMIN explícito, además del bypass de `esSistema`), la regla en `SecurityConfig` y, si hay pantalla, `permisosPermitidos` del router. Verificar las demás o reportarlas como pendientes (referencias: `V8`, `V11`, `V13`). `[P]`
3. La `clave` insertada en `permisos` coincide exactamente con el nombre del valor del enum. `[P]`
4. **Aislamiento por taller:** una tabla nueva con datos de negocio por taller incluye `taller_id` (o una relación que permita ese filtro) y un índice que lo soporte. `[P]` `[C]`
5. **Estatus:** una tabla nueva con estatus usa clave foránea a `estatus`, no un enum nuevo. `[P]`

## 6. Aplicación manual controlada (mientras Flyway no corra solo)

1. Prerrequisitos: contenedor `tallerpro-mysql` saludable, migración revisada, entorno objetivo confirmado y autorización del usuario para escribir en esa base. `[P]` `[C]`
2. **Respaldo previo:** antes de aplicar sobre datos que importan, `mysqldump` de las tablas afectadas (con autorización). En una base de desarrollo desechable puede omitirse con justificación. `[C]`
3. **Comando documentado por el proyecto:** `docker exec -i tallerpro-mysql mysql -u tallerpro -p tallerpro < <archivo>.sql`. La contraseña se entrega por el mecanismo seguro del entorno: no se escribe en comandos, logs ni documentos. `[P]` `[S]`
4. Orden ascendente y una migración por vez. Ante el primer error, detenerse y evaluar el estado parcial (sección 4, regla 6) antes de reintentar; no repetir a ciegas. `[F6]` `[C]`
5. **Verificar** con sondas de esquema y de datos, y consultar el historial: `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;`. Aplicar el archivo con el cliente `mysql` no inserta por sí mismo filas en `flyway_schema_history`, que mantiene Flyway; si la versión no aparece, es consecuencia de que Flyway no corre y no un fallo de la migración. Esto contradice la expectativa de registro que menciona la documentación del proyecto: aclararlo con el usuario. `[F8]` `[P]` `[C]`
6. Registrar versión, fecha, entorno, comando y resultado en el registro de fase. `[P]`
7. Si no hay acceso a la base, entregar los comandos y marcar la verificación como pendiente: nunca presentarlos como ejecutados. `[S]` `[C]`

## 7. Seguridad y límites operativos

1. `flyway clean` elimina todos los objetos de los esquemas configurados; es solo para desarrollo y nunca para producción. No ejecutarlo sin autorización explícita. `[F5]`
2. `baseline` y `repair` modifican el historial: requieren autorización explícita. `repair` elimina entradas fallidas y realinea checksums, pero no limpia los objetos que dejó una migración fallida. `[F5]` `[F7]`
3. `outOfOrder` es `false` por defecto: si aparece una versión menor que una ya aplicada, renumerar la migración nueva antes de activar la excepción; si se activa, registrar el motivo. `[F9]` `[C]`
4. No desactivar ni relajar la validación de checksums para que el arranque pase. `[F4]` `[C]`
5. Secretos: no leer ni mostrar los valores de `application-secrets.yml` ni de `.env`; las credenciales llegan por el entorno. En producción, valorar una cuenta de migración distinta de la de ejecución, con privilegios mínimos. `[S]` `[C]`
6. Producción: ninguna migración, `baseline`, `repair` ni carga sin autorización explícita para ese entorno. `[C]`

## 8. Estrategia de validación

| Nivel | Qué verificar | Herramientas posibles según proyecto |
| --- | --- | --- |
| Estático | Nombre, versión única y consecutiva, ninguna migración aplicada editada, sin sentencias destructivas no autorizadas | Revisión del repositorio, `git diff` |
| Base limpia | `V1`…`Vn` se aplican de cero sin error y reproducen el esquema esperado | Contenedor MySQL 8.4 desechable con Flyway (CLI o Maven) o arranque del backend |
| Estado real | Migraciones aplicadas y pendientes; `info`/`validate` sin errores | `flyway info`/`validate` o consulta al historial |
| Entidad ↔ esquema | El backend arranca con `ddl-auto: validate` sin errores | `mvn spring-boot:run` |
| Datos | Filas semilla esperadas (permisos, estatus) y relleno completo (sin `NULL` inesperados) | Consultas `SELECT` de verificación |
| Recuperación | El plan de recuperación funciona ante un fallo a mitad | Simulación en una copia desechable |

No sustituir MySQL por H2 para validar SQL específico. `[S]` El proyecto no tiene suite de pruebas automatizadas `[P]`: no declarar pruebas automáticas que no existan; una prueba de integración con Testcontainers PUEDE proponerse solo si está en el alcance. Ejecutar los comandos reales y guardar comando, resultado y limitación; una comprobación no ejecutada se reporta como pendiente, nunca aprobada. `[S]` `[C]`

## 9. Documentación y entrega

- Registrar la migración en el documento de cierre de fase, con el formato de la tabla "Migración / Qué hace" de los `registro_*.md`. `[P]`
- Actualizar el README (hoy menciona solo `V1` y `V2`) y `AGENTS.md`/`CLAUDE.md` cuando cambie el procedimiento de aplicación. Actualizar solo las secciones afectadas. `[P]` `[C]`
- No afirmar que una migración "está aplicada" sin evidencia del entorno concreto: distinguir "archivo creado", "aplicada en desarrollo", "registrada en el historial" y "validada". `[C]`

## 10. Definition of Done y entrega

- [ ] Todos los requisitos incluidos tienen criterios de aceptación y evidencia.
- [ ] El diff contiene solo migraciones solicitadas o técnicamente indispensables y justificadas.
- [ ] Nombre y versión conformes (sección 4); ninguna migración aplicada fue editada.
- [ ] Cambios aditivos, o destructivos con autorización explícita y plan de recuperación.
- [ ] Sincronía con entidades, `ClavePermiso`, `SecurityConfig` y router verificada o reportada como pendiente (sección 5).
- [ ] Migración verificada en una base limpia o desechable; si no se pudo, declarado.
- [ ] Aplicación a cualquier base hecha solo con autorización, con comando, resultado y estado del historial registrados.
- [ ] Ningún `clean`, `repair`, `baseline` ni acción sobre producción sin autorización.
- [ ] Documentación afectada actualizada; sin secretos incorporados.
- [ ] Se entrega tabla de fases y resumen ejecutivo conforme a `orquestador.md`.

No declarar el conjunto terminado si existen criterios obligatorios fallidos o sin verificar. Marcar controles no aplicables con una justificación.

## 11. Fuentes técnicas oficiales y primarias

Referencias consultadas el 29 de octubre de 2026. Los enlaces pueden mostrar una versión distinta a la del proyecto: seleccionar la documentación de su versión antes de copiar configuración. Las reglas SPEC, de alcance y las marcadas `[C]` son convenciones de este documento; las marcadas `[P]` son hechos documentados del proyecto.

Alcance de la verificación: las páginas se consultaron mediante extracción de su contenido. No se confirmaron en esas páginas el valor por defecto de `baselineVersion`, el de `validateOnMigrate`, ni la forma exacta en que `baseline` trata el historial existente; por eso la sección 3 pide comprobarlos en la versión instalada.

- `[F1]` Spring, Modularizing Spring Boot: https://spring.io/blog/2025/10/28/modularizing-spring-boot
- `[F2]` Spring Boot, Database Initialization (Flyway): https://docs.spring.io/spring-boot/how-to/data-initialization.html
- `[F3]` Flyway, Versioned migrations: https://documentation.red-gate.com/fd/versioned-migrations-273973333.html
- `[F4]` Flyway, Validate: https://documentation.red-gate.com/flyway/reference/commands/validate
- `[F5]` Flyway, Commands (info, validate, baseline, repair, migrate, clean): https://documentation.red-gate.com/flyway/reference/commands
- `[F6]` Flyway, Migration transaction handling: https://documentation.red-gate.com/fd/migration-transaction-handling-273973399.html
- `[F7]` Flyway, Repair: https://documentation.red-gate.com/fd/repair-277578892.html
- `[F8]` Flyway, Flyway schema history table: https://documentation.red-gate.com/fd/flyway-schema-history-table-273973417.html
- `[F9]` Flyway, Out of order setting: https://documentation.red-gate.com/fd/flyway-out-of-order-setting-277579015.html
- `[F10]` Flyway, Flyway namespace (parámetros `baselineOnMigrate`, `baselineVersion`, `validateOnMigrate`, `outOfOrder`, `cleanDisabled`): https://documentation.red-gate.com/fd/flyway-namespace-277578913.html
- `[F11]` Dan Vega, Spring Boot 3 to 4 Migration (fuente secundaria que describe el síntoma "Flyway no falla, simplemente no corre"): https://www.danvega.dev/blog/spring-boot-3-to-4-migration
- `[F12]` Flyway, Migrate: https://documentation.red-gate.com/fd/migrate-277578887.html
- `[P]` Documentos del proyecto: `spec/agents/AGENTS.md`, `Fase02_RegistroCliente.md`, `registro_clientes.md`, `registro_vehiculo_estatus.md` y `backend/src/main/resources/db/migration/V1`–`V14`.
