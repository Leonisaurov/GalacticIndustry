# GalacticIndustry

Juego Android en **Java + libGDX 1.14.2**. Build canónico: **apkc + apkc-deps**
(sin Gradle). `app/module.toml` es solo un puente para CodeAssist.

## Cómo continuar el proyecto (obligatorio)

Este repositorio usa un sistema persistente de continuidad. **No dependas de la
memoria de una conversación anterior.** El estado real está en archivos.

Al empezar cualquier sesión, en este orden:

1. Lee este `AGENTS.md`.
2. Lee `docs/MASTER_PLAN.md` (visión, arquitectura y etapas).
3. Lee `docs/IMPLEMENTATION_TRACKER.json` (fuente canónica del estado).
4. Lee `docs/IMPLEMENTATION_LOG.md` (últimas sesiones).
5. Lee `docs/DECISIONS.md` (decisiones pendientes y aprobadas).
6. Consulta `docs/PENDING_DECISIONS.md` para ver si queda alguna decisión abierta.
7. Inspecciona el código real antes de modificarlo.
8. Localiza la tarea `EN_PROGRESO` o `LISTO` y sus dependencias.
9. Implementa **una sola tarea**.
10. Ejecuta sus pruebas y verifica sus criterios de aceptación.
11. Actualiza tracker, log, decisiones, documentación y panel.
12. Detente. No empieces automáticamente la siguiente tarea.

> **Regla de oro:** una sesión = una tarea. No implementes una etapa completa
> ni te adelantes a funcionalidades posteriores.

## Archivos de continuidad

| Archivo | Responsabilidad |
|---|---|
| `AGENTS.md` | Punto de entrada y protocolo |
| `docs/MASTER_PLAN.md` | Plan maestro, arquitectura, fórmulas, riesgos |
| `docs/IMPLEMENTATION_TRACKER.json` | **Fuente canónica** de etapas, tareas, estados, dependencias y progreso |
| `docs/IMPLEMENTATION_LOG.md` | Historial cronológico de sesiones |
| `docs/DECISIONS.md` | Decisiones de producto y técnicas |
| `docs/PENDING_DECISIONS.md` | Resumen de decisiones cerradas; ya no hay pendientes |
| `docs/dashboard/index.html` | Panel visual autocontenido; datos embebidos en `file://` y refresco JSON por HTTP |
| `docs/dashboard/dashboard.css` | Estilos del panel |
| `docs/dashboard/dashboard.js` | Lógica del panel (filtros, temas, enlaces y render) |
| `docs/PROJECT_DASHBOARD.html` | Redirección de compatibilidad a `docs/dashboard/index.html` |
| `tools/sync-dashboard.py` | Validación del tracker y sincronización JSON → HTML |

No edites a mano el bloque `tracker-data` de `docs/dashboard/index.html` salvo que sea imprescindible.
Después de modificar el JSON, ejecuta:

```sh
python3 tools/sync-dashboard.py          # valida y sincroniza el panel
python3 tools/sync-dashboard.py --check  # solo valida y comprueba el HTML
```

El panel se abre con doble clic; no necesita servidor, Node.js, Python como
servidor, CDN ni red.

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
   - evidencias y `executionLog`;
   - sesión en `sessions`.
5. Añade la sesión a `docs/IMPLEMENTATION_LOG.md`.
6. Actualiza `docs/DECISIONS.md` si tomaste una decisión.
7. Actualiza el plan maestro si cambió el diseño o las dependencias.
8. Ejecuta `python3 tools/sync-dashboard.py`.
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

## Estado actual

El estado real está siempre en el tracker. Al cierre de la sesión de preparación:

- Etapa actual: `S9` — Estación espacial personal.
- No hay tarea en progreso.
- Última tarea completada: `T-901` — Crear PlayerStation y ModuleDef para plataforma vacía.
- Primera tarea disponible: `T-902` — Construir el núcleo/plataforma vacía.
- Decisiones: **todas cerradas**; D-012 (facciones/combate/online) queda aplazada.
- La implementación del juego está al **74 %** (60 de 81 tareas de progreso).
- `MyGame` ya no es un monolito: el dibujo vive en `UiKit`, `PlayHud`,
  `StationScreen`, `OverlayRenderer` y `SpaceBackground` (T-007/T-504).

No confundas “plan escrito” con “funcionalidad implementada”.

## Documentación existente

- [Plan maestro](docs/MASTER_PLAN.md)
- [Tracker](docs/IMPLEMENTATION_TRACKER.json)
- [Log de implementación](docs/IMPLEMENTATION_LOG.md)
- [Decisiones](docs/DECISIONS.md)
- [Decisiones cerradas](docs/PENDING_DECISIONS.md)
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
las decisiones están documentadas y el panel está sincronizado.

Una etapa está terminada cuando todas sus tareas de progreso están
`COMPLETADO` o `POSPUESTO` con justificación, y su gate se ha verificado.

## No hacer

- No implementar varias tareas o una etapa completa en una sesión.
- No marcar `COMPLETADO` sin pruebas.
- No inventar estados en el panel.
- No usar `fetch()` como única fuente del panel: con `file://` debe leer el JSON embebido; si se sirve por HTTP puede releer `IMPLEMENTATION_TRACKER.json` sin recargar la página y, si falla, debe mantener los datos actuales.
- No añadir dependencias, servicios, servidores ni APIs para el panel.
- No cambiar IDs, dependencias o estados sin registrarlo.
- No empezar la siguiente tarea después de terminar la actual.
