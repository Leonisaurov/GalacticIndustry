# Panel de continuidad

Panel visual autocontenido de GalacticIndustry. Se abre con doble clic en
`index.html` y funciona con `file://` sin servidor, CDN ni red.

## Archivos

| Archivo | Responsabilidad |
|---|---|
| `index.html` | Estructura del panel, plantilla única de versión, bloque `tracker-data` con los datos embebidos para `file://` y bloque `derived-data` con los indicadores derivados |
| `dashboard.css` | Tema oscuro/claro, selector, línea de tiempo, tarjetas, tablas, badges y responsive |
| `dashboard.js` | Modelo de versiones, selector compacto/descriptivo, línea de tiempo, contenido dinámico, filtros, propuestas, expedientes, temas y refresco por HTTP. Lee los indicadores de `derived-data`; si el bloque falta, usa el cálculo local de respaldo |

`docs/PROJECT_DASHBOARD.html` se conserva como redirección de compatibilidad
para enlaces antiguos.

## Organización por versiones

La implementación inicial es la versión visual **0.1**. Cada actualización
`UPD-XXX` tiene una etiqueta `displayVersion` estable:

| Versión | Identificador | Contenido |
|---|---|---|
| 0.1 | `BASE` | Plan maestro original (`stages` + `tasks`) |
| 0.2 | `UPD-001` | Sistema de actualizaciones y revisiones |
| 0.3 | `UPD-002` | Contexto por sistema y coordinación concurrente |
| 0.4 | `UPD-003` | Identidad visual industrial |
| 0.5 | `UPD-004` | Compra e instalación de módulos de nave |
| 0.6 | `UPD-005` | Dashboard por versiones, selector y línea de tiempo |
| 0.7 | `UPD-006` | Exploración, navegación, personaje y comienzo de partida |
| 0.8 | `UPD-007` | Renovación gráfica del mundo (propuesta pendiente de aprobación) |
| 0.9 | `UPD-008` | Rediseño y organización del dashboard (carta estelar) |
| 0.10 | `UPD-009` | Scroll interno en listas largas y pulido del panel |
| 0.11 | `UPD-010` | Reconstrucción visual del panel: carta náutica técnica |
| 0.12 | `UPD-011` | Indicadores derivados canónicos y verificación comprobable |

La numeración es solo de presentación. Los identificadores internos (`BASE`,
`UPD-XXX`, `T-XXX`, `S-XXX`) no cambian. `displayVersion` se guarda en el
tracker para que las etiquetas históricas no cambien al añadir actualizaciones.

## Selector y línea de tiempo

- **Selector**: lista horizontal de la versión más reciente a la más antigua.
  El elemento seleccionado se destaca con un marcador dorado. Comparte estado
  con la línea de tiempo. Cada tarjeta compacta muestra estado y una barra mini
  de progreso; el modo descriptivo añade nombre, descripción, objetivo y cifras.
  Se puede recorrer con las flechas, `Inicio` y `Fin` cuando el foco está en el
  selector.
- **Modo compacto** (predeterminado): número de versión, identificador, estado y
  barra de progreso.
- **Modo descriptivo**: además muestra nombre, descripción, objetivo y progreso
  con cifras. La preferencia se guarda en `localStorage` (`gi-version-mode`); no
  es fuente de verdad.
- **Línea de tiempo**: de `0.1` a la versión más reciente, con puntos
  seleccionables, anillo de progreso por versión, cifras tabulares y
  desplazamiento horizontal con fundido en los bordes. Se auto-centra en la
  versión seleccionada. No representa versiones futuras.

Al abrir el panel se selecciona `meta.currentUpdateId`. Si no existe, se elige la
actualización activa más reciente; si no hay activas, la versión más reciente
registrada. Una propuesta pendiente se muestra con su estado y un aviso, nunca
como trabajo en desarrollo.

## Plantilla dinámica

Debajo del selector y la línea de tiempo hay una sola plantilla. Solo se
renderiza la versión seleccionada:

1. Identificación: nombre, versión, identificador, estado, fecha, descripción y
   objetivo.
2. Progreso: porcentaje de implementación y conteos de completadas, en progreso,
   listas, pendientes y bloqueadas.
3. Etapas o grupos: las etapas `S0`–`S11` solo para la versión `0.1`; grupos
   propios si la actualización declara `groups`; lista plana si solo tiene
   tareas.
4. Tareas: búsqueda y filtros por estado, etapa/grupo, prioridad y orden. Cada
   tarea se expande con dependencias, contexto, artefactos, pruebas, evidencias
   y registro.
5. Historial: solo sesiones y registros relacionados con la versión.
6. Pendientes: tareas bloqueadas, con decisión o pospuestas de la versión.
7. Ficha completa: aprobación, sistemas, funcionalidades, referencias, criterios
   y observaciones de la actualización.

La información global (resumen, estado actual, propuestas pendientes de
aprobación, decisiones, problemas, tareas bloqueadas de otras versiones,
actividad reciente y reglas) vive después del contenido de la versión y no
cambia al seleccionar otra.

### Listas largas con scroll interno

Las listas que crecen con los datos no alargan la página: cada una tiene una
altura máxima proporcional al viewport y desplazamiento interno. Se aplica a
decisiones, problemas técnicos, tareas bloqueadas, aprobaciones pendientes,
tareas de la versión, historial de la versión y actividad reciente. Un fundido
en el borde superior o inferior (según la posición) y la barra de desplazamiento
indican que hay más contenido; ambos desaparecen cuando la lista cabe entera o
se llega a ese extremo. Cada contenedor es enfocable por teclado
(`tabindex="0"`, `role="region"`, `aria-label`) y las teclas de dirección lo
desplazan. El scroll se conserva al refrescar datos y se reinicia al cambiar de
versión o de filtros.

### Accesibilidad y estados

- Todos los controles (botones, chips, selectores de versión, puntos de la línea
  de tiempo, cierre de modales y volver arriba) definen hover, foco visible,
  activo y deshabilitado. El foco visible usa el anillo dorado del tema.
- Los modales abren con el foco en su primer campo, atrapan `Tab` y `Shift+Tab`,
  cierran con `Escape`, marcan el resto de la página como `inert` +
  `aria-hidden` y devuelven el foco al control que los abrió.
- La posición, el resumen de versión y la distribución usan `aria-live` para
  anunciar cambios; la navegación marca la sección activa con `aria-current`.
- Con `prefers-reduced-motion: reduce` se desactivan transiciones y
  transformaciones.

### Regla de progreso

El porcentaje usa solo tareas `category=implementation` con
`countsForProgress=true` de la versión seleccionada. Si no hay tareas de
implementación, se muestra `—`, no un porcentaje ficticio. Las versiones
`RECHAZADA` o `ARCHIVADA` no contabilizan tareas pendientes. Las tareas
`POSPUESTO` y `REQUIERE_DECISION` se muestran aparte y no se suman a
`PENDIENTE`.

## Datos

`index.html` es la única parte que contiene el tracker integrado, porque
`file://` no permite leer `IMPLEMENTATION_TRACKER.json` con `fetch()`. Hay
**dos bloques generados**, ambos con marcadores propios:

| Bloque | Marcadores | Contenido |
|---|---|---|
| `tracker-data` | `TRACKER_DATA_START` / `TRACKER_DATA_END` | Copia exacta del JSON canónico |
| `derived-data` | `DERIVED_DATA_START` / `DERIVED_DATA_END` | Indicadores calculados por `vista_derivada()` en Python |

`derived-data` vive **fuera** del bloque `tracker-data`: añadirlo dentro
rompería `embedded_tracker()` y la comparación con el JSON canónico. Lo emite
`tools/sync-dashboard.py` de forma determinista (solo depende del tracker, sin
fechas de ejecución) y contiene progreso base, preparación, conteos por estado,
progreso por actualización, mapa `id → displayVersion`, la regla de
«Actualización actual», la versión seleccionada por defecto, los conteos de
aprobaciones y el resultado de la verificación. El JS **lee** esos valores en
lugar de recalcularlos; si el bloque no existe o no es legible (HTML antiguo),
usa el cálculo local de respaldo.

```sh
python3 tools/sync-dashboard.py          # panel + docs/STATUS.md + ambos bloques
python3 tools/sync-dashboard.py --check  # falla con exit 1 si algo diverge
```

`--check` compara los tres artefactos: `tracker-data` contra el JSON canónico,
`derived-data` contra `vista_derivada(tracker)` y `STATUS.md` contra
`status_markdown(tracker)`. No edites ninguno a mano.

**Una sola regla de «Actual».** `actualizacionActual` (para `top-update` y
`STATUS.md`) siempre es trabajo autorizado: `meta.currentUpdateId` si el tracker
lo fija, si no la activa autorizada más reciente y, si no hay activas, la
autorizada más reciente. Una propuesta `PROPUESTA`, `EN_REVISION` o
`PENDIENTE_APROBACION` **nunca** se presenta como trabajo en desarrollo.
`versionPorDefecto` es distinta: reproduce `defaultVersionId()` de `UPD-005`
§6.3 y sí puede seleccionar la versión más reciente registrada, porque se
muestra con su estado real y su aviso.

El badge **Última verificación** ya no lee `meta.lastVerification` (un valor
autodeclarado sin escritor): muestra `DERIVED.verificacion`, emitido solo tras
`validate()` sin errores y con la lista de comprobaciones que `--check`
ejecuta de verdad.

Si el panel se sirve por HTTP, el botón **Actualizar** relee
`../IMPLEMENTATION_TRACKER.json` sin recargar la página. El `fetch` valida forma
mínima y que `meta.lastUpdated` coincida con `derivadoDe`; si no pasa, conserva
los embebidos y sus derivados con indicador `warn`, nunca `ok`. Así el panel no
mezcla KPIs de dos versiones distintas del tracker. No se reimplementa
`validate()` de Python en JS.

La sincronización usa `tools/continuity_io.py`: toma un bloqueo `flock` breve,
relee el tracker, valida y escribe `STATUS.md` y el HTML de forma atómica. Dos
sesiones pueden sincronizar a la vez; el resultado refleja el tracker más
reciente. Si el proceso muere entre las dos escrituras, los derivados pueden
quedar desfasados hasta la siguiente sincronización. Ver
[`../CONCURRENCY.md`](../CONCURRENCY.md).

## Actualizaciones y revisiones

El selector conserva las acciones **Nueva propuesta** y **Preparar expediente de
revisión**:

- **Nueva propuesta**: redacta una propuesta, la guarda como borrador local y
  permite descargar `.md`, `.txt` y copiar un bloque JSON para el tracker. El
  bloque incluye `displayVersion` y `objective`.
- **Preparar expediente de revisión**: selecciona etapas, tareas, tareas de
  actualización, sistemas, funcionalidades y actualizaciones; descarga un
  expediente `.md` o `.txt` con la sección final `OBSERVACIONES DEL USUARIO`.

Los documentos se generan con `Blob` y `URL.createObjectURL`. No se escribe en
el proyecto desde el navegador: con `file://` eso no es posible. Los borradores
de `localStorage` son temporales y no son la fuente canónica. La persistencia
real es el tracker y los archivos de `docs/updates/`, actualizados por un agente.

Flujo completo: [`docs/updates/README.md`](../updates/README.md).

## Pruebas

```sh
tools/run-tests.sh --tools              # los cuatro comandos, sin dispositivo ni build
python3 tools/sync-dashboard.py --check
python3 tools/tests/continuity_test.py
node tools/tests/dashboard_versions_test.js
python3 tools/tests/dashboard_versions_test.py
```

`--tools` no forma parte de `--all` (el runner solo descubre
`[0-9][0-9]_*.sh`). `continuity_test.py` incluye mutaciones de estado,
`currentUpdateId`, `displayVersion` y del propio bloque derivado que **deben**
hacer fallar `--check`. La prueba de renderizado ejecuta `dashboard.js` en un
DOM mínimo de Node y comprueba selector, línea de tiempo, modos, contenido por
versión, progreso aislado, ausencia de etapas inventadas, pendientes globales,
estados de propuesta, que los KPIs leen `derived-data` (con respaldo local si
falta) y que un `fetch` con forma inválida o `lastUpdated` desincronizado se
rechaza con `warn` conservando los embebidos. No sustituye una revisión visual
en navegador, pero detecta regresiones de datos y de plantilla sin red.
