# Sistema — Continuidad, tracker y panel

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `tools/sync-dashboard.py`, `tools/continuity.py`, `tools/continuity_io.py`, `docs/dashboard/index.html`, `dashboard.css`, `dashboard.js` |
| Documentación | `docs/dashboard/README.md`, `docs/updates/README.md`, `docs/CONCURRENCY.md`, `docs/sessions/README.md`, `docs/MASTER_PLAN.md` §18 |
| Decisiones | D-013 (actualizaciones y revisiones), D-014 (coordinación concurrente), D-016 (versiones de presentación y dashboard dinámico), D-017 (contraste obligatorio con el código real), D-037…D-039 (reconstrucción visual, tipografía y deduplicación) |
| Tareas | T-003, T-006, T-007, UPD-001-T01…T06, UPD-002-T01…T06, UPD-005-T01, UPD-006, UPD-008, UPD-009, UPD-010 |
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
| Componentes y accesibilidad | Implementado | Estados hover/foco/activo/deshabilitado, breakpoints ordenados, modales con foco atrapado y devuelto, fondo `inert` y regiones `aria-live`. |
| Actualizaciones `UPD-XXX` | Implementado | Propuesta, revisión, aprobación, tareas propias, `displayVersion` y `groups` opcionales. |
| Escrituras atómicas | Implementado | Temporal + `fsync` + `os.replace` en `tools/continuity_io.py`. |
| Bloqueo entre sesiones | Implementado | `flock` sobre `docs/.continuity.lock`; se libera al morir el proceso. |
| Reserva de tareas | Implementado | `tools/continuity.py claim` con `assignedSessionId`. |
| Sesiones por archivo | Implementado | `docs/sessions/S-XXX.md`; la sesión se registra al abrirse con `outcome: EN_PROGRESO` y se cierra con `session.update` o `closeSession`. |
| Catálogo de revisión | Implementado | `catalog.update` vincula una actualización nueva a sus sistemas y funcionalidades. |
| Contraste obligatorio | Implementado (documentación) | `AGENTS.md` y `docs/updates/README.md` exigen auditar el código real y clasificar la brecha antes de crear tareas. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `tools/continuity_io.py` | Bloqueo `flock`, escritura atómica, lectura JSON, raíz configurable. |
| `tools/continuity.py` | `status`, `new-session`, `claim`, `apply`, `append`, `replace`, `lock-status`. |
| `tools/sync-dashboard.py` | Valida el tracker, calcula `vista_derivada()`, regenera `STATUS.md` y los bloques `tracker-data` / `derived-data` del panel bajo bloqueo. |
| `docs/dashboard/dashboard.js` | Modelo de versiones, selector compacto/descriptivo, línea de tiempo, plantilla dinámica, filtros, búsqueda, propuestas, expedientes y contenedores de scroll interno con fundidos. Consume `derived-data` con respaldo local. |
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
- **D-036:** las listas largas del panel (decisiones, problemas, bloqueadas,
  aprobaciones, tareas e historiales) usan altura máxima y scroll interno con
  fundidos, barra temática y foco de teclado; no alargan la página.
- **D-037:** se conserva la carta estelar de `D-033` y se reconstruye su
  ejecución visual; se descarta la dirección industrial.
- **D-038:** sans del sistema para UI y prosa, mono para cifras, IDs, versiones y
  fechas; sin serif.
- **D-039:** una sola fuente de verdad por cifra; cabecera comprimida, readout
  rail, resumen global denso y distribución única en el bloque global.

## 6. Tareas relacionadas

T-003 (tracker), T-006 (panel), T-007 (validación), UPD-001 (actualizaciones y
revisiones), UPD-002 (contexto y concurrencia), UPD-005 (dashboard por
versiones), UPD-008 (rediseño del panel), UPD-009 (scroll interno) y UPD-010
(reconstrucción visual y accesibilidad).

## 7. Pruebas existentes

- `sync-dashboard.py --check`: tracker válido y los tres artefactos coherentes
  (`tracker-data`, `derived-data` y `STATUS.md`); valida `displayVersion`,
  `objective`, `meta.initialVersion` y `groups`. Falla con `exit 1` ante
  cualquier divergencia.
- `continuity_test.py`: modificaciones independientes, conflicto en la misma
  tarea, interrupción, bloqueos, sincronización concurrente, conservación de
  trabajo ajeno, dependencias entre tareas de actualización, sesiones activas,
  cierre de sesión, versiones/grupos y mutaciones de derivados (20 pruebas).
- `dashboard_versions_test.js` / `.py`: renderizado en DOM mínimo de Node;
  selector, línea de tiempo, modos, contenido por versión, progreso aislado,
  ausencia de etapas inventadas, pendientes globales, propuesta pendiente y
  consumo de `derived-data` con respaldo local.
- `tools/run-tests.sh --tools`: ejecuta los cuatro comandos anteriores sin
  dispositivo Android ni build. No forma parte de `--all`.
- Apertura manual del panel con `file://`.

## 8. Problemas conocidos

- El panel no puede escribir en el proyecto desde `file://`; las propuestas y
  expedientes se descargan, no se guardan solos.
- La prueba de renderizado usa un DOM mínimo de Node, no un navegador real; los
  cambios visuales se verifican con Firefox headless en 1440/1024/768/390 px y
  ambos temas, midiendo desbordamiento y altura de cabecera.
- Los archivos derivados (`STATUS.md`, `index.html`) pueden quedar uno detrás de
  otro si el proceso muere entre dos escrituras; `--check` lo detecta con
  `exit 1` y la siguiente sincronización lo corrige.
- Una sesión interrumpida permanece `EN_PROGRESO` hasta que otra la inspecciona
  y la cierra; no se borra automáticamente.
- `IMPLEMENTATION_LOG.md` conserva el historial anterior a S-093; las sesiones
  nuevas viven en `docs/sessions/`.

## 9. Dependencias

Depende de Python 3 y `fcntl` (Linux/macOS). No usa red, servidores, bases de
datos ni dependencias externas.

## 10. Indicadores derivados (UPD-011)

Antes de UPD-011 el panel recalculaba en JavaScript los KPIs que Python ya
calculaba para `STATUS.md`, y existían tres reglas distintas para decidir qué
era la «Actualización actual». `--check` solo comparaba dos artefactos y podía
dar verde mientras el panel mostraba una propuesta sin aprobar como trabajo
vigente.

Ahora `vista_derivada(tracker)` es la única fuente de cálculo. Es determinista
(no incluye relojes ni marcas de ejecución), se emite en el bloque
`derived-data` con marcadores propios fuera de `tracker-data`, y el JS la lee
con un respaldo local para HTML antiguo. `--check` compara ese bloque con un
cálculo nuevo y falla si difiere. La regla de «Actual» se separa en dos:

- `actualizacionActual`: solo trabajo autorizado; alimenta `top-update` y
  `STATUS.md`. Nunca una propuesta sin aprobar.
- `versionPorDefecto`: selección inicial del panel según `UPD-005` §6.3; puede
  ser la versión más reciente registrada, mostrada con su estado y su aviso.

El badge de verificación se deriva del mismo bloque (ya no de
`meta.lastVerification`, que era autodeclarado) y el refresco por HTTP valida
forma mínima y coherencia de `meta.lastUpdated` antes de aplicar el JSON; si
falla, conserva los embebidos con indicador `warn`. Referencia completa:
[`updates/UPD-011.md`](../updates/UPD-011.md) y
[`dashboard/README.md`](../dashboard/README.md).
