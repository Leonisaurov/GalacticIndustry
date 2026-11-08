# GalacticIndustry

Juego Android en **Java + libGDX 1.14.2**. Build canónico: **apkc + apkc-deps**
(sin Gradle). `app/module.toml` es solo un puente para CodeAssist.

- **Código del juego:** `src/com/example/codegame/`
- **Recursos:** `assets/`, `res/`, `lib/<abi>/`
- **Herramientas:** `tools/` (`run-tests.sh`, `tests/`, `sync-dashboard.py`)

## Cómo continuar el proyecto (obligatorio)

Este repositorio usa un sistema persistente de continuidad. **No dependas de la
memoria de una conversación anterior.** El estado real está en archivos y puede
haber otras sesiones trabajando al mismo tiempo.

Al empezar cualquier sesión, en este orden:

1. Lee este `AGENTS.md`.
2. Ejecuta `git status --short` y `python3 tools/continuity.py status`. Registra
   qué archivos ya estaban modificados o sin seguimiento y qué tareas o sesiones
   están activas. **No son tuyos.**
3. Consulta `docs/STATUS.md` para conocer el estado real sin abrir el tracker
   completo.
4. Consulta `docs/INDEX.md` para localizar el sistema, los archivos, el diseño,
   las decisiones, las tareas y las pruebas.
5. Abre **solo** la ficha `docs/systems/<sistema>.md` correspondiente. No leas
   todas las fichas ni todo `MASTER_PLAN.md`.
6. Localiza la tarea `EN_PROGRESO` o `LISTO` y sus dependencias, primero en el
   plan base y después en la actualización aprobada. Usa `contextFiles`,
   `contextDocs`, `artifacts` y `tests` si están registrados.
7. Identifica solo los archivos y documentos pertinentes.
8. Contrasta la documentación con el código real antes de modificarlo.
9. Consulta las decisiones aplicables en `docs/DECISIONS.md`.
10. Comprueba que las dependencias están `COMPLETADO` y que no hay decisiones
    pendientes que bloqueen la tarea.
11. Reserva la tarea con `python3 tools/continuity.py claim ...`. No empieces una
    tarea que ya esté `EN_PROGRESO` con otra sesión.

Después, implementa **una sola tarea**, ejecuta sus pruebas, verifica sus
criterios y actualiza tracker, ficha de sistema, sesión, decisiones y panel.
Detente. No empieces automáticamente la siguiente tarea.

> **Regla de oro:** una sesión = una tarea principal. Varias sesiones pueden
> trabajar a la vez en tareas distintas. No implementes una etapa completa ni te
> adelantes a funcionalidades posteriores.

### Reglas de lectura

- No leas el tracker completo ni el plan maestro completo salvo que la tarea lo
  requiera. `docs/STATUS.md` y `docs/INDEX.md` existen para evitarlo.
- No explores indiscriminadamente todo `src/` cuando el índice ya señala los
  archivos. Lee solo los fragmentos necesarios.
- Si el índice no cubre el sistema, haz búsquedas dirigidas y, al terminar,
  añade o corrige la ficha y la fila de `docs/INDEX.md`.
- Si un documento y el código discrepan, prevalece el código real; corrige la
  documentación o el comentario obsoleto y deja constancia.

## Coordinación entre sesiones

No hay un agente coordinador permanente. Cada sesión documenta y sincroniza su
propio trabajo. El protocolo completo está en `docs/CONCURRENCY.md`.

- **Detecta cambios ajenos:** `git status --short`; inspecciona antes de editar.
  No uses `git reset --hard`, `git clean`, `git checkout --`, `git stash` ajeno
  ni `git add .`.
- **Reserva tu tarea:** `tools/continuity.py claim T-XXX --session S-XXX`.
  No reasignes una tarea `EN_PROGRESO` de otra sesión sin inspeccionarla y sin
  autorización.
- **Actualiza el tracker:** `tools/continuity.py apply plan.json`. Relee dentro
  del bloqueo, aplica solo tu tarea, valida y escribe de forma atómica. Usa
  `expectUpdatedAt` para detectar conflictos.
- **Documentos compartidos:** `tools/continuity.py replace ARCHIVO --old-file ... --new-file ... --count 1`
  para cambios localizados, o `append` para añadir al final.
- **Sesiones:** `tools/continuity.py new-session` crea
  `docs/sessions/S-XXX.md`. El log histórico anterior a S-093 no se reescribe.
- **Sincroniza:** `python3 tools/sync-dashboard.py` regenera `STATUS.md` y el
  panel bajo bloqueo.
- **Bloqueo breve:** `docs/.continuity.lock` (`flock`). No se mantiene durante la
  implementación. Se libera al terminar el proceso.
- **Conflicto:** si dos sesiones tocan la misma tarea o los mismos campos, el
  segundo recibe conflicto y debe releer. No sobrescribas una copia antigua.

## Archivos de continuidad

| Archivo | Responsabilidad |
|---|---|
| `AGENTS.md` | Punto de entrada y protocolo |
| `docs/STATUS.md` | **Resumen generado** del estado; no editar a mano |
| `docs/INDEX.md` | Mapa de sistemas → código, diseño, decisiones, tareas y pruebas |
| `docs/systems/*.md` | Fichas de referencia por sistema; navegación selectiva |
| `docs/CONCURRENCY.md` | Bloqueos, escrituras atómicas y coordinación entre sesiones |
| `docs/sessions/S-XXX.md` | Registro independiente de cada sesión nueva |
| `docs/MASTER_PLAN.md` | Plan maestro histórico, arquitectura, fórmulas, riesgos |
| `docs/IMPLEMENTATION_TRACKER.json` | **Fuente canónica** de etapas, tareas, estados, dependencias y progreso |
| `docs/IMPLEMENTATION_LOG.md` | Historial narrativo hasta S-092; no se reescribe |
| `docs/DECISIONS.md` | Decisiones de producto y técnicas |
| `docs/PENDING_DECISIONS.md` | Resumen de decisiones cerradas; ya no hay pendientes |
| `docs/updates/README.md` | Flujo, estados, esquema y restricciones de las actualizaciones |
| `docs/updates/UPD-XXX.md` | Plan detallado de cada actualización complementaria |
| `docs/dashboard/index.html` | Panel visual autocontenido; datos embebidos en `file://` y refresco JSON por HTTP |
| `docs/dashboard/dashboard.css` | Estilos del panel |
| `docs/dashboard/dashboard.js` | Lógica del panel (filtros, temas, enlaces y render) |
| `docs/PROJECT_DASHBOARD.html` | Redirección de compatibilidad a `docs/dashboard/index.html` |
| `tools/sync-dashboard.py` | Validación del tracker, sincronización JSON → HTML y generación de `STATUS.md` |
| `tools/continuity.py` | Reserva de tareas, planes de actualización, sesiones y bloqueo |
| `tools/continuity_io.py` | Bloqueo `flock` y escritura atómica reutilizable |

No edites a mano el bloque `tracker-data` de `docs/dashboard/index.html` salvo que sea imprescindible.
Después de modificar el JSON, ejecuta:

```sh
python3 tools/sync-dashboard.py          # valida, sincroniza el panel y genera STATUS.md
python3 tools/sync-dashboard.py --check  # solo valida y comprueba HTML y STATUS.md
```

El panel se abre con doble clic; no necesita servidor, Node.js, Python como
servidor, CDN ni red. `docs/STATUS.md` es derivado: si cambia el tracker, vuelve
a ejecutar el sincronizador.

### Jerarquía de fuentes

| Documento | Responsabilidad | No debe contener |
|---|---|---|
| `docs/IMPLEMENTATION_TRACKER.json` | Estado operativo de tareas y actualizaciones | Prosa larga de diseño |
| `docs/DECISIONS.md` | Decisiones aprobadas, aplazadas o resueltas | Copia del estado de las tareas |
| `docs/MASTER_PLAN.md` | Especificación histórica y planificación | Estado de ejecución actualizado |
| `docs/INDEX.md` | Mapa de navegación entre sistemas y documentos | Duplicación del plan maestro |
| `docs/systems/*.md` | Ficha de referencia de un sistema | Duplicación del plan maestro |
| `docs/STATUS.md` | Resumen generado del estado actual | Datos escritos a mano |
| `docs/IMPLEMENTATION_LOG.md` | Historial narrativo hasta S-092 | Fuente canónica de estado |
| `docs/sessions/S-XXX.md` | Registro de una sesión | Estado canónico o copia del tracker |
| `docs/CONCURRENCY.md` | Protocolo de bloqueo y coordinación | Estado de tareas |

Los demás documentos deben enlazar a estas fuentes en lugar de reproducir
bloques extensos. No se añaden nuevas fuentes de verdad.

## Estados de tarea

| Estado | Uso |
|---|---|
| `PENDIENTE` | No iniciada o dependencias sin cumplir |
| `LISTO` | Dependencias completadas; puede ejecutarse |
| `EN_PROGRESO` | Un agente está trabajando en ella |
| `COMPLETADO` | Implementada, probada y con criterios verificados |
| `BLOQUEADO` | Un problema impide continuar |
| `POSPUESTO` | Aplazada conscientemente |
| `REQUIERE_DECISION` | Necesita decisión del usuario |

- Una tarea solo pasa a `COMPLETADO` cuando sus criterios y pruebas están
  verificados.
- Una tarea terminada sin evidencias no está terminada.
- Las tareas de preparación no cuentan para el progreso de implementación.

## Protocolo al iniciar una tarea

1. Identifica el alcance exacto en el tracker y en la ficha del sistema.
2. Comprueba que todas sus dependencias están `COMPLETADO`.
3. Revisa si tiene `decisionIds` pendientes. Si los tiene, no implementes:
   registra la decisión y detente.
4. Inspecciona los archivos y el código existente.
5. Reserva la tarea con `python3 tools/continuity.py claim T-XXX --session S-XXX`.
   La herramienta marca `EN_PROGRESO`, registra `assignedSessionId` y añade una
   entrada inicial al `executionLog`. Si ya está en progreso con otra sesión, no
   la reclames sin inspeccionarla.
6. Implementa solo lo necesario para esa tarea.
7. No refactorices sistemas ajenos salvo que sea imprescindible; justifícalo.
8. No implementes tareas posteriores “de paso”.

## Protocolo al terminar

1. Ejecuta las pruebas de la tarea.
2. Verifica cada criterio de aceptación.
3. Registra archivos modificados, contexto consultado y decisiones técnicas.
4. Prepara un plan JSON y aplica los cambios al tracker con
   `python3 tools/continuity.py apply plan.json`. Incluye:
   - estado de la tarea;
   - `meta.lastUpdated`, `meta.lastSessionId`;
   - `meta.currentStageId`, `meta.currentTaskId`, `meta.nextTaskId`;
   - si es una actualización: estado de la actualización,
     `meta.currentUpdateId`, tareas y aprobación;
   - `artifacts` con los archivos realmente creados o modificados;
   - `contextFiles`/`contextDocs` si la tarea se reabre o se revisa;
   - evidencias y `executionLog`;
   - sesión en `sessions`, con `document` apuntando a `docs/sessions/S-XXX.md`.
5. Escribe el resumen completo en `docs/sessions/S-XXX.md`. No reescribas
   `docs/IMPLEMENTATION_LOG.md`; conserva el histórico anterior a S-093.
6. Actualiza `docs/systems/<sistema>.md` si cambió arquitectura, clases,
   responsabilidades, dependencias, pruebas o problemas conocidos.
7. Actualiza `docs/DECISIONS.md` si tomaste una decisión. Actualiza la fila
   correspondiente de `docs/INDEX.md` solo si cambió un sistema, una ruta, una
   tarea o una prueba.
8. Ejecuta `python3 tools/sync-dashboard.py`; esto regenera `docs/STATUS.md` y
   sincroniza el panel bajo bloqueo. No edites `STATUS.md` a mano.
9. Revisa `git status --short` y comprueba que no has pisado cambios ajenos.
10. No hagas commit salvo que el usuario lo pida expresamente. Detente.

## Git: conservación del trabajo

Los commits no son automáticos. El repositorio puede contener cambios de otras
sesiones.

1. Antes de empezar, guarda una lista de los archivos ya modificados.
2. No uses `git reset --hard`, `git clean`, `git checkout --` sobre archivos
   modificados, `git stash` de cambios ajenos ni `git add .`.
3. Si el usuario pide un commit, selecciona archivos concretos, inspecciona el
   diff preparado y comprueba que solo incluye trabajo propio.
4. Usa un mensaje con el ID y el título (`T-207: ...`, `UPD-002-T03: ...`).
5. No mezcles varias tareas en el mismo commit.
6. No commitees archivos generados (`build/`, `jar/`, `res/__deps/`,
   `deps.installed.json`, `R.java`, `Manifest.java`).
7. No hagas `push` salvo que el usuario lo pida.
8. La conservación del trabajo de todas las sesiones tiene prioridad sobre la
   limpieza del repositorio.

## Recuperación tras una interrupción

Si una tarea está `EN_PROGRESO`:

1. No la des por terminada.
2. Ejecuta `python3 tools/continuity.py status` y mira `assignedSessionId` y
   `assignedAt`.
3. Abre `docs/sessions/S-XXX.md` de la sesión asignada y `docs/IMPLEMENTATION_LOG.md`.
4. Inspecciona los cambios reales en el código y en `git status --short`.
5. Ejecuta las pruebas relevantes.
6. Determina qué falta exactamente.
7. Continúa la misma tarea con la misma sesión o, si está abandonada y el usuario
   lo autoriza, reclámala con `--allow-reassign --reason`.
8. Si no puedes continuar, márcala `BLOQUEADO` con el motivo y lo que falta.

No empieces otra tarea mientras haya una `EN_PROGRESO` recuperable. No borres una
asignación solo porque haya pasado tiempo.

## Planificación y cambios

- No cambies IDs existentes.
- No elimines tareas o etapas silenciosamente.
- Para una característica nueva: añádela al plan, define dependencias, crea
  tareas con IDs nuevos, actualiza `taskCount`/`implementationTaskCount` de la
  etapa, actualiza tracker, log y panel.
- Las propuestas no aprobadas se registran como `PENDIENTE` en `DECISIONS.md`;
  no se convierten automáticamente en requisitos.
- Cuando el usuario devuelva respuestas, actualiza `DECISIONS.md`, el tracker y
  `PENDING_DECISIONS.md` (marca las resueltas y actualiza el resumen de pendientes).
- Si una tarea se divide, crea IDs nuevos; no reutilices el anterior.

## Actualizaciones complementarias y revisiones

El plan maestro (`docs/MASTER_PLAN.md`) es **histórico**. Todo cambio posterior
se registra como una actualización `UPD-XXX` en `docs/updates/` y en
`updates[]` del tracker. El flujo completo está en `docs/updates/README.md`.

### Reglas esenciales

- Una actualización nueva nace en `PROPUESTA`, `EN_REVISION` o
  `PENDIENTE_APROBACION`, con `approval.status = "PENDIENTE"`.
- **No se implementa nada sin aprobación explícita del usuario.**
- Tras la aprobación se registra `approvedBy`, `approvedAt` y `basis`, y la
  actualización pasa a `APROBADA`.
- Las tareas de actualización (`UPD-XXX-TNN`) viven en `updates[].tasks`, no en
  `tasks`; no alteran el progreso del plan base.
- Una tarea nueva puede referenciar una tarea histórica con `relatedTaskIds` o
  `dependencies`; la tarea histórica **no cambia de estado**.
- No se reutilizan IDs ni se renumeran tareas existentes.
- Una actualización en `PROPUESTA`, `EN_REVISION` o `PENDIENTE_APROBACION` no
  puede tener tareas `EN_PROGRESO` ni `COMPLETADO`.
- El panel permite preparar propuestas y expedientes de revisión descargables,
  pero no escribe en el proyecto. Los borradores de `localStorage` no son
  estado canónico.
- No se añaden servidores, backends, bases de datos, CDN ni APIs.

Estados: `PROPUESTA`, `EN_REVISION`, `PENDIENTE_APROBACION`, `APROBADA`,
`EN_PROGRESO`, `BLOQUEADA`, `COMPLETADA`, `RECHAZADA`, `ARCHIVADA`.


## Estado y documentación

- Estado actual: `docs/STATUS.md` (generado) y `docs/IMPLEMENTATION_TRACKER.json` (canónico).
- Navegación: `docs/INDEX.md` y `docs/systems/`; sesiones en `docs/sessions/`.
- El plan base está cerrado; los cambios posteriores se registran como actualizaciones (`docs/updates/`). No confundas “plan escrito” con “funcionalidad implementada”.

## Comandos mínimos

```sh
# Build
tcr -c 3 -m 2G -r 128M apkc build --inc

# Instalar/ejecutar
apkc run --inc

# Lógica sin Android
java -cp "build/classes:jar/*" com.example.codegame.SelfTest

# Tests
tools/run-tests.sh --list
tools/run-tests.sh 01_logic 04_autopilot
# tools/run-tests.sh --all  solo si el usuario lo pide

# Continuidad
python3 tools/continuity.py status
python3 tools/continuity.py new-session --agent "..." --title "..."
python3 tools/continuity.py claim T-XXX --session S-XXX --agent "..."
python3 tools/continuity.py apply plan.json
python3 tools/continuity.py replace docs/INDEX.md --old-file old.txt --new-file new.txt
python3 tools/continuity.py append docs/IMPLEMENTATION_LOG.md --text "..."
python3 tools/sync-dashboard.py
python3 tools/sync-dashboard.py --check
python3 tools/tests/continuity_test.py
```

## Reglas críticas del juego

- No metas recursos de jars en el APK: la fuente está en `assets/fonts/` y se
  carga con `Gdx.files.internal`. No uses `new BitmapFont()`.
- Acelerar y disparar consumen combustible; no rompas esa economía sin
  actualizar `SelfTest` y la documentación.
- No hay vidas: cada impacto daña el casco y un sistema o ala al azar. El casco
  a 0 termina la partida; motor, depósito, cañón, timón y alas aplican sus
  penalizaciones. La tienda repara las partes con monedas.
- Pruebas en dispositivo con `tools/run-tests.sh`, no con `adb` manual ni
  ejecutando la suite completa por defecto. Las capturas son solo para diseño.
- Tras una pasada aditiva: `Visuals.normal(batch)` **y** `beginShapes()`.
- No crees `Color` ni objetos dentro de `render()`.
- El piloto automático frena (`STOP`) al perder el objetivo, limpia el
  acelerador y tiene margen de captura para los bidones.
- Si cambias la duración o fases de la muerte, ajusta los tests `05_death` y
  `08_visual_death`.
- Mantén la fuente ASCII en la UI; evita acentos si el glifo no existe.
- Detalle completo en `docs/`; no infles este archivo.

## Definición de terminado

Una tarea está terminada cuando compila, pasa sus pruebas, cumple sus criterios,
no rompe lo existente, está registrada en el tracker, tiene sesión documentada en
`docs/sessions/S-XXX.md`, las decisiones están documentadas, la ficha del sistema
y `docs/INDEX.md` siguen siendo correctos y el panel y `docs/STATUS.md` están
sincronizados.

Una etapa está terminada cuando todas sus tareas de progreso están
`COMPLETADO` o `POSPUESTO` con justificación, y su gate se ha verificado.

## No hacer

- No implementar varias tareas o una etapa completa en una sesión.
- No marcar `COMPLETADO` sin pruebas.
- No inventar estados en el panel.
- No usar `fetch()` como única fuente del panel: con `file://` debe leer el JSON embebido; si se sirve por HTTP puede releer `IMPLEMENTATION_TRACKER.json` sin recargar la página y, si falla, debe mantener los datos actuales.
- No añadir dependencias, servicios, servidores ni APIs para el panel.
- No cambiar IDs, dependencias o estados sin registrarlo.
- No reabrir ni reescribir etapas o tareas del plan base para incorporar cambios posteriores.
- No aprobar una actualización sin una decisión explícita del usuario.
- No implementar tareas de una actualización en estado `PROPUESTA`, `EN_REVISION` o `PENDIENTE_APROBACION`.
- No presentar borradores de `localStorage` como estado canónico.
- No editar `docs/STATUS.md` a mano: se regenera con `tools/sync-dashboard.py`.
- No duplicar en `AGENTS.md` cifras o resúmenes de progreso que ya genera `STATUS.md`.
- No inventar clases, archivos, pruebas ni rutas en `docs/INDEX.md` ni en las fichas.
- No empezar la siguiente tarea después de terminar la actual.
- No hacer commits automáticos ni mezclar trabajo de otras sesiones.
- No usar `git reset --hard`, `git clean`, `git checkout --` sobre archivos modificados, `git stash` ajeno ni `git add .`.
- No reasignar una tarea `EN_PROGRESO` de otra sesión sin inspeccionarla y sin autorización.
- No escribir el tracker directamente sin releerlo y validarlo; usa `tools/continuity.py`.
- No mantener el bloqueo durante la implementación, las pruebas o la edición de código.
