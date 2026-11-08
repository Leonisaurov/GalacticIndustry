# GalacticIndustry

Juego Android en **Java + libGDX 1.14.2**. Build canónico: **apkc + apkc-deps**
(sin Gradle). `app/module.toml` es solo un puente para CodeAssist.

- **Código del juego:** `src/com/example/codegame/`
- **Recursos:** `assets/`, `res/`, `lib/<abi>/`
- **Herramientas:** `tools/` (`run-tests.sh`, `tests/`, `sync-dashboard.py`)

## Cómo continuar el proyecto (obligatorio)

Este repositorio usa un sistema persistente de continuidad. **No dependas de la
memoria de una conversación anterior.** El estado real está en archivos.

Al empezar cualquier sesión, en este orden:

1. Lee este `AGENTS.md`.
2. Consulta `docs/STATUS.md` para conocer el estado real sin abrir el tracker
   completo.
3. Consulta `docs/INDEX.md` para localizar el sistema, los archivos, el diseño,
   las decisiones, las tareas y las pruebas.
4. Localiza la tarea `EN_PROGRESO` o `LISTO` y sus dependencias, primero en el
   plan base y después en la actualización aprobada. Usa `contextFiles` y
   `contextDocs` si están registrados.
5. Identifica solo los archivos y documentos pertinentes.
6. Contrasta la documentación con el código real antes de modificarlo.
7. Consulta las decisiones aplicables en `docs/DECISIONS.md`.
8. Comprueba que las dependencias están `COMPLETADO` y que no hay decisiones
   pendientes que bloqueen la tarea.

Después, implementa **una sola tarea**, ejecuta sus pruebas, verifica sus
criterios y actualiza tracker, log, decisiones, documentación y panel. Detente.
No empieces automáticamente la siguiente tarea.

> **Regla de oro:** una sesión = una tarea. No implementes una etapa completa
> ni te adelantes a funcionalidades posteriores.

### Reglas de lectura

- No leas el tracker completo ni el plan maestro completo salvo que la tarea lo
  requiera. `docs/STATUS.md` y `docs/INDEX.md` existen para evitarlo.
- No explores indiscriminadamente todo `src/` cuando el índice ya señala los
  archivos. Lee solo los fragmentos necesarios.
- Si el índice no cubre el sistema, haz búsquedas dirigidas y, al terminar,
  valora añadir o corregir la entrada en `docs/INDEX.md`.
- Si un documento y el código discrepan, prevalece el código real; corrige la
  documentación o el comentario obsoleto y deja constancia.

## Archivos de continuidad

| Archivo | Responsabilidad |
|---|---|
| `AGENTS.md` | Punto de entrada y protocolo |
| `docs/STATUS.md` | **Resumen generado** del estado; no editar a mano |
| `docs/INDEX.md` | Mapa de sistemas → código, diseño, decisiones, tareas y pruebas |
| `docs/MASTER_PLAN.md` | Plan maestro, arquitectura, fórmulas, riesgos |
| `docs/IMPLEMENTATION_TRACKER.json` | **Fuente canónica** de etapas, tareas, estados, dependencias y progreso |
| `docs/IMPLEMENTATION_LOG.md` | Historial cronológico de sesiones |
| `docs/DECISIONS.md` | Decisiones de producto y técnicas |
| `docs/PENDING_DECISIONS.md` | Resumen de decisiones cerradas; ya no hay pendientes |
| `docs/updates/README.md` | Flujo, estados, esquema y restricciones de las actualizaciones |
| `docs/updates/UPD-XXX.md` | Plan detallado de cada actualización complementaria |
| `docs/dashboard/index.html` | Panel visual autocontenido; datos embebidos en `file://` y refresco JSON por HTTP |
| `docs/dashboard/dashboard.css` | Estilos del panel |
| `docs/dashboard/dashboard.js` | Lógica del panel (filtros, temas, enlaces y render) |
| `docs/PROJECT_DASHBOARD.html` | Redirección de compatibilidad a `docs/dashboard/index.html` |
| `tools/sync-dashboard.py` | Validación del tracker, sincronización JSON → HTML y generación de `STATUS.md` |

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
| `docs/STATUS.md` | Resumen generado del estado actual | Datos escritos a mano |
| `docs/IMPLEMENTATION_LOG.md` | Historial cronológico de sesiones | Fuente canónica de estado |

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

1. Identifica el alcance exacto en el tracker.
2. Comprueba que todas sus dependencias están `COMPLETADO`.
3. Revisa si tiene `decisionIds` pendientes. Si los tiene, no implementes:
   registra la decisión y detente.
4. Inspecciona los archivos y el código existente.
5. Marca la tarea `EN_PROGRESO` en el tracker y añade una entrada inicial al log.
6. Implementa solo lo necesario para esa tarea.
7. No refactorices sistemas ajenos salvo que sea imprescindible; justifícalo.
8. No implementes tareas posteriores “de paso”.

## Protocolo al terminar

1. Ejecuta las pruebas de la tarea.
2. Verifica cada criterio de aceptación.
3. Registra archivos modificados y decisiones técnicas.
4. Actualiza el tracker:
   - estado de la tarea;
   - `meta.lastUpdated`, `meta.lastSessionId`;
   - `meta.currentStageId`, `meta.currentTaskId`, `meta.nextTaskId`;
   - si es una actualización: estado de la actualización,
     `meta.currentUpdateId`, tareas y aprobación;
   - `artifacts` con los archivos realmente creados o modificados;
   - `contextFiles`/`contextDocs` si la tarea se reabre o se revisa;
   - evidencias y `executionLog`;
   - sesión en `sessions`.
5. Añade la sesión a `docs/IMPLEMENTATION_LOG.md`.
6. Actualiza `docs/DECISIONS.md` si tomaste una decisión.
7. Actualiza `docs/INDEX.md` si cambió un sistema, una ruta, una tarea o una
   prueba. Actualiza el documento `docs/updates/UPD-XXX.md` si la tarea
   pertenece a una actualización. No reescribas el plan maestro: solo aclara si
   cambió el diseño y las dependencias del plan base.
8. Ejecuta `python3 tools/sync-dashboard.py`; esto regenera `docs/STATUS.md` y
   sincroniza el panel. No edites `STATUS.md` a mano.
9. Haz commit de la tarea (ver “Git: un commit por paso”).
10. Detente.

## Git: un commit por paso

Cada tarea o paso completado debe quedar guardado en un commit local:

1. Ejecuta las pruebas y `python3 tools/sync-dashboard.py` antes de commitear.
2. Revisa `git status` y añade solo lo que corresponda a la tarea:
   código, tracker, log, documentación, panel y pruebas.
3. Usa un mensaje con el ID y el título:
   - `T-207: integración y balance de supervivencia`
   - `docs: ...` solo para cambios documentales sin tarea.
4. No mezcles varias tareas en el mismo commit.
5. No commitees archivos generados: `build/`, `jar/`, `res/__deps/`,
   `deps.installed.json`, `R.java`, `Manifest.java` (ya están ignorados).
6. Si la tarea queda interrumpida, haz un commit `WIP T-XXX: ...` para no
   perder el estado; el commit final se hace al completarla.
7. No hagas `push` salvo que el usuario lo pida.
8. Si el árbol ya tenía cambios previos sin commit, no los mezcles a ciegas:
   revisa el diff y deja constancia en el mensaje o en el log.

## Recuperación tras una interrupción

Si una tarea está `EN_PROGRESO`:

1. No la des por terminada.
2. Lee el log y el tracker.
3. Inspecciona los cambios reales en el código.
4. Ejecuta las pruebas relevantes.
5. Determina qué falta exactamente.
6. Continúa la misma tarea.
7. Si no puedes continuar, márcala `BLOQUEADO` con el motivo y lo que falta.

No empieces otra tarea mientras haya una `EN_PROGRESO` recuperable.

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

El plan maestro (`docs/MASTER_PLAN.md`) es **histórico**. No se reescribe para
incorporar correcciones posteriores. Todo cambio posterior al plan base se
registra como una actualización complementaria.

### Cuándo crear una actualización

- El usuario critica o pide cambiar una funcionalidad ya implementada.
- Se descubre un fallo, una limitación o un desequilibrio.
- Se propone una mejora, refactorización o característica nueva.
- Se revisa un expediente de revisión y hay observaciones que aplicar.

### Flujo obligatorio

1. Leer `AGENTS.md`, el tracker y `docs/updates/README.md`.
2. Consultar el elemento afectado: etapa, tarea, funcionalidad, sistema o
   actualización.
3. Revisar el código real y los registros de implementación.
4. Analizar las observaciones del usuario sin inventar datos ausentes.
5. Preparar una propuesta complementaria con alcance, tareas, criterios,
   pruebas, riesgos y referencias a tareas históricas.
6. Registrar una nueva actualización en estado `PROPUESTA` o
   `PENDIENTE_APROBACION`, con `approval.status = "PENDIENTE"`.
7. **Esperar la aprobación explícita del usuario.** No implementar nada todavía.
8. Tras la aprobación, cambiar la actualización a `APROBADA` y registrar quién
   aprobó, cuándo y con qué base.
9. Trabajar **una tarea de actualización por sesión** (`UPD-XXX-TNN`).
10. Registrar evidencias, actualizar el tracker, el log, las decisiones y el
    panel; después detenerse.

### Estados de actualización

| Estado | Uso |
|---|---|
| `PROPUESTA` | Idea registrada; sin autorización para implementar. |
| `EN_REVISION` | Se está contrastando alcance, código y observaciones. |
| `PENDIENTE_APROBACION` | Plan listo; espera decisión del usuario. |
| `APROBADA` | Autorizada; aún sin tareas activas. |
| `EN_PROGRESO` | Hay tareas de implementación activas. |
| `BLOQUEADA` | Existe un impedimento documentado. |
| `COMPLETADA` | Tareas implementadas y verificadas. |
| `RECHAZADA` | Se decidió no continuar; se conserva como antecedente. |
| `ARCHIVADA` | Se conserva como antecedente sin trabajo previsto. |

El estado de una actualización es independiente del estado de sus tareas. Una
actualización en `PROPUESTA`, `EN_REVISION` o `PENDIENTE_APROBACION` no puede
tener tareas `EN_PROGRESO` ni `COMPLETADO`.

### Identificadores y estructura

- Actualización: `UPD-001`, `UPD-002`, …
- Tarea: `UPD-001-T01`, `UPD-001-T02`, …
- Documento: `docs/updates/UPD-001.md`.
- Las tareas de actualización viven en `updates[].tasks` del tracker, no en
  `tasks`; así no alteran el progreso del plan base.
- Una tarea nueva puede depender de una tarea histórica mediante
  `dependencies` o `relatedTaskIds`. La tarea histórica **no cambia de estado**.
- No se reutilizan IDs ni se renumeran tareas existentes.

### Propuestas y aprobación

- El panel permite redactar una propuesta y descargarla en `.md` o `.txt`, además
  de copiar un bloque JSON para el tracker.
- El navegador no puede escribir en el directorio del proyecto desde `file://`.
  Los borradores de `localStorage` son una comodidad, no la fuente canónica.
- Un agente guarda el documento en `docs/updates/`, registra la actualización en
  el tracker y ejecuta `python3 tools/sync-dashboard.py`.
- La aprobación se registra en `approval` con `approvedBy`, `approvedAt` y
  `basis`. Sin esos datos, la actualización no puede avanzar.

### Expediente de revisión

1. En el panel, pulsar **Preparar expediente de revisión**.
2. Seleccionar etapas, tareas, funcionalidades, sistemas, tareas de
   actualización y/o actualizaciones completadas.
3. Descargar el documento en Markdown o TXT.
4. Añadir la sección **OBSERVACIONES DEL USUARIO**.
5. Entregarlo a un agente para contrastar las observaciones con el código real.
6. El agente prepara una propuesta nueva; no modifica el plan histórico.

El expediente refleja el tracker. Si falta un archivo, una prueba o un resultado,
debe indicarlo como no documentado; no se inventan detalles técnicos.

### Reglas del panel

- El plan base aparece como historial contraíble.
- Las actualizaciones se ordenan cronológicamente.
- Solo la actualización actual se expande automáticamente; las anteriores se
  pueden expandir a mano y no se eliminan.
- El panel no es una fuente de verdad: lee el tracker embebido y, por HTTP,
  puede releer `IMPLEMENTATION_TRACKER.json`.
- No se añaden servidores, backends, bases de datos, CDN ni APIs.


## Estado actual

No mantengas aquí un resumen manual de progreso: quedaría desactualizado. El
estado real está en:

- `docs/STATUS.md` — resumen generado (etapa, tarea actual, próxima tarea,
  conteos, decisiones pendientes y últimas sesiones).
- `docs/IMPLEMENTATION_TRACKER.json` — fuente canónica de etapas, tareas,
  decisiones, sesiones, incidencias y actualizaciones.
- `docs/INDEX.md` — mapa de sistemas para localizar código, diseño y pruebas.

El plan base está cerrado; los cambios posteriores se registran como
actualizaciones (`docs/updates/`). No confundas “plan escrito” con
“funcionalidad implementada”.

## Documentación existente

- [Estado resumido (generado)](docs/STATUS.md)
- [Índice de sistemas](docs/INDEX.md)
- [Plan maestro](docs/MASTER_PLAN.md)
- [Tracker](docs/IMPLEMENTATION_TRACKER.json)
- [Log de implementación](docs/IMPLEMENTATION_LOG.md)
- [Decisiones](docs/DECISIONS.md)
- [Decisiones cerradas](docs/PENDING_DECISIONS.md)
- [Actualizaciones complementarias](docs/updates/README.md)
- [Panel](docs/dashboard/index.html)
- [Build, dependencias y layout](docs/build.md)
- [Pruebas y telemetría](docs/pruebas.md)
- [Arquitectura, render y piloto automático](docs/arquitectura.md)
- [Convenciones](docs/convenciones.md)

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
python3 tools/sync-dashboard.py
python3 tools/sync-dashboard.py --check
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
no rompe lo existente, está registrada en el tracker, tiene entrada en el log,
las decisiones están documentadas, `docs/INDEX.md` sigue siendo correcto y el
panel y `docs/STATUS.md` están sincronizados.

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
- No inventar clases, archivos, pruebas ni rutas en `docs/INDEX.md`.
- No empezar la siguiente tarea después de terminar la actual.
