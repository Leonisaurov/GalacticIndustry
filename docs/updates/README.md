# Actualizaciones complementarias

Esta carpeta contiene los planes de actualización que se añaden **después** del
plan maestro original. No sustituyen ni reescriben `docs/MASTER_PLAN.md`.

## Regla de separación

| Documento | Qué contiene | Se modifica |
|---|---|---|
| `docs/MASTER_PLAN.md` | Plan histórico del videojuego y sus etapas S0–S11 | Solo para aclaraciones que no reabren tareas |
| `docs/updates/UPD-XXX.md` | Plan detallado de una actualización complementaria | Sí, mientras la actualización esté viva |
| `docs/IMPLEMENTATION_TRACKER.json` | Estado canónico de etapas, tareas base, actualizaciones y tareas de actualización | Sí, al trabajar en cada tarea |
| `docs/STATUS.md` | Resumen generado del estado; no se edita a mano | Lo regenera `tools/sync-dashboard.py` |
| `docs/INDEX.md` | Mapa de sistemas, código, diseño, tareas y pruebas | Sí, cuando cambia un sistema o una ruta |
| `docs/systems/*.md` | Ficha de referencia del sistema afectado | La actualiza el agente que cambia ese sistema |
| `docs/IMPLEMENTATION_LOG.md` | Historial narrativo hasta S-092 | No; las sesiones nuevas van a `docs/sessions/` |
| `docs/sessions/S-XXX.md` | Registro independiente de una sesión | Sí, al cerrar cada sesión |
| `docs/DECISIONS.md` | Decisiones de producto y técnicas | Sí, cuando se decide algo duradero |

El plan base **no se reabre**. Si una actualización corrige o mejora algo ya
implementado, se crea una tarea nueva (`UPD-XXX-TNN`) y se referencia la tarea
histórica en `relatedTaskIds`; la tarea histórica conserva su estado.

## Identificadores

- Actualización: `UPD-001`, `UPD-002`, …
- Tarea de actualización: `UPD-001-T01`, `UPD-001-T02`, …
- Los identificadores son permanentes y no se reutilizan.
- Los identificadores del plan base (`S0`…`S11`, `T-001`…) no cambian.

## Estados de una actualización

| Código | Etiqueta | Significado |
|---|---|---|
| `PROPUESTA` | Propuesta | Idea registrada. No hay autorización para implementar. |
| `EN_REVISION` | En revisión | Se está contrastando alcance, código y observaciones. |
| `PENDIENTE_APROBACION` | Pendiente de aprobación | El plan está listo, pero espera decisión del usuario. |
| `APROBADA` | Aprobada | El usuario autorizó el desarrollo; aún no hay tareas activas. |
| `EN_PROGRESO` | En progreso | Hay al menos una tarea de implementación activa. |
| `BLOQUEADA` | Bloqueada | Existe un impedimento documentado. |
| `COMPLETADA` | Completada | Sus tareas de progreso están implementadas y verificadas. |
| `RECHAZADA` | Rechazada | Se decidió no continuar. Se conserva como antecedente. |
| `ARCHIVADA` | Archivada | Se conserva como antecedente, sin trabajo previsto. |

Una propuesta **nunca** pasa a `APROBADA` automáticamente. El validador
`tools/sync-dashboard.py` comprueba que una actualización en `PROPUESTA`,
`EN_REVISION` o `PENDIENTE_APROBACION` no tenga tareas `EN_PROGRESO` ni
`COMPLETADO`, y que las actualizaciones aprobadas registren quién aprobó y
cuándo.

## Estructura de `UPD-XXX.md`

Cada documento debe incluir, como mínimo:

1. Identificación y estado.
2. Resumen y motivación.
3. Alcance: qué incluye y qué no incluye.
4. Sistemas, funcionalidades y etapas afectadas.
5. Referencias a tareas y actualizaciones existentes.
6. Plan de tareas con criterios de aceptación y pruebas.
7. Criterios de aceptación de la actualización.
8. Pruebas y verificación.
9. Riesgos, limitaciones y problemas conocidos.
10. Observaciones y decisiones relacionadas.
11. Registro de implementación.

Si un dato no está documentado, se escribe `No documentado` en lugar de
inventarlo.

## Representación en el tracker

`docs/IMPLEMENTATION_TRACKER.json` incorpora tres claves nuevas:

- `updateStatusDefinitions`: etiquetas y descripciones de los estados.
- `updates`: lista de actualizaciones; cada una contiene sus propias tareas.
- `reviewCatalog`: catálogo opcional de sistemas y funcionalidades que el panel
  usa para preparar expedientes de revisión.

Una actualización tiene esta forma (resumida):

```json
{
  "id": "UPD-002",
  "displayVersion": "0.3",
  "objective": "Objetivo principal de la actualización.",
  "name": "Nombre de la actualización",
  "status": "PROPUESTA",
  "changeType": "MEJORA",
  "priority": "P2",
  "description": "…",
  "motivation": "…",
  "affectedSystems": ["SYS-MINERIA"],
  "affectedFeatures": ["F-MINERIA"],
  "relatedStages": ["S4"],
  "relatedTasks": ["T-406"],
  "relatedUpdates": [],
  "dependencies": [],
  "document": "docs/updates/UPD-002.md",
  "approval": {
    "required": true,
    "status": "PENDIENTE",
    "approvedBy": null,
    "approvedAt": null,
    "basis": "…"
  },
  "acceptanceCriteria": ["…"],
  "groups": [],
  "tasks": [
    {
      "id": "UPD-002-T01",
      "title": "…",
      "status": "PENDIENTE",
      "category": "implementation",
      "countsForProgress": true,
      "dependencies": [],
      "relatedTaskIds": ["T-406"],
      "files": [],
      "contextFiles": [],
      "contextDocs": [],
      "artifacts": [],
      "acceptanceCriteria": ["…"],
      "tests": ["…"],
      "technicalNotes": "",
      "executionLog": [],
      "evidence": []
    }
  ],
  "observations": [],
  "implementationLog": [],
  "reviewExports": []
}
```

Las tareas de actualización viven dentro de su actualización para no mezclar el
progreso del plan base. El panel calcula el progreso base solo con `tasks` y el
progreso de cada actualización con `updates[].tasks`.

## Versiones de presentación

El panel representa el desarrollo como versiones visuales:

- `meta.initialVersion.displayVersion` identifica la implementación inicial
  (por defecto `0.1`).
- Cada actualización puede declarar `displayVersion` (`0.2`, `0.3`, …). Es un
  metadato de presentación estable: no sustituye al `id` ni cambia el estado.
- El validador exige formato `N.N` y unicidad entre la versión inicial y las
  actualizaciones. Si falta `displayVersion`, el panel deriva una etiqueta y el
  validador emite un aviso.
- `objective` es el objetivo principal que muestra el modo descriptivo y la
  cabecera de la versión. Si falta, el panel usa la descripción o la motivación.
- `groups` es opcional. Si una actualización declara grupos, el panel los
  muestra como grupos de trabajo con sus tareas, dependencias y pruebas. Si no
  los declara, muestra una lista plana: **no reutiliza las etapas `S0`–`S11` del
  plan inicial ni inventa una jerarquía**.

Ejemplo de grupo:

```json
"groups": [
  {
    "id": "G1",
    "name": "Grupo de trabajo",
    "objective": "…",
    "description": "…",
    "status": "PENDIENTE",
    "taskIds": ["UPD-002-T01"],
    "dependencies": [],
    "tests": ["…"]
  }
]
```

La numeración visual no se reutiliza ni se renumera. Añadir una actualización
posterior no cambia las etiquetas históricas.

## Reserva y concurrencia

Varias sesiones pueden trabajar en actualizaciones distintas. Antes de empezar
una tarea:

```sh
python3 tools/continuity.py claim UPD-002-T01 --session S-093 --agent "..."
```

La tarea queda `EN_PROGRESO` con `assignedSessionId`. No se reasigna una tarea
activa de otra sesión sin inspeccionarla y sin autorización.

Al modificar el tracker, usa un plan JSON con `expectUpdatedAt`:

```sh
python3 tools/continuity.py apply plan.json
```

La herramienta relee el tracker bajo bloqueo, aplica solo las operaciones del
plan, valida el JSON y escribe de forma atómica. Si dos sesiones tocan la misma
tarea, la segunda recibe conflicto y debe releer. Los detalles están en
`docs/CONCURRENCY.md`.

## Flujo de trabajo

### Crear una propuesta desde el panel

1. Abrir `docs/dashboard/index.html`.
2. Pulsar **Nueva propuesta** en la sección ACTUALIZACIONES.
3. Rellenar nombre, descripción, motivo, tipo, sistemas, funcionalidades,
   prioridad y observaciones.
4. Descargar el `.md` (o `.txt`) y el bloque JSON para el tracker.
5. Guardar el documento como `docs/updates/UPD-XXX.md`.
6. Registrar la actualización en `updates` del tracker con estado `PROPUESTA` y
   `approval.status = "PENDIENTE"`.
7. Ejecutar `python3 tools/sync-dashboard.py`.

El navegador no puede escribir en el directorio del proyecto desde `file://`.
Los borradores de `localStorage` son una comodidad temporal, no la fuente
canónica.

### Revisar una implementación y proponer una corrección

1. En el panel, pulsar **Preparar expediente de revisión**.
2. Seleccionar etapas, tareas, funcionalidades, sistemas, tareas de
   actualización y/o actualizaciones completadas.
3. Descargar el expediente en Markdown o TXT.
4. Añadir la sección **OBSERVACIONES DEL USUARIO** con críticas y cambios
   deseados.
5. Entregar el expediente a un agente.
6. El agente contrasta cada observación con el código real y registra una nueva
   actualización en estado `PROPUESTA` o `PENDIENTE_APROBACION`.
7. El usuario revisa y aprueba (o rechaza) la propuesta.
8. Solo después de la aprobación se crean y ejecutan tareas individuales.

### Aprobar una actualización

La aprobación es una acción del usuario registrada por un agente en el tracker:

```json
"approval": {
  "required": true,
  "status": "APROBADA",
  "approvedBy": "Usuario",
  "approvedAt": "2026-11-08T12:00:00-06:00",
  "basis": "Respuesta del usuario en la sesión"
}
```

El estado de la actualización pasa a `APROBADA`. Las tareas siguen `PENDIENTE`
hasta que una sesión empiece una.

## Comandos

```sh
python3 tools/continuity.py status
python3 tools/continuity.py new-session --agent "..." --title "..."
python3 tools/continuity.py claim UPD-XXX-TNN --session S-XXX --agent "..."
python3 tools/continuity.py apply plan.json
python3 tools/sync-dashboard.py          # valida, sincroniza el panel y genera STATUS.md
python3 tools/sync-dashboard.py --check  # solo valida y comprueba HTML y STATUS.md
python3 tools/tests/continuity_test.py   # pruebas de concurrencia
```

`docs/STATUS.md` es derivado: no lo edites a mano. Si `docs/INDEX.md` no cubre un
sistema nuevo, añade la entrada correspondiente y enlázala con las tareas y
pruebas reales.

## Restricciones

- Sin servidor, backend, base de datos, CDN ni red.
- El panel debe seguir funcionando con `file://`.
- Las descargas se generan en el navegador con `Blob` y `URL.createObjectURL`.
- La persistencia real son los archivos del proyecto.
- No se modifican tareas ni etapas históricas para incorporar correcciones.
