# Coordinación descentralizada entre sesiones

Este documento explica cómo varias sesiones de agentes pueden trabajar en el
mismo repositorio sin un agente coordinador permanente. Cada sesión es
responsable de su tarea, de sus registros y de sincronizar lo que cambió.

## Principios

1. **Sin coordinador permanente.** No hay un agente dedicado a mantener el
   tracker, el índice o el panel.
2. **Una sesión, una tarea principal.** Varias sesiones pueden trabajar a la vez
   en tareas distintas.
3. **El tracker es la fuente canónica.** `STATUS.md` y el bloque embebido del
   panel son derivados.
4. **Los cambios ajenos son legítimos.** No se revierten, limpian ni absorben sin
   inspeccionarlos.
5. **Bloqueo breve.** El bloqueo cubre solo lectura-modificación-escritura, no la
   implementación completa.
6. **Escritura atómica.** Un fallo no debe dejar un JSON o un documento a medias.
7. **Conflicto explícito.** Si dos sesiones tocan la misma tarea o los mismos
   campos, se detecta; no se pierde trabajo en silencio.

## Al empezar una sesión

1. Lee `AGENTS.md`, `docs/STATUS.md` y `docs/INDEX.md`.
2. Ejecuta `git status --short` y guarda una lista de los archivos que ya estaban
   modificados o sin seguimiento. No son tuyos.
3. Ejecuta `python3 tools/continuity.py status` para ver tareas y sesiones
   abiertas.
4. Localiza tu sistema en `docs/INDEX.md` y abre solo su ficha.
5. Reserva la tarea con `claim` antes de modificarla.

```sh
python3 tools/continuity.py status
python3 tools/continuity.py new-session --agent "OpenCode / modelo" --title "..."
python3 tools/continuity.py claim T-XXX --session S-093 --agent "OpenCode / modelo"
```

`claim` comprueba:

- que la tarea existe y no está completada, pospuesta o bloqueada;
- que sus dependencias están `COMPLETADO`;
- que no depende de decisiones `PENDIENTE`;
- que no está `EN_PROGRESO` con otra sesión.

Si la tarea está `EN_PROGRESO` con otra sesión, **no se reasigna
automáticamente**. Inspecciona el trabajo. Solo si la sesión está abandonada y
el usuario lo autoriza, usa:

```sh
python3 tools/continuity.py claim T-XXX --session S-094 \
  --allow-reassign --reason "La sesión S-093 se interrumpió y el usuario autorizó continuar"
```

## Archivos compartidos y derivados

| Archivo | Tipo | Cómo se actualiza |
|---|---|---|
| `docs/IMPLEMENTATION_TRACKER.json` | Fuente canónica | `continuity.py apply` con plan y `expectUpdatedAt`. |
| `docs/DECISIONS.md` | Compartido | Edición localizada; decisiones nuevas también en el tracker. |
| `docs/INDEX.md` | Compartido | Edición localizada de la fila y el enlace a la ficha. |
| `docs/systems/*.md` | Ficha de sistema | La actualiza el agente que cambia ese sistema. |
| `docs/sessions/S-XXX.md` | Sesión | Archivo propio; no se comparte con otras sesiones. |
| `docs/IMPLEMENTATION_LOG.md` | Histórico | No se reescribe; las sesiones nuevas van a `docs/sessions/`. |
| `docs/STATUS.md` | Derivado | Lo regenera `sync-dashboard.py`; no editar a mano. |
| `docs/dashboard/index.html` | Derivado | Bloque `tracker-data` regenerado por el sincronizador. |
| `docs/updates/UPD-XXX.md` | Plan de actualización | Edición localizada del documento de esa actualización. |

No guardes estado canónico en `localStorage`, en el panel ni en archivos
temporales.

## Bloqueo

El bloqueo es un `flock` de POSIX sobre `docs/.continuity.lock`.

- Cualquier proceso del mismo usuario puede adquirirlo.
- No pertenece a una sesión concreta.
- El kernel lo libera al cerrar el descriptor o al morir el proceso.
- `lock-status` pide un bloqueo exclusivo: informa si un escritor podría entrar,
  aunque otra sesión mantenga un bloqueo compartido de lectura.
- El archivo puede quedar en disco; eso no significa que esté bloqueado.
- Se espera hasta 30 s por defecto (`--timeout` o `GI_LOCK_TIMEOUT`).

```sh
python3 tools/continuity.py lock-status   # libre | ocupado
```

El bloqueo se usa solo en:

- `continuity.py claim` / `apply` / `append` / `replace`;
- `sync-dashboard.py` (lectura + validación + escritura de derivados).

No se mantiene durante la implementación, las pruebas ni la edición de código.

## Actualizar el tracker

`continuity.py apply` recibe un plan JSON:

```json
{
  "sessionId": "S-093",
  "updatedAt": "2026-11-08T18:00:00-06:00",
  "operations": [
    {
      "op": "task.update",
      "id": "T-XXX",
      "expectUpdatedAt": "2026-11-08T17:00:00-06:00",
      "set": {"status": "COMPLETADO"},
      "append": {
        "artifacts": ["src/com/example/codegame/Ejemplo.java"],
        "evidence": ["Prueba X OK"]
      }
    },
    {
      "op": "session.add",
      "value": {
        "id": "S-093",
        "date": "2026-11-08T18:00:00-06:00",
        "agent": "OpenCode / modelo",
        "title": "Título",
        "summary": "Resumen",
        "tasks": ["T-XXX"],
        "files": ["..."],
        "tests": ["..."],
        "outcome": "Completado",
        "notes": "Notas",
        "document": "docs/sessions/S-093.md"
      }
    }
  ]
}
```

Secuencia interna:

1. Adquiere el bloqueo.
2. Relee el tracker actual.
3. Comprueba `expectUpdatedAt` de la tarea.
4. Aplica solo las operaciones del plan.
5. Valida el JSON resultante.
6. Escribe de forma atómica.
7. Libera el bloqueo.
8. Sincroniza el panel (salvo `--no-sync`).

Operaciones disponibles:

- `task.update` (plan base)
- `updateTask.update` (tarea de actualización)
- `update.add` / `update.update`
- `decision.add` / `decision.update`
- `session.add` / `session.update`
- `catalog.update` (vincula una actualización a sistemas/funcionalidades)
- `meta.update`

Al abrir una sesión con `new-session`, la herramienta la registra en el tracker
con `outcome: "EN_PROGRESO"` y `document`. Para cerrarla, actualiza su `outcome`
con `session.update` o añade `"closeSession": true` al plan: el plan marca
`lastSessionId` y cambia `EN_PROGRESO` por `Completado`.

`claim` resuelve dependencias tanto del plan base como de otras tareas de
actualización, y rechaza reclamar tareas de una actualización que no esté
`APROBADA` o `EN_PROGRESO`.

Si el resultado no valida, **no se escribe nada**. El original queda intacto.

### Conflictos

| Código | Significado | Acción |
|---|---|---|
| 0 | Éxito | Continuar. |
| 1 | Error de uso o E/S | Corregir el plan. |
| 2 | Conflicto de tarea o meta | Releer, reconciliar y reintentar. |
| 3 | Validación fallida | Corregir datos; no se escribió. |
| 4 | Bloqueo ocupado | Esperar o reintentar. |
| 5 | Plataforma sin `flock` | Usar Linux/macOS. |

Si dos sesiones modifican tareas distintas, ambas se conservan. Si modifican la
misma tarea con el mismo `expectUpdatedAt`, la segunda recibe conflicto y debe
releer. Nunca escribas una copia antigua del tracker.

## Editar documentos compartidos

Para `INDEX.md`, `DECISIONS.md`, `AGENTS.md` o cualquier Markdown compartido,
usa un reemplazo exacto con bloqueo en lugar de reescribir todo el archivo:

```sh
python3 tools/continuity.py replace docs/INDEX.md \
  --old-file /tmp/old.txt --new-file /tmp/new.txt --count 1
```

La herramienta relee el archivo bajo bloqueo, comprueba que el texto aparece
exactamente el número de veces esperado y escribe de forma atómica. Si otra
sesión cambió el texto, devuelve conflicto y no escribe. Para añadir al final
(por ejemplo, una nota de sesión) usa `append`.

## Sesiones

Cada sesión nueva se registra en su propio archivo:

```sh
python3 tools/continuity.py new-session \
  --agent "OpenCode / modelo" \
  --title "Título de la sesión" \
  --task T-XXX
```

El comando reserva el siguiente `S-XXX` bajo bloqueo y crea
`docs/sessions/S-XXX.md`. Al cerrar, añade la sesión al tracker con
`session.add` y el campo `document`. El historial anterior a S-093 permanece en
`docs/IMPLEMENTATION_LOG.md`.

## Sincronizar el panel

```sh
python3 tools/sync-dashboard.py          # valida, regenera STATUS.md y el panel
python3 tools/sync-dashboard.py --check  # solo comprueba
```

El sincronizador toma el bloqueo, relee el tracker, valida, escribe los derivados
y lo libera. Dos sincronizaciones simultáneas producen el mismo resultado; una
sincronización antigua no puede pisar un tracker más reciente porque no lee el
tracker antes de adquirir el bloqueo.

## Cambios ajenos

Antes de editar un archivo que ya estaba modificado:

1. Relee su contenido actual.
2. Compara con lo que esperabas.
3. Haz cambios localizados, no un reemplazo ciego.
4. Si el conflicto no se puede reconciliar sin riesgo, detente y documenta.

Nunca uses para "limpiar" el trabajo ajeno:

- `git reset --hard`
- `git clean`
- `git checkout --` sobre archivos modificados
- `git stash` de cambios que no son tuyos
- `git add .` sin revisar
- sobrescribir con una versión antigua

## Git

- No se hacen commits automáticos.
- Si el usuario pide un commit, selecciona archivos concretos, revisa el diff
  preparado y comprueba que solo incluye trabajo propio.
- No se hace `push` salvo petición explícita.
- La conservación del trabajo de todas las sesiones tiene prioridad sobre la
  limpieza del repositorio.

## Pruebas

```sh
python3 tools/tests/continuity_test.py
```

Cubre modificaciones independientes, conflicto en la misma tarea, interrupción
durante la escritura, bloqueos, sincronización concurrente y conservación de
trabajo ajeno.

## Limitaciones

- Las pruebas usan procesos del mismo equipo y el mismo sistema de archivos. No
  se ha probado con agentes reales en máquinas distintas.
- `flock` no coordina máquinas diferentes ni sistemas de archivos que no lo
  soporten. Para trabajo entre máquinas haría falta Git y una política de
  integración, no este bloqueo.
- La escritura es atómica por archivo. Si el proceso muere entre la escritura de
  `STATUS.md` y la del panel, los dos derivados pueden quedar desfasados hasta la
  siguiente sincronización.
- El bloqueo no impide que un proceso ignore la herramienta y escriba el tracker
  a mano. La disciplina del protocolo sigue siendo necesaria.
