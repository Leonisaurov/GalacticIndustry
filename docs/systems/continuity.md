# Sistema — Continuidad, tracker y panel

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `tools/sync-dashboard.py`, `tools/continuity.py`, `tools/continuity_io.py`, `docs/dashboard/index.html`, `dashboard.css`, `dashboard.js` |
| Documentación | `docs/dashboard/README.md`, `docs/updates/README.md`, `docs/CONCURRENCY.md`, `docs/sessions/README.md`, `docs/MASTER_PLAN.md` §18 |
| Decisiones | D-013 (actualizaciones y revisiones), D-014 (coordinación concurrente), D-016 (versiones de presentación y dashboard dinámico) |
| Tareas | T-003, T-006, T-007, UPD-001-T01…T06, UPD-002-T01…T06, UPD-005-T01 |
| Pruebas | `python3 tools/sync-dashboard.py --check`, `python3 tools/tests/continuity_test.py`, `node tools/tests/dashboard_versions_test.js`, `python3 tools/tests/dashboard_versions_test.py`, apertura del panel con `file://` |

## 1. Descripción

Infraestructura que permite a sesiones independientes recuperar el estado,
registrar su trabajo y sincronizar el panel sin un agente coordinador. El
tracker JSON es la fuente canónica; el panel y `STATUS.md` son derivados.

## 2. Estado real

| Pieza | Estado | Detalle |
|---|---|---|
| Tracker canónico | Implementado | Etapas, tareas base, decisiones, sesiones, incidencias, actualizaciones y catálogo de revisión. |
| Validador | Implementado | IDs, estados, dependencias, ciclos, documentos, aprobaciones, referencias y concurrencia. |
| `STATUS.md` | Implementado | Generado desde el tracker; incluye conteos, sesiones activas, contexto de la tarea y referencias. |
| Panel `file://` | Implementado | Datos embebidos; sin servidor, CDN ni red. Por HTTP puede releer el JSON. |
| Panel por versiones | Implementado | Selector compacto/descriptivo, línea de tiempo y plantilla dinámica; solo se renderiza la versión seleccionada. |
| Actualizaciones `UPD-XXX` | Implementado | Propuesta, revisión, aprobación, tareas propias, `displayVersion` y `groups` opcionales. |
| Escrituras atómicas | Implementado | Temporal + `fsync` + `os.replace` en `tools/continuity_io.py`. |
| Bloqueo entre sesiones | Implementado | `flock` sobre `docs/.continuity.lock`; se libera al morir el proceso. |
| Reserva de tareas | Implementado | `tools/continuity.py claim` con `assignedSessionId`. |
| Sesiones por archivo | Implementado | `docs/sessions/S-XXX.md`; la sesión se registra al abrirse con `outcome: EN_PROGRESO` y se cierra con `session.update` o `closeSession`. |
| Catálogo de revisión | Implementado | `catalog.update` vincula una actualización nueva a sus sistemas y funcionalidades. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `tools/continuity_io.py` | Bloqueo `flock`, escritura atómica, lectura JSON, raíz configurable. |
| `tools/continuity.py` | `status`, `new-session`, `claim`, `apply`, `append`, `replace`, `lock-status`. |
| `tools/sync-dashboard.py` | Valida el tracker, regenera `STATUS.md` y el bloque embebido del panel bajo bloqueo. |
| `docs/dashboard/dashboard.js` | Modelo de versiones, selector compacto/descriptivo, línea de tiempo, plantilla dinámica, filtros, búsqueda, propuestas y expedientes. |
| `docs/IMPLEMENTATION_TRACKER.json` | Estado operativo canónico. |
| `docs/STATUS.md` | Resumen derivado; nunca se edita a mano. |

## 4. Flujo

1. El agente lee `AGENTS.md`, `STATUS.md` e `INDEX.md`.
2. Abre su sesión con `continuity.py new-session` (queda activa en el tracker).
3. Reserva la tarea con `continuity.py claim`.
4. Aplica cambios al tracker con un plan JSON y `expectUpdatedAt`.
5. Ejecuta `sync-dashboard.py` (o deja que `apply` lo haga).
6. Cierra la sesión con `session.update` o `closeSession` y escribe su resumen en
   `docs/sessions/S-XXX.md`.
7. No hay un agente permanente de mantenimiento.

## 5. Decisiones aplicables

- **D-013:** plan maestro histórico + actualizaciones `UPD-XXX`; aprobación
  explícita antes de implementar.
- **D-014:** coordinación descentralizada, bloqueo breve, escritura atómica y
  detección de conflictos por tarea.
- **D-016:** la implementación inicial es la versión visual `0.1`; cada
  actualización tiene `displayVersion` estable. Una única plantilla dinámica,
  selector y línea de tiempo compartiendo selección. La numeración no cambia
  IDs internos.

## 6. Tareas relacionadas

T-003 (tracker), T-006 (panel), T-007 (validación), UPD-001 (actualizaciones y
revisiones), UPD-002 (contexto y concurrencia) y UPD-005 (dashboard por
versiones).

## 7. Pruebas existentes

- `sync-dashboard.py --check`: tracker válido, HTML y `STATUS.md` coherentes;
  valida `displayVersion`, `objective`, `meta.initialVersion` y `groups`.
- `continuity_test.py`: modificaciones independientes, conflicto en la misma
  tarea, interrupción, bloqueos, sincronización concurrente, conservación de
  trabajo ajeno, dependencias entre tareas de actualización, sesiones activas,
  cierre de sesión y versiones/grupos (17 pruebas).
- `dashboard_versions_test.js` / `.py`: renderizado en DOM mínimo de Node;
  selector, línea de tiempo, modos, contenido por versión, progreso aislado,
  ausencia de etapas inventadas, pendientes globales y propuesta pendiente.
- Apertura manual del panel con `file://`.

## 8. Problemas conocidos

- El panel no puede escribir en el proyecto desde `file://`; las propuestas y
  expedientes se descargan, no se guardan solos.
- La prueba de renderizado usa un DOM mínimo de Node, no un navegador real; una
  revisión visual sigue siendo recomendable para cambios de CSS.
- Los archivos derivados (`STATUS.md`, `index.html`) pueden quedar uno detrás de
  otro si el proceso muere entre dos escrituras; la siguiente sincronización lo
  corrige.
- Una sesión interrumpida permanece `EN_PROGRESO` hasta que otra la inspecciona
  y la cierra; no se borra automáticamente.
- `IMPLEMENTATION_LOG.md` conserva el historial anterior a S-093; las sesiones
  nuevas viven en `docs/sessions/`.

## 9. Dependencias

Depende de Python 3 y `fcntl` (Linux/macOS). No usa red, servidores, bases de
datos ni dependencias externas.
