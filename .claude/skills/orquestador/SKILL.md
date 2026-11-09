---
name: orquestador
description: Orquestador Spring Boot + Vue para TallerPro. Úsala para cualquier tarea de implementación, corrección, optimización, migración de base de datos o documentación sobre el backend (Spring Boot/Java) o el frontend (Vue 3) de este proyecto. Coordina el flujo análisis → diseño → desarrollo → validación → entrega con alcance cerrado (SPEC, REQ/AC/TASK, control de scope creep) y carga bajo demanda las skills de backend, frontend, migraciones y documentación según corresponda.
---

Lee por completo `spec/orquestador/orquestador.md` (ruta relativa a la raíz del repositorio)
con la herramienta de lectura de archivos y síguelo al pie de la letra durante toda esta
tarea: es el contrato de ejecución completo (reglas no negociables, ficha del prompt,
control de alcance, las 5 fases con sus gates, formato de entrega y autoverificación).

No resumas ni parafrasees ese archivo de memoria — ábrelo siempre, porque es la fuente
de verdad y puede haber cambiado desde la última vez. Sigue exactamente lo que diga,
incluyendo su propia tabla "Selección de skill por tipo de tarea": esa tabla te dirá
cuándo cargar también, bajo demanda y nunca todas a la vez:

- `spec/skills/skills.md` — backend Spring Boot / API REST.
- `spec/frontend/frontend.md` — frontend Vue 3.
- `spec/migraciones/migraciones.md` — migraciones Flyway / cambios de esquema.
- `spec/documentacion/documentacion-backend.md` y/o
  `spec/documentacion/documentacion-frontend.md` — documentación de esa capa (recuerda:
  `orquestador.md` las exige también por defecto al cerrar cualquier tarea que cambie
  código de esa capa, no solo cuando se pida documentación aparte).

Todas las rutas anteriores son relativas a la raíz del repositorio (no a esta carpeta de
la skill). Si `spec/orquestador/orquestador.md` no existe en este checkout, dilo
explícitamente y no sigas: no imitar su contenido de memoria ni inventar un sustituto.
