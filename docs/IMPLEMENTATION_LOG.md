# REGISTRO DE IMPLEMENTACIÓN — GalacticIndustry

**Última actualización:** 2026-11-08T02:24:00-06:00  
**Fuente canónica de estado:** `docs/IMPLEMENTATION_TRACKER.json`  
**Plan maestro:** `docs/MASTER_PLAN.md`  
**Preguntas para conversar:** `docs/PENDING_DECISIONS.md`

Este archivo es el historial cronológico de las sesiones. El estado operativo de cada tarea vive en el tracker; aquí se explica qué ocurrió, qué se probó y qué quedó pendiente.

## Formato de una entrada

```markdown
## S-XXX — Título de la sesión

- **Fecha:** YYYY-MM-DDTHH:MM:SS-06:00
- **Agente:** nombre/modelo
- **Tarea(s):** T-XXX
- **Resultado:** COMPLETADO | EN_PROGRESO | BLOQUEADO | REQUIERE_DECISION
- **Archivos modificados:** ...
- **Pruebas ejecutadas:** ...
- **Evidencias:** ...
- **Decisiones tomadas:** ...
- **Pendiente / siguiente paso:** ...
```

Reglas:

- Una sesión debe registrar **una tarea principal**. Si se tocaron varias, explicar por qué.
- No declarar una tarea completada sin pruebas y criterios verificados.
- Si la sesión se interrumpe, dejar la tarea `EN_PROGRESO` y anotar exactamente qué falta.
- No borrar entradas antiguas.
- No reescribir la historia para que parezca más limpia.

---

## S-041 — Nave nodriza del sector inicial

- **Fecha:** 2026-11-08T02:24:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-501
- **Resultado:** COMPLETADO

### Resumen

- `StationDef` define nombre, radio visual 200, radio de acople 260, zona
  segura 480 y seis servicios.
- `Station` instancia la nodriza en `(0, 480)` con almacén de 2000 UM, giro
  lento y dibujo a gran escala (anillo, brazos, núcleo y bahía).
- `GameWorld.generateChunk()` descarta asteroides dentro de la zona segura.
- Telemetría: `stationX`, `stationY`, `stationRadius`, `stationDistance` y
  `stationAsteroids`.
- Nuevo `station_test` y prueba `13_station` con captura `25_station`.

### Archivos modificados

- `src/com/example/codegame/StationDef.java` (nuevo)
- `src/com/example/codegame/Station.java` (nuevo)
- `src/com/example/codegame/GameData.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/MainActivity.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/13_station.sh` (nuevo)
- `docs/arquitectura.md`
- `docs/pruebas.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh 13_station`: **OK**.
- `12_mining 04_autopilot 03_controls 07_visual_play 13_tractor 16_universe`: **6/6 OK**.

### Evidencias

- `PASS la nodriza existe en el sector inicial`
- `PASS la nodriza tiene radio, acople y zona segura`
- `PASS la nodriza ofrece todos los servicios`
- `PASS la nodriza tiene almacen`
- `PASS la zona segura no tiene asteroides`
- `13_station`: distancia 330, radio 200 y 0 asteroides en la zona segura.
- Captura `25_station`: anillo, núcleo y bahía visibles a escala.

### Decisiones tomadas

- La nodriza es fija en `(0, 480)` para quedar cerca del origen sin tapar el
  arranque.
- La zona segura se aplica al generar chunks, no eliminando asteroides después.
- El acople y los servicios quedan fuera de T-501 (T-502..T-505).

### Pendiente / siguiente paso

- Continuar con **T-502 — Implementar acoplamiento y Autopilot.STATION**.

---

## S-040 — Panel de continuidad dividido y mejorado

- **Fecha:** 2026-11-08T02:20:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** — (infraestructura de continuidad)
- **Resultado:** COMPLETADO

### Resumen

- El panel deja de ser un monolito: `docs/dashboard/` separa estructura,
  estilos y lógica.
  - `index.html`: estructura y bloque `tracker-data` embebido para `file://`.
  - `dashboard.css`: tema oscuro/claro, tarjetas, badges, tablas y responsive.
  - `dashboard.js`: render, filtros, orden, temas, enlaces y refresco HTTP.
  - `README.md`: mapa de archivos y flujo de sincronización.
- `docs/PROJECT_DASHBOARD.html` queda como redirección de compatibilidad.
- Mejoras de lectura y uso:
  - tema claro/oscuro persistido y aplicado antes del primer pintado;
  - ocho tarjetas de resumen y distribución interactiva por estado;
  - etapa actual resaltada, gate visible y tarjeta de última verificación;
  - búsqueda por varios términos, orden configurable y agrupación por etapa;
  - enlaces directos `#task-T-XXX`, botón «Abrir en la lista» y atajo `/`;
  - historial plegable, decisiones resolubles/ocultables e incidencias con
    severidad, estado y tareas afectadas;
  - sección de reglas y definiciones de estado;
  - botón de volver arriba y estilos de impresión.
- `tools/sync-dashboard.py` apunta a `docs/dashboard/index.html` y comprueba que
  existan `dashboard.css` y `dashboard.js`.

### Archivos modificados

- `docs/dashboard/index.html`
- `docs/dashboard/dashboard.css`
- `docs/dashboard/dashboard.js`
- `docs/dashboard/README.md`
- `docs/PROJECT_DASHBOARD.html` (redirección)
- `tools/sync-dashboard.py`
- `AGENTS.md`
- `docs/MASTER_PLAN.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `python3 tools/sync-dashboard.py --check`: **OK**.
- Firefox headless: capturas a `1400×1200` y `390×1600` en tema claro y oscuro.
- Marionette: **17/17** comprobaciones del panel.
- Servidor HTTP local: el botón **Actualizar** relee el tracker sin recargar la
  página y muestra `JSON en vivo`.

### Evidencias

- `PASS` resumen con 8 tarjetas, 12 etapas y 92 tareas.
- `PASS` filtro por segmento, limpieza de filtros, agrupación y desagrupación.
- `PASS` búsqueda, orden por ID, cambio de tema y atajo `/`.
- `PASS` historial desplegable, decisiones plegables y enlace directo a tarea.
- `PASS` fuente `embebidos (file://)` y recarga `JSON en vivo` por HTTP.
- No se modificó ninguna tarea ni código de juego.

### Decisiones tomadas

- Separar CSS y JS del HTML; los datos siguen embebidos en `index.html` porque
  `file://` no permite leer el JSON con `fetch()`.
- Mantener `docs/PROJECT_DASHBOARD.html` como redirección para no romper enlaces
  antiguos.
- No añadir dependencias, servicios ni CDN.

### Pendiente / siguiente paso

- Continuar con **T-501 — Crear Station y la nave nodriza del sector inicial**
  (Etapa 5). El panel ya refleja `S5` y el 38 % de implementación.

---

## S-039 — Verificación y cierre de minería y recursos

- **Fecha:** 2026-11-08T02:14:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-408
- **Resultado:** COMPLETADO

### Resumen

- Añadido un bloque de **verificación integrada S4** al `SelfTest`:
  - tiempos C/S/M distintos y dentro del plan;
  - minado completo de una roca M (50 UM) hasta agotarla;
  - sin monedas, capacidad respetada y valor de fundición.
- Ejecutadas las pruebas de la etapa: lógica, minería, tractor, autopiloto,
  carga y fundición.
- Documentado el **gate de la Etapa 4** en `docs/pruebas.md`.
- **S4 cerrada**; **S5 lista**.

### Archivos modificados

- `src/com/example/codegame/SelfTest.java`
- `docs/pruebas.md`
- `docs/MASTER_PLAN.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 12_mining 13_tractor 04_autopilot 11_cargo 19_melt`: **5/5 OK**.

### Evidencias

- `PASS S4: C, S y M tienen tiempos distintos`
- `PASS S4: los tiempos caben en el plan`
- `PASS S4: minar M agota la roca y llena la bodega`
- `PASS S4: el tiempo de minado M es razonable`
- `PASS S4: minar no da monedas`
- `PASS S4: la fundicion temporal valora el mineral`
- `PASS S4: el mineral extraido respeta la capacidad`

### Decisiones tomadas

- La verificación integrada vive en `SelfTest` y el gate se documenta en
  `docs/pruebas.md`.
- No se implementa mercado ni estación en esta etapa.

### Pendiente / siguiente paso

- Empezar la Etapa 5 con **T-501 — Crear Station y la nave nodriza del sector
  inicial**.

---

## S-038 — Rayo tractor y captura de fragmentos

- **Fecha:** 2026-11-08T02:12:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-410
- **Resultado:** COMPLETADO

### Resumen

- `TractorBeam` atrae automáticamente fragmentos dentro de su alcance:
  - alcance `140 + 25·nivel`;
  - velocidad `120 + 30·nivel`;
  - hasta `2 + nivel` fragmentos simultáneos.
- Consume **1,5 energía/s** mientras atrae; comparte capacitor con el láser y
  el láser tiene prioridad si está minando.
- La nave captura el fragmento al tocarlo; el tractor solo lo acerca.
- Mejora `TRACTOR` en la tienda (alcance, velocidad y capacidad), aplicada con
  `Ship.syncTractor()` y persistida en el loadout.
- Haces verde-cian y HUD `ENERGIA xx%  TRACTOR n`.
- Telemetría: `tractorActive`, `tractorTargets`, `tractorLevel`.
- Nuevo `tractor_test` y prueba `13_tractor`.
- Migración: guardados con 7 niveles cargan con Tractor 0.

### Archivos modificados

- `src/com/example/codegame/TractorBeam.java`
- `src/com/example/codegame/EnergyCapacitor.java`
- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/ShipLoadout.java`
- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/PlayHud.java`
- `src/com/example/codegame/Visuals.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/MainActivity.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/13_tractor.sh` (nuevo)
- `docs/pruebas.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh 13_tractor`: **OK**.
- `12_mining 04_autopilot 03_controls 09_wing_damage 19_melt 07_visual_play`: **6/6 OK**.

### Evidencias

- `PASS tractor base sin mejora`
- `PASS la mejora Tractor se compra`
- `PASS la nave aplica alcance y velocidad del tractor`
- `PASS el tractor atrae el fragmento`
- `PASS el tractor consume energia`
- `PASS fuera de alcance no se atrae`
- `PASS el tractor respeta su capacidad`
- `PASS el nivel de tractor se guarda/restaura`
- `PASS un v2 con 7 niveles carga con Tractor 0`
- `13_tractor`: 3 fragmentos capturados, bodega 12 UM, sin monedas.
- Captura `24_tractor`: haz verde-cian y `ENERGIA 98% TRACTOR 1`.

### Decisiones tomadas

- El tractor es automático: no añade un botón ni compite con el joystick.
- Solo drena energía mientras hay un fragmento atraído.
- El láser tiene prioridad de drenaje si ambos sistemas están activos.
- La capacidad mejorable se interpreta como fragmentos simultáneos.

### Pendiente / siguiente paso

- Continuar con **T-408 — Verificación de minería y recursos** para cerrar S4.

---

## S-037 — Refactor de MyGame en módulos de UI

- **Fecha:** 2026-11-08T02:00:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-007
- **Resultado:** COMPLETADO

### Resumen

`MyGame` deja de ser un monolito: pasa de **2.623 a ~1.460 líneas** y delega
el dibujo en módulos con estado explícito:

| Clase | Responsabilidad |
|---|---|
| `UiKit` | Paneles, botones, iconos, pips, barras y texto. |
| `SpaceBackground` | Degradado, estrellas con parallax, nebulosas y planeta. |
| `PlayHud` | Panel superior, combustible, energía, casco, supervivencia, avisos y controles. |
| `ShopRenderer` | Pestañas y tarjetas de mejoras, reparaciones y fundición. |
| `OverlayRenderer` | Menú, pausa, fin de partida y fundido de muerte. |
| `HudView` | Estado efímero reutilizable para el HUD. |

`MyGame` conserva el flujo de estados, la entrada, el guardado, la telemetría y
la coordinación. No cambia ningún comportamiento visible.

### Archivos modificados

- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/UiKit.java` (nuevo)
- `src/com/example/codegame/SpaceBackground.java` (nuevo)
- `src/com/example/codegame/PlayHud.java` (nuevo)
- `src/com/example/codegame/ShopRenderer.java` (nuevo)
- `src/com/example/codegame/OverlayRenderer.java` (nuevo)
- `src/com/example/codegame/HudView.java` (nuevo)
- `docs/arquitectura.md`
- `docs/MASTER_PLAN.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `06_visual_menu 07_visual_play 08_visual_death 09_wing_damage 10_survival 11_cargo 12_mining 19_melt`: **8/8 OK**.
- `03_controls 04_autopilot 05_death 16_universe`: **4/4 OK**.

### Evidencias

- `wc -l MyGame.java`: 2.623 → ~1.460.
- Capturas de menú, partida, tienda, muerte, HUD, carga y minería correctas.
- Sin cambios en la lógica de juego ni en el guardado.

### Decisiones tomadas

- Los renderizadores reciben referencias estables y un `HudView` reutilizable;
  no se asignan objetos por frame.
- `MyGame` sigue siendo el dueño del estado y del flujo; los módulos solo leen
  o pintan.
- El refactor es estructural: no adelanta funcionalidad de T-410.

### Pendiente / siguiente paso

- Retomar **T-410 — Implementar rayo tractor y captura de fragmentos**
  (`TractorBeam` y `EnergyCapacitor.drain` están en el commit WIP).

---

## S-036 — Fragmentos persistentes y pérdida configurable

- **Fecha:** 2026-11-08T01:47:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-409
- **Resultado:** COMPLETADO

### Resumen

- `OreFragment` se guarda con posición, velocidad, mineral, cantidad y vida.
- `SaveCodec` v2 escribe/lee `fragments`; los guardados antiguos sin ese campo
  cargan con la lista vacía.
- El formato legacy (`serialize`/`restore`) usa líneas `o` y también conserva
  los fragmentos.
- `fragmentShare` y `fragmentLoss` pasan a ser ajustables por mundo, con los
  valores por defecto de T-406.
- El mundo legado (`copyWorldFrom`) conserva los fragmentos; un personaje nuevo
  no los borra.
- La recuperación sigue siendo por contacto; el rayo tractor y sus mejoras
  quedan para T-410.
- Se actualizó el criterio de T-409 para reflejar ese reparto.

### Archivos modificados

- `src/com/example/codegame/OreFragment.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/arquitectura.md`
- `docs/pruebas.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 12_mining 04_autopilot 16_universe`: **3/3 OK**.

### Evidencias

- `PASS los fragmentos se guardan`
- `PASS los fragmentos se restauran`
- `PASS un guardado sin fragmentos carga vacio`
- `PASS el formato legacy conserva fragmentos`
- `PASS la perdida de material es configurable`
- `PASS sin fragmentos todo queda en escombros`
- `12_mining`, `04_autopilot` y `16_universe` OK.

### Decisiones tomadas

- La vida restante del fragmento se conserva al guardar.
- Un guardado sin el campo `fragments` es válido (migración).
- La pérdida se controla con dos floats por mundo, no con constantes fijas.
- El rayo tractor no se adelanta: T-410 lo implementa con sus mejoras y HUD.

### Pendiente / siguiente paso

- Continuar con **T-410 — Implementar rayo tractor y captura de fragmentos**.

---

## S-035 — Mejora de láser, HUD de energía y efectos

- **Fecha:** 2026-11-08T01:43:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-407
- **Resultado:** COMPLETADO

### Resumen

- `LASER` es la séptima mejora:
  - +1.5 de potencia por nivel;
  - +1 de penetración por nivel;
  - +25 de capacidad de capacitor por nivel (100 → 225).
- `Ship.reset()` aplica nivel y capacitor; `syncLaser()` conserva la carga
  relativa al comprar la mejora.
- HUD contextual: barra, rayo y porcentaje de energía solo con el haz activo o
  el capacitor incompleto; color cian/ámbar/rojo según la carga.
- Haz e impacto con color por tipo de roca:
  - C: cian;
  - S: azul claro;
  - M: ámbar.
- El impacto emite chispas del color del mineral extraído.
- Sin creación de objetos en `render()`: colores estáticos y solo floats.
- Telemetría: `energy`, `energyMax`, `mining` y `laserLevel`.
- Migración verificada de guardados con 5 y 6 niveles: cargan con Laser 0.

### Archivos modificados

- `src/com/example/codegame/ShipLoadout.java`
- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/Visuals.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/12_mining.sh`
- `docs/pruebas.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh 12_mining`: **OK**.
- `tools/run-tests.sh --no-build 03_controls 04_autopilot 09_wing_damage 19_melt`: **4/4 OK**.

### Evidencias

- `PASS laser base sin mejora`
- `PASS la mejora Laser se compra`
- `PASS la nave aplica potencia y penetracion`
- `PASS la mejora aumenta el capacitor`
- `PASS la mejora sube la tasa en roca C`
- `PASS la penetracion mejora la roca M`
- `PASS comprar laser conserva la carga relativa`
- `PASS el nivel de laser se guarda/restaura`
- `PASS un v2 con 6 niveles carga con Laser 0`
- `12_mining`: `mining=true`, `energy < energyMax`, captura `23_mining_beam`.

### Decisiones tomadas

- El nivel del láser se deriva del loadout; no se guarda aparte.
- El capacitor mejora con el mismo nivel que potencia/penetración.
- La energía se muestra solo cuando es relevante para no cargar el panel.
- El rayo tractor y su HUD quedan fuera de esta tarea (T-410).

### Pendiente / siguiente paso

- Continuar con **T-409 — Generar fragmentos de mineral y dispersión por misiles
  (estado persistente y recuperación)**.

---

## S-034 — Misiles con fragmentos de mineral

- **Fecha:** 2026-11-08T01:36:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-406
- **Resultado:** COMPLETADO

### Resumen

- Nuevo `OreFragment`: mineral + cantidad + posición/velocidad, con 90 s de vida.
- Al destruir una roca con un misil:
  - 60 % (`FRAGMENT_SHARE`) de su reserva se dispersa en fragmentos;
  - 25 % (`FRAGMENT_LOSS`) se pierde;
  - el resto queda en escombros que heredan el mineral restante.
- El mineral **no** entra directamente en la bodega ni da monedas.
- La nave recoge los fragmentos al tocarlos; si la bodega está llena, siguen
  en el espacio.
- Los fragmentos se dibujan con color por mineral y un brillo propio.
- Corregido un bug de fragmentos fantasma de 1 UM al repartir minerales con
  cantidad cero.
- Telemetría: `fragments` y `fragmentUnits` en el estado.

### Archivos modificados

- `src/com/example/codegame/OreFragment.java` (nuevo)
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/DECISIONS.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 12_mining 04_autopilot 03_controls`: **3/3 OK**.

### Evidencias

- `PASS el misil dispersa fragmentos`
- `PASS el mineral no entra directo en la bodega`
- `PASS romper roca no da monedas`
- `PASS parte del material se pierde`
- `PASS la nave recoge fragmentos`
- `PASS una roca grande se parte en escombros`
- `PASS los fragmentos y escombros no duplican mineral`

### Decisiones tomadas

- `FRAGMENT_SHARE = 0.60` y `FRAGMENT_LOSS = 0.25` son valores iniciales de
  balance, no definitivos.
- La recogida es por contacto; el rayo tractor llega en T-410.
- La persistencia de fragmentos se deja para T-409.

### Pendiente / siguiente paso

- Continuar con **T-407 — Añadir mejoras de láser, HUD de energía y efectos visuales**.

---

## S-033 — Mineral en vez de monedas y fundición

- **Fecha:** 2026-11-08T01:28:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-405
- **Resultado:** COMPLETADO

### Resumen

- Los asteroides ya no dan monedas al destruirlos.
- Minar sigue depositando mineral en la bodega.
- Nueva pestaña `FUNDIR` en la tienda temporal:
  - muestra cada mineral, cantidad, precio y créditos;
  - convierte la entrada completa a precio base;
  - vacía la bodega y suma los créditos al saldo.
- `Upgrades.meltValue()` centraliza la conversión.
- La simulación de SelfTest mide puntos/destrucción, no monedas.
- El puente de fundición es temporal hasta la estación de S5.

### Archivos modificados

- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/Asteroid.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/19_melt.sh` (nuevo)
- `tools/tests/testlib.sh`
- `docs/pruebas.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 12_mining 03_controls 19_melt`: **3/3 OK**.

### Evidencias

- `PASS destruir meteorito no da monedas`
- `PASS minar no da monedas`
- `PASS fundir hielo a precio base`
- `PASS fundir oro a precio base`
- `PASS la simulacion destruye asteroides`
- `19_melt`: 100 Silicato → 200 créditos y bodega vacía.
- `09_wing_damage` sigue abriendo la pestaña REPARAR con las 3 pestañas.

### Decisiones tomadas

- La fundición usa precio base y redondeo entero.
- Se funde la entrada completa; no hay selector de cantidad todavía.
- La pestaña desaparecerá cuando la estación asuma la economía en S5.

### Pendiente / siguiente paso

- Continuar con **T-406 — Misiles: fragmentación y dispersión de mineral**.

---

## S-032 — Autopilot.MINE y selección de roca

- **Fecha:** 2026-11-08T01:21:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-404
- **Resultado:** COMPLETADO

### Resumen

- `Autopilot.MINE` reutiliza la aproximación y evitación existentes.
- Aparca a 180 u, dentro del alcance del láser.
- Mantiene el láser si hay energía, láser operativo y espacio en bodega.
- No dispara misiles en modo MINE.
- Se detiene al agotar la roca o llenar la bodega.
- Tocar una roca selecciona MINE; el disparo manual sigue disponible.
- Añadido `mining_test` y `12_mining`.

### Archivos modificados

- `src/com/example/codegame/Autopilot.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/MainActivity.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/12_mining.sh` (nuevo)
- `docs/pruebas.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 12_mining`: **OK**.
- `tools/run-tests.sh --no-build 04_autopilot 03_controls`: **2/2 OK**.

### Evidencias

- `PASS el piloto entra en modo MINE`
- `PASS MINE mantiene una distancia segura`
- `PASS MINE llena la bodega`
- `PASS MINE no dispara misiles`
- `PASS MINE termina al agotar la roca`
- `PASS MINE se detiene con la bodega llena`
- `12_mining`: 15 UM, masa 14+, sin misiles ni monedas.
- Captura `22_mining.png`: `CARGA 15/100 UM` y sin roca.

### Decisiones tomadas

- MINE aparca a 180 u para quedar dentro del alcance de 260 u.
- Si la bodega está llena, el piloto pasa a STOP.
- Si falta energía, espera a la recarga sin cancelar el modo.
- El disparo manual y el modo ASTEROID siguen disponibles.

### Pendiente / siguiente paso

- Continuar con **T-405 — Sustituir monedas directas por mineral y fundición temporal**.

---

## S-031 — Extracción a bodega y agotamiento

- **Fecha:** 2026-11-08T01:15:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-403
- **Resultado:** COMPLETADO

### Resumen

- Cada asteroide precalcula su composición exacta por mineral.
- `MiningLaser` extrae en lotes y los pasa a `CargoHold` respetando masa y
  capacidad.
- Si el lote no cabe, el láser se detiene sin perder reserva.
- Las rocas agotadas desaparecen del mundo sin explosión ni monedas.
- La reserva, la carga y el mundo persisten; una roca agotada no reaparece.
- Se corrigió la distribución fraccionaria de la composición y se estabilizó
  la prueba de partículas de fuga.

### Archivos modificados

- `src/com/example/codegame/Asteroid.java`
- `src/com/example/codegame/CargoHold.java`
- `src/com/example/codegame/MiningLaser.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK** (dos ejecuciones).
- `tools/run-tests.sh --no-build 03_controls 04_autopilot`: **2/2 OK**.

### Evidencias

- `PASS el mineral entra en la bodega`
- `PASS la masa de la carga es correcta`
- `PASS la roca agotada desaparece`
- `PASS minar no da monedas`
- `PASS la bodega llena detiene la extraccion`
- `PASS la roca agotada se guarda`
- `PASS la roca agotada no reaparece`
- `03_controls` y `04_autopilot` OK.

### Decisiones tomadas

- La composición se precalcula con el método de mayor resto y se consume en
  orden de mineral, de forma determinista y reconstruible.
- La extracción es atómica por lote: no se pierde reserva si no cabe en bodega.
- La recompensa legacy en monedas de `Asteroid` se mantiene hasta T-405; minar
  no la usa.

### Pendiente / siguiente paso

- Continuar con **T-404 — Añadir Autopilot.MINE y selección de objetivo**.

---

## S-030 — Láser minero y capacitor

- **Fecha:** 2026-11-08T01:07:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-402
- **Resultado:** COMPLETADO

### Resumen

- `EnergyCapacitor`: 100 de energía, consumo 3/s minando y recarga 8/s tras
  1 s sin minar.
- `MiningLaser`:
  - potencia 6 + 1.5·nivel;
  - penetración 3 + 1·nivel;
  - resistencia por dureza;
  - rango 260 u;
  - exige objetivo, energía y la parte `MINING_LASER`.
- `MINING_LASER` es la novena parte de la nave.
- El láser agota la reserva de la roca y se detiene.
- No consume combustible.
- `GameWorld` integra el tick; T-403 moverá el mineral a la bodega.
- Corregido el texto de reparación de `LASER` en la tienda.

### Archivos modificados

- `src/com/example/codegame/EnergyCapacitor.java` (nuevo)
- `src/com/example/codegame/MiningLaser.java` (nuevo)
- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK** (dos ejecuciones).
- `tools/run-tests.sh --no-build 03_controls 09_wing_damage`: **2/2 OK**.

### Evidencias

- `PASS capacitor inicial lleno`
- `PASS minar consume 3 de energia`
- `PASS el capacitor se agota`
- `PASS recarga tras 1 s de espera`
- `PASS tasa C ~6 UM/s`
- `PASS tasa S ~4.8 UM/s`
- `PASS tasa M ~3 UM/s`
- `PASS tiempos C/S/M dentro del plan`
- `PASS el laser sin objetivo no mina`
- `PASS el laser extrae y consume energia`
- `PASS el laser no consume combustible`
- `PASS sin energia el laser no mina`
- `PASS el laser agota la roca y se detiene`
- `PASS sin laser operativo no se mina`
- `PASS GameWorld integra el laser`

### Decisiones tomadas

- La energía vive en `Ship`; el objetivo y el tick en `MiningLaser`.
- La extracción aún no llena la bodega; se conecta en T-403.
- La parte `MINING_LASER` se añade al final del array.

### Pendiente / siguiente paso

- Continuar con **T-403 — Integrar extracción, bodega y agotamiento**.

---

## S-029 — Tipos de asteroide y RNG determinista

- **Fecha:** 2026-11-08T01:01:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-401
- **Resultado:** COMPLETADO

### Resumen

- `AsteroidType` C/S/M con:
  - dureza 2/5/8;
  - reserva 12–18 / 28–42 / 60–90;
  - composición mineral del plan maestro.
- `Asteroid` guarda tipo, reserva y máximo; expone dureza y composición.
- La generación usa `Random` sembrado por chunk y por roca; dos mundos
  idénticos producen los mismos asteroides.
- `GameWorld` y `SaveCodec` persisten tipo y reserva; los guardados antiguos
  cargan con tipo C.
- El tipo no incluye recompensa en monedas; la recompensa legacy del asteroide
  se eliminará en T-405.

### Archivos modificados

- `src/com/example/codegame/AsteroidType.java` (nuevo)
- `src/com/example/codegame/Asteroid.java`
- `src/com/example/codegame/GameData.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls 04_autopilot`: **2/2 OK**.

### Evidencias

- `PASS existen tres tipos de asteroide`
- `PASS dureza C/S/M`
- `PASS reservas C/S/M`
- `PASS composiciones suman 1`
- `PASS C incluye hielo y M incluye titanio`
- `PASS el tipo de asteroide no lleva monedas`
- `PASS asteroide tiene tipo, dureza y composicion`
- `PASS la generacion de asteroides es determinista`
- `PASS el asteroide tipado se guarda/restaura`
- `PASS un asteroide antiguo carga con tipo y reserva`

### Decisiones tomadas

- La reserva es total; la composición se consulta por tipo.
- La semilla de cada roca deriva de posición, tamaño y tipo.
- El tipo no concede monedas; la extracción de mineral llega en T-402/T-403.

### Pendiente / siguiente paso

- Continuar con **T-402 — Implementar MiningLaser y EnergyCapacitor**.

---

## S-028 — Verificación y cierre de carga

- **Fecha:** 2026-11-08T00:56:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-306
- **Resultado:** COMPLETADO

### Resumen

- Verificados capacidad, masa, viaje de 800 u, persistencia y control.
- Añadida telemetría `cargoMass`, `cargoCapacity` y `cargoUnits`.
- Nuevo `cargo_test` y prueba `11_cargo`.
- La simulación de SelfTest pasa a inmortal para no cortarse por un choque
  aleatorio; la verificación de generación/monedas es determinista.
- Etapa **S3 (Carga y masa) cerrada**.

### Archivos modificados

- `src/com/example/codegame/CargoHold.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/MainActivity.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/11_cargo.sh` (nuevo)
- `docs/pruebas.md`
- `docs/MASTER_PLAN.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK** (dos ejecuciones seguidas).
- `tools/run-tests.sh --no-build 11_cargo`: **OK**.

### Evidencias

- `11_cargo`: bodega `100/100 UM`, HUD y persistencia al pausar.
- `BENCH viaje ida/vuelta 800u combustible=111.8 restante=48.2`.
- Mejora Bodega y guardado de carga verificados.
- `S3` marcada `COMPLETADO`; `S4` queda `LISTO`.

### Decisiones tomadas

- La simulación de SelfTest es inmortal porque mide generación y monedas, no
  supervivencia.
- `cargo_test` solo inyecta carga; no altera el juego normal.

### Pendiente / siguiente paso

- Empezar la Etapa 4 con **T-401 — Definir AsteroidType, composición y RNG determinista**.

---

## S-027 — Mejora Bodega y HUD de masa

- **Fecha:** 2026-11-08T00:52:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-305
- **Resultado:** COMPLETADO

### Resumen

- `BODEGA` es la sexta mejora: +25 UM de capacidad por nivel.
- `Ship.reset()` y `syncCargo()` aplican la capacidad del loadout.
- `Upgrades.cargoCapacity()` parte de 100 UM.
- El HUD muestra `CARGA usado/capacidad UM` bajo la barra de combustible;
  no se añadió otra barra sin jerarquía.
- La mejora se guarda/restaura en el loadout y los guardados v2 de 5 niveles
  migran con Bodega 0.

### Archivos modificados

- `src/com/example/codegame/ShipLoadout.java`
- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh 02_navigation 03_controls 10_survival`: **3/3 OK**.

### Evidencias

- `PASS bodega base 100`
- `PASS la mejora Bodega se compra`
- `PASS bodega sube 25 por nivel`
- `PASS la nave aplica la capacidad de bodega`
- `PASS la sesion usa la bodega mejorada`
- `PASS la bodega mejorada acepta carga`
- `PASS la mejora de bodega se guarda/restaura`
- `PASS un v2 con 5 niveles carga con Bodega 0`
- Captura `20_survival_hud`: `CARGA 0/100 UM`.

### Decisiones tomadas

- La capacidad se deriva del nivel de Bodega; no se guarda como campo aparte.
- Los niveles antiguos se completan con 0.
- El HUD usa texto compacto para no añadir otra barra.

### Pendiente / siguiente paso

- Continuar con **T-306 — Verificación de carga, masa y combustible**.

---

## S-026 — Rebalanceo de depósito y consumo

- **Fecha:** 2026-11-08T00:49:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-304
- **Resultado:** COMPLETADO

### Resumen

- Depósito base: **160** (antes 100).
- Consumo base: **10/s** (antes 12/s).
- Recarga de combustible: **1 crédito/unidad**, mínimo 5; ya no se duplica
  con cada compra.
- La etiqueta de la tienda muestra `DEPOSITO OK` si está lleno.
- Bench de ida y vuelta de 800 u con carga completa:
  **111.8 de combustible usado y 48.2 restante**.
- Un bidón de 40 sigue siendo útil (25 % del depósito base).
- Se actualizaron `MASTER_PLAN.md` y `docs/arquitectura.md`.

### Archivos modificados

- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/MASTER_PLAN.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh 03_controls 09_wing_damage`: **2/2 OK**.

### Evidencias

- `PASS deposito base 160`
- `PASS consumo base 10`
- `PASS el bidon sigue siendo util`
- `PASS repostar 160 cuesta 160`
- `PASS repostar 40 cuesta 40`
- `PASS repostar tiene coste minimo`
- `PASS ida y vuelta de 800 u con carga completa`
- `PASS la ida y vuelta deja margen`
- `BENCH viaje ida/vuelta 800u combustible=111.8 restante=48.2`

### Decisiones tomadas

- El coste de repostar pasa de exponencial a lineal por unidad.
- El depósito base sube a 160 para que la carga completa no sea un softlock.
- La masa del combustible no se incluye todavía.

### Pendiente / siguiente paso

- Continuar con **T-305 — Añadir la mejora Bodega y el HUD de masa**.

---

## S-025 — Masa en movimiento y consumo

- **Fecha:** 2026-11-08T00:42:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-303
- **Resultado:** COMPLETADO

### Resumen

- `MassModel` calcula la carga normalizada y cuatro factores:
  - aceleración: `1 − 0.25·carga`;
  - velocidad: `1 − 0.12·carga`;
  - giro: `1 − 0.10·carga`;
  - consumo: `1 + 0.35·carga`.
- `Ship.update()` aplica esos factores a empuje, límite de velocidad, giro y
  combustible.
- La carga por encima de capacidad se limita a 1 y no dispara los factores.
- La masa del combustible queda fuera de esta versión.
- La nave llena sigue siendo controlable y cubre 800 u con combustible restante.

### Archivos modificados

- `src/com/example/codegame/MassModel.java` (nuevo)
- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls`: **OK**.

### Evidencias

- `PASS factores de masa a carga completa`
- `PASS carga completa reduce la aceleracion ~25%`
- `PASS carga completa sube el consumo ~35%`
- `PASS carga completa reduce la velocidad ~12%`
- `PASS carga completa reduce el giro ~10%`
- `PASS la nave cargada sigue siendo controlable`
- `PASS viaje de 800 u con carga completa`

### Decisiones tomadas

- La penalización depende de la fracción de capacidad ocupada, no de una masa
  fija, para que la mejora de bodega de T-305 funcione.
- Los factores se calculan por frame desde la bodega; no se cachean.
- Los topes de carga evitan naves incontrolables.

### Pendiente / siguiente paso

- Continuar con **T-304 — Rebalancear depósito y consumo**.

---

## S-024 — Bodega persistente en Ship/GameSession

- **Fecha:** 2026-11-08T00:39:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-302
- **Resultado:** COMPLETADO

### Resumen

- `Ship` incorpora un `CargoHold` y lo expone con `cargo()`.
- `GameSession.cargo()` delega en la nave activa.
- `Ship.reset()` deja la bodega vacía y con capacidad base.
- `SaveCodec` v2 guarda las entradas de carga con mineral y cantidad, y las
  restaura sin aplicar la capacidad actual (para no perder inventario si
  cambia entre versiones).
- Los guardados antiguos sin campo `cargo` cargan con bodega vacía.
- Guardar al pausar conserva el inventario.

### Archivos modificados

- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/GameSession.java`
- `src/com/example/codegame/CargoHold.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls`: **OK**.

### Evidencias

- `PASS la nave expone su bodega`
- `PASS la bodega de la nave acepta carga`
- `PASS la bodega se guarda`
- `PASS la bodega se restaura`
- `PASS una partida sin bodega carga vacia`
- Masa restaurada: 10 Hielo + 5 Oro = 20.5 UM.

### Decisiones tomadas

- La bodega pertenece a `Ship`; `GameSession` solo la expone.
- `CargoHold.restore` permite conservar carga aunque la capacidad guardada
  sea distinta a la actual.
- La capacidad por defecto sigue siendo 100 UM hasta la mejora de T-305.

### Pendiente / siguiente paso

- Continuar con **T-303 — Aplicar la masa al movimiento y al consumo**.

---

## S-023 — Commodities y bodega por masa

- **Fecha:** 2026-11-08T00:36:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-301
- **Resultado:** COMPLETADO

### Resumen

- `Commodity` define Hielo, Silicato, Hierro, Cobre, Titanio y Oro con masa y
  precio base del plan maestro.
- `Cargo` es una entrada inmutable con masa y valor.
- `CargoHold` usa capacidad en UM, no en unidades; suma la masa real, rechaza
  el exceso y permite añadir/quitar/consultar.
- `GameData` crea la bodega inicial de 100 UM.
- No se conectó minería ni la nave; la carga se prueba inyectada.

### Archivos modificados

- `src/com/example/codegame/Commodity.java` (nuevo)
- `src/com/example/codegame/Cargo.java` (nuevo)
- `src/com/example/codegame/CargoHold.java` (nuevo)
- `src/com/example/codegame/GameData.java` (nuevo)
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.

### Evidencias

- `PASS existen seis minerales`
- `PASS bodega inicial vacia`
- `PASS anadir hielo suma masa`
- `PASS anadir hierro suma masa`
- `PASS rechaza el exceso exacto`
- `PASS acepta lo que cabe y rechaza el resto`
- `PASS quitar carga resta masa`
- `PASS no se puede quitar de mas`
- `PASS valor base de la bodega`

### Decisiones tomadas

- La capacidad se mide en masa (UM); 100 UM iniciales.
- `add` acepta lo que quepa; `addExact` exige que quepa todo.
- `Cargo` es inmutable y `CargoHold` es el contenedor mutable.

### Pendiente / siguiente paso

- Continuar con **T-302 — Integrar la bodega en Ship/GameSession y el guardado**.

---

## S-022 — Recuperación de desarrollo tras muerte

- **Fecha:** 2026-11-08T00:33:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-210
- **Resultado:** COMPLETADO

### Resumen

- Nuevo extra `recovery_test`, desactivado por defecto.
- Con el flag, se guarda un punto seguro al empezar y cada 10 s.
- Tras morir, el botón cambia a `RECUPERAR (DEBUG)` y restaura el punto
  seguro: salud, O2, casco, nave y mundo.
- La recuperación limpia la nave legado y guarda la partida restaurada.
- Sin el flag, el juego normal sigue con muerte permanente y `NUEVO PERSONAJE`.
- Etapa **S2 (Supervivencia) cerrada**.

### Archivos modificados

- `src/com/example/codegame/MainActivity.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SaveService.java`
- `tools/tests/05_death.sh`
- `docs/pruebas.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 05_death`: **OK**.
- `tools/run-tests.sh --no-build 08_visual_death`: **OK**.

### Evidencias

- `05_death`:
  - casco → `OVER`;
  - piloto 0 HP → `OVER`;
  - `NUEVO PERSONAJE` → `PLAYING`;
  - `RECUPERAR (DEBUG)` → `PLAYING` con salud 100, estado `OK`, casco lleno
    y sin nave legado.
- `08_visual_death` valida las capturas de la secuencia.

### Decisiones tomadas

- El modo de recuperación solo existe con el extra de test; no se persiste.
- El punto seguro se guarda cada 10 s y al iniciar/reanudar.
- La recuperación restaura el punto seguro completo, no una resurrección
  parcial.

### Pendiente / siguiente paso

- Empezar la Etapa 3 con **T-301 — Definir Commodity y Cargo**.

---

## S-021 — Nuevo personaje y nave legado recuperable

- **Fecha:** 2026-11-08T00:29:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-209
- **Resultado:** COMPLETADO

### Resumen

- El nuevo personaje conserva el mundo persistente: asteroides, bidones,
  chunks y restos.
- La nave legado se dibuja en el punto de muerte.
- A ≤100 u aparece el botón `RECUPERAR NAVE`.
- Al recuperarla se obtiene un desguace en créditos (10–90 según las partes) y
  el resto desaparece.
- El estado recuperado se guarda; al cargar ya no reaparece la nave legado.
- Se añadió `16_universe`, que cubre muerte de piloto, nuevo personaje, mundo
  persistente y recuperación.

### Archivos modificados

- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/testlib.sh`
- `tools/tests/16_universe.sh`
- `docs/pruebas.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 05_death`: **OK**.
- `tools/run-tests.sh --no-build 16_universe`: **OK**.

### Evidencias

- `PASS la nave legado no se recupera de lejos`
- `PASS la nave legado se recupera al acercarse`
- `16_universe: nuevo personaje, mundo persistente y nave legado recuperada`
- El botón solo aparece dentro del radio; fuera se ignora.

### Decisiones tomadas

- La recuperación es manual (botón) y no automática.
- El desguace da créditos como puente hasta la carga/minería de S3/S4.
- El estado `legacyWreck=false` se guarda para no duplicar la nave.

### Pendiente / siguiente paso

- Continuar con **T-210 — Modo de desarrollo de recuperación tras muerte**.

---

## S-020 — Muerte permanente y mundo persistente

- **Fecha:** 2026-11-08T00:24:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-208
- **Resultado:** COMPLETADO

### Resumen

- La partida termina por casco 0 **o** por piloto a 0 HP/asfixiado.
- Al morir se guarda un snapshot `legacy.json` con el mundo y la nave.
- La nave del personaje anterior queda como resto en las coordenadas de muerte.
- `NUEVO PERSONAJE` carga el mismo mundo (asteroides, bidones, chunks y
  restos) y crea una nave nueva en el origen.
- El guardado v2 incluye la posición y partes de la nave legado, así que el
  resto sobrevive a reinicios.
- Se corrigió el bug reportado: antes, a 0 HP el piloto quedaba
  `INCAPACITATED` y la partida seguía indefinidamente.
- No hay reaparición en la nave nodriza; el modo de recuperación de desarrollo
  queda para T-210.

### Archivos modificados

- `src/com/example/codegame/Pilot.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/SaveService.java`
- `src/com/example/codegame/MainActivity.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/05_death.sh`
- `docs/DECISIONS.md`
- `docs/arquitectura.md`
- `docs/pruebas.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 05_death`: **OK**.
- `tools/run-tests.sh --no-build 08_visual_death 03_controls`: **2/2 OK**.

### Evidencias

- `PASS la muerte guarda el mundo legado`
- `PASS el mundo legado carga y conserva asteroides`
- `PASS la nave legado queda en el lugar de la muerte`
- `PASS el nuevo personaje resetea la nave y conserva el legado`
- `PASS save v2 conserva la nave legado`
- `PASS cargar conserva la nave legado`
- `05_death`: casco → OVER; piloto 0 HP → OVER sin romper casco;
  `NUEVO PERSONAJE` → PLAYING; `legacyWreck=true`.

### Decisiones tomadas

- La muerte por piloto no destruye el casco: la nave queda intacta como resto.
- Solo se conserva un mundo legado a la vez; T-209 podrá ampliar la recuperación.
- El modo de recuperación de desarrollo se separa en T-210.

### Pendiente / siguiente paso

- Continuar con **T-209 — Crear flujo de nuevo personaje y nave legado**.

---

## S-019 — Panel: refresco JSON sin recarga

- **Fecha:** 2026-11-08T00:17:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** mantenimiento del panel
- **Resultado:** COMPLETADO

### Resumen

- `Actualizar datos` y la auto-recarga ya no hacen `window.location.reload()`.
- Por HTTP, el panel hace `fetch` de `IMPLEMENTATION_TRACKER.json` con
  `cache: no-store`, reemplaza los datos y re-renderiza.
- Se conservan filtros, estado de tareas abiertas y posición de scroll.
- Por `file://` se sigue usando el bloque JSON embebido; no se intenta un
  `fetch` que el navegador bloquearía.
- El indicador `Datos:` muestra `JSON en vivo` o `embebidos (file://)`.
- Se actualizó la regla de `AGENTS.md` sobre `fetch`.

### Archivos modificados

- `docs/PROJECT_DASHBOARD.html`
- `AGENTS.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `node --check` del script inline: **OK**.
- HTTP con servidor local + Firefox headless: `Datos: JSON en vivo`.
- `file://` con Firefox headless: `Datos: embebidos (file://)`.
- `python3 tools/sync-dashboard.py --check`: **OK**.

### Evidencias

- Captura HTTP: `/tmp/opencode/dashboard-http.png` con `Datos: JSON en vivo`.
- Captura file://: `/tmp/opencode/dashboard-file.png` con
  `Datos: embebidos (file://)`.
- El panel siguió mostrando 16 % y la tarea T-208 sin recargar.

### Decisiones tomadas

- El `fetch` solo se usa cuando el panel se sirve por HTTP.
- Si el `fetch` falla, se mantienen los datos actuales y se avisa; no se
  recarga la página.
- El bloque embebido sigue siendo la fuente para `file://`.

### Pendiente / siguiente paso

- Continuar con **T-208 — Implementar muerte permanente y mundo persistente**.

---

## S-018 — Integración y balance de supervivencia

- **Fecha:** 2026-11-08T00:10:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-207
- **Resultado:** COMPLETADO

### Resumen

- Expedición de 10 min sin daño: **75 % de O2**, salud al 100 %.
- Casco al 50 %: fuga de 3 O2/min y aviso visible.
- O2 a 0 con salud completa: **105 s de margen** (5 s de gracia + 100 s de
  daño a 1 HP/s), por encima de los 60 s exigidos.
- Reparar `LIFE_SUPPORT` devuelve el filtro al 100 % si el filtro está sano.
- La telemetría incluye `pilotO2Seconds` para estimar la reserva.
- `docs/pruebas.md` documenta `10_survival`, `survival_test` y el balance.

La etapa S2 sigue abierta por T-208/T-209/T-210; T-207 solo cierra el balance.

### Archivos modificados

- `src/com/example/codegame/LifeSupport.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/pruebas.md`
- `docs/arquitectura.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 10_survival`: **OK**.

### Evidencias

- `BENCH expedicion 10min O2=75.0 salud=100.0`
- `PASS expedicion de 10 min deja mas del 50% de O2`
- `PASS casco al 50% produce aviso de fuga`
- `PASS O2 0 da al menos 60 s de margen`
- `PASS reparar soporte vital recupera el filtro`
- Telemetría real: `o2Alert=1`, `pilotLeak=4.5`, `pilotO2Seconds=199`.

### Decisiones tomadas

- No se ajustaron las tasas: 4 O2/min y 1.5 O2/min de filtro cumplen el gate.
- La estimación de reserva se expone en telemetría; el HUD mantiene el diseño
  compacto de T-205.
- La etapa no se marca completada hasta resolver T-208/T-209.

### Pendiente / siguiente paso

- Continuar con **T-208 — Implementar muerte permanente y mundo persistente**.

---

## S-017 — Recarga de O2 y reparación de soporte

- **Fecha:** 2026-11-08T00:06:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-206
- **Resultado:** COMPLETADO

### Resumen

- La pestaña `REPARAR` incluye una tarjeta `OXIGENO`.
- Coste: 0,5 créditos por unidad faltante, mínimo 5; si está lleno muestra
  `O2 OK`.
- Al pagar se llena la reserva, se repone la gracia y `HYPOXIC` vuelve a `OK`.
- `LIFE_SUPPORT` aparece como `SOPORTE` y se repara con la misma fórmula que
  las demás partes.
- La recarga se guarda en v2 y sobrevive a la carga.
- El servicio es temporal; se moverá a la estación en la Etapa 5.

### Archivos modificados

- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/Pilot.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 02_navigation 03_controls 09_wing_damage`:
  **3/3 OK**.

### Evidencias

- `PASS O2 lleno no cuesta`
- `PASS O2 a 40 unidades cuesta 20`
- `PASS O2 tiene coste minimo`
- `PASS la recarga de O2 descuenta creditos`
- `PASS reabastecer O2 llena y reactiva`
- `PASS LIFE_SUPPORT se repara como las demas partes`
- `PASS la recarga de O2 no rompe el guardado`
- `build/screens/12_shop_repair.png`: `SOPORTE 100 %` y `OXIGENO / O2 OK`.

### Decisiones tomadas

- El O2 usa precio lineal de 0,5/unidad con mínimo 5, no el contador
  exponencial del combustible.
- La recarga no revive a un piloto `INCAPACITATED`; solo recupera O2 y el
  estado `HYPOXIC`.
- La tarjeta se elimina/traslada cuando exista la estación física.

### Pendiente / siguiente paso

- Continuar con **T-207 — Integración y balance de supervivencia**.

---

## S-016 — HUD y alertas de supervivencia

- **Fecha:** 2026-11-08T00:03:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-205
- **Resultado:** COMPLETADO

### Resumen

- Se añadió una fila compacta de supervivencia justo debajo del panel superior:
  barra `O2` y barra `HP`, separadas del casco.
- Iconos ASCII: anillo de O2 y cruz de salud; texto con porcentaje.
- Aviso de O2 al 25 % (`O2 BAJO`) y al 10 % (`O2 CRITICO`).
- La fuga se muestra como `FUGA` en la barra y `FUGA O2`/`BRECHA O2` debajo.
- La barra de O2 y su borde cambian de color con la alerta y la brecha.
- Se añadió `survival_test` y la prueba `10_survival`.
- La telemetría incluye `pilotLeak`, `pilotBreach` y `o2Alert`.

### Archivos modificados

- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/Visuals.java`
- `src/com/example/codegame/Pilot.java`
- `src/com/example/codegame/LifeSupport.java`
- `src/com/example/codegame/MainActivity.java`
- `src/com/example/codegame/SelfTest.java`
- `tools/tests/10_survival.sh` (nuevo)
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 10_survival`: **OK**, con captura validada.

### Evidencias

- `build/screens/20_survival_hud.png`: O2 24 %, HP 70 %, CASCO 50 %,
  `O2 BAJO 24 %` y `BRECHA O2`.
- `PASS O2 normal sin alerta`
- `PASS O2 al 25% activa aviso`
- `PASS O2 al 10% activa alerta critica`
- `PASS O2 a 0 sigue en alerta critica`
- Telemetría de `10_survival`: `o2Alert=1`, `pilotLeak≈4.5`,
  `pilotBreach=true`.

### Decisiones tomadas

- Las barras de supervivencia van debajo del panel superior para no saturarlo.
- Los avisos usan texto ASCII y color; no dependen solo del color.
- `10_survival` es una prueba de telemetría + captura; las alertas de 25/10 %
  se comprueban también en lógica.
- El modo `survival_test` arranca con O2 24 %, salud 70 %, casco 50 % y brecha.

### Pendiente / siguiente paso

- Continuar con **T-206 — Reabastecer O2 y reparar soporte vital**.

---

## S-015 — Asfixia, salud y estados del piloto

- **Fecha:** 2026-11-07T23:59:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-204
- **Resultado:** COMPLETADO

### Resumen

- Con O2 a 0 hay 5 s de gracia sin daño.
- Pasada la gracia, la salud baja 1 HP/s.
- Con O2 disponible la salud no baja y la gracia se repone.
- Salud 0 deja al piloto `INCAPACITATED`.
- La salud del piloto es independiente del casco.
- `oxygenGrace` se guarda en la línea legacy y como `grace` en v2.
- La telemetría muestra `pilotStatus` y `pilotGrace`.
- La muerte permanente y el mundo persistente quedan para T-208.

### Archivos modificados

- `src/com/example/codegame/Pilot.java`
- `src/com/example/codegame/LifeSupport.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls`: **OK**.

### Evidencias

- `PASS 5 s de gracia sin perder salud`
- `PASS tras la gracia la salud baja 1 HP/s`
- `PASS salud 0 deja al piloto incapacitado`
- `PASS con O2 la salud no baja`
- `PASS la asfixia no toca el casco`
- `PASS LifeSupport aplica la asfixia`
- `PASS guardado antiguo de piloto usa 5 s de gracia`
- `PASS save v2 conserva asfixia y gracia`
- Telemetría real: `pilotStatus="OK"`, `pilotGrace=5.0`.

### Decisiones tomadas

- La gracia se aplica de forma prorrateada dentro del frame.
- La incapacitación no dispara todavía la muerte permanente; T-208 la conecta.
- La salud y el casco no se mezclan.

### Pendiente / siguiente paso

- Continuar con **T-205 — Añadir HUD y alertas de O2, salud y fuga**.

---

## S-014 — Fugas por casco y brecha

- **Fecha:** 2026-11-07T23:56:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-203
- **Resultado:** COMPLETADO

### Resumen

- La fuga de O2 depende de la integridad del casco: 0/min al 100 %, 3/min al
  50 %, 4.8/min al 20 % y 6/min al 0 %.
- Una brecha crítica añade un 50 % de fuga y puede repararse.
- Un impacto fuerte (asteroide grande) con el casco ya castigado, o un casco por
  debajo del 20 %, abre brecha. Un impacto fuerte con casco intacto no la abre.
- La fuga consume O2 y se incluye en el tiempo estimado.
- `GameWorld` emite partículas de gas desde el casco dañado.
- `MyGame` muestra un aviso mínimo `FUGA O2` / `BRECHA O2` en el HUD.

### Archivos modificados

- `src/com/example/codegame/LifeSupport.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/GameSession.java`
- `src/com/example/codegame/Effects.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls`: **OK**.

### Evidencias

- `PASS sin fuga con casco intacto`
- `PASS fuga al 50% de casco ~3/min`
- `PASS fuga al 20% de casco ~4.8/min`
- `PASS brecha aumenta la fuga un 50%`
- `PASS reparar brecha devuelve la fuga base`
- `PASS impacto fuerte con casco intacto no abre brecha`
- `PASS impacto fuerte con casco dañado abre brecha`
- `PASS la fuga reduce el O2 a 60 s`
- `PASS tiempo estimado incluye la fuga`
- `PASS la fuga emite particulas en el mundo`

### Decisiones tomadas

- La fuga usa la fórmula `6·(1 − casco/cascoMax)·(1 + 0.5·brecha)` por minuto.
- La brecha se abre por impacto fuerte con casco ≤70 % o por casco ≤20 %.
- El aviso de HUD es mínimo; T-205 ampliará la información de supervivencia.

### Pendiente / siguiente paso

- Continuar con **T-204 — Implementar asfixia, salud y estados del piloto**.

---

## S-013 — Consumo de O2 y filtro

- **Fecha:** 2026-11-07T23:53:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-202
- **Resultado:** COMPLETADO

### Resumen

- El piloto consume 4 unidades de O2 por minuto.
- El filtro efectivo regenera 1.5/min con soporte intacto y se modula por la
  integridad de `LIFE_SUPPORT`: 100 % → 100 %, 50 % → 60 %, 0 % → 20 %.
- La pérdida neta con soporte intacto es 2.5/min; 100 de O2 duran ~40 min.
- `GameSession.update()` aplica el consumo cada frame.
- `filterEffectiveness()`, `netOxygenPerMinute()` y `estimatedSeconds()` quedan
  disponibles para HUD y decisiones.
- No se añadió energía de soporte vital, asfixia ni HUD.

### Archivos modificados

- `src/com/example/codegame/LifeSupport.java`
- `src/com/example/codegame/GameSession.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls`: **OK**.

### Evidencias

- `PASS la sesion consume O2`
- `PASS consumo de O2 a 60 s ~2.5`
- `PASS filtro intacto al 100%`
- `PASS filtro al 50% de soporte`
- `PASS filtro al 0% de soporte cae al 20%`
- `PASS tiempo estimado de O2 ~2400 s`
- Telemetría real: `pilotOxygen` bajó de 100.0 a 99.5 durante la partida.

### Decisiones tomadas

- El filtro se calcula como `(0.20 + 0.80·LIFE/100) · filter/100`.
- El nivel de mejora de soporte se deja a 0; la fórmula admite un bonus futuro.
- Las fugas y la asfixia quedan para T-203/T-204.

### Pendiente / siguiente paso

- Continuar con **T-203 — Implementar fugas por daño de casco y brecha**.

---

## S-012 — LIFE_SUPPORT y persistencia de supervivencia

- **Fecha:** 2026-11-07T23:50:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-201
- **Resultado:** COMPLETADO

### Resumen

Se añadió la base de la supervivencia sin activar consumo:

- `LIFE_SUPPORT` es la última parte de `Ship` (índice 7), reparable como las demás.
- `LifeSupport` guarda `filter`, `leak` y `breach`.
- `GameSession` agrupa el soporte vital y lo reinicia al empezar.
- El guardado v2 incluye `lifeSupport`; el formato legacy añade una línea `life`.
- Los guardados anteriores de 7 partes cargan con `LIFE_SUPPORT` a 100.
- No hay consumo de O2, fugas automáticas ni HUD.

### Archivos modificados

- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/LifeSupport.java` (nuevo)
- `src/com/example/codegame/GameSession.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls`: **OK** como smoke test del cambio de partes.

### Evidencias

- `PASS soporte vital es la ultima parte`
- `PASS soporte vital inicial intacto`
- `PASS soporte vital inicia lleno`
- `PASS save v2 conserva LIFE_SUPPORT`
- `PASS save v2 conserva soporte vital`
- `PASS guardado de 7 partes rellena LIFE_SUPPORT`

### Decisiones tomadas

- `LIFE_SUPPORT` se añade al final para no romper índices antiguos.
- El soporte vital vive en `GameSession`, no en `Ship`; la parte solo mide integridad.
- La compatibilidad de 7 partes se resuelve rellenando la parte nueva con 100.

### Pendiente / siguiente paso

- Continuar con **T-202 — Implementar consumo de O2 y filtro**.

---

## S-011 — Cierre de la Etapa 1

- **Fecha:** 2026-11-07T23:47:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-106
- **Resultado:** COMPLETADO

### Resumen

Se cerró la Etapa 1 verificando que la separación piloto/nave y el guardado v2
no cambiaron el comportamiento visible:

- `01_logic` en verde con todas las comprobaciones nuevas.
- `03_controls`, `04_autopilot` y `05_death` sin regresiones.
- Migración `run2` → v2 probada en SelfTest.
- Reinicio real: la app vuelve en `PAUSED` con combustible y posición restaurados.
- Revisión de extensibilidad T-107 documentada.
- `S1` marcada `COMPLETADO`; `S2` queda `LISTO`.

### Archivos modificados

- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`
- `docs/MASTER_PLAN.md`
- `docs/PROJECT_DASHBOARD.html` (sincronizado)

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 03_controls 04_autopilot 05_death`: **3/3 OK**.

### Evidencias

- `03_controls`: PLAYING → PAUSED → SHOP → PLAYING.
- `04_autopilot`: ASTEROID → STOP → NONE.
- `05_death`: DYING → OVER → REINTENTAR.
- Migración y backup cubiertos por las 17 comprobaciones de guardado del SelfTest.

### Decisiones tomadas

- La Etapa 1 cumple su gate: guardado v1 migra a v2 y el juego se comporta igual.
- La siguiente etapa es S2 (Supervivencia); no se adelantan sus mecánicas aquí.

### Pendiente / siguiente paso

- Empezar **T-201 — Añadir LIFE_SUPPORT y campos de supervivencia**.

---

## S-010 — Extensibilidad de identidad y online futuro

- **Fecha:** 2026-11-07T23:46:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-107
- **Resultado:** COMPLETADO

### Resumen

Se preparó la arquitectura sin implementar red ni combate:

- `PlayerProfile` separa `playerId` (cuenta/jugador) de `id` (personaje).
- El guardado v2 persiste `playerId` y `characterId`, manteniendo `id` por compatibilidad.
- `SaveService` acepta slots (`slot1`, `slot2`…) y sanea nombres inválidos.
- `docs/arquitectura.md` y `docs/MASTER_PLAN.md` documentan los límites de la simulación local y los puntos de extensión futuros.

### Archivos modificados

- `src/com/example/codegame/PlayerProfile.java`
- `src/com/example/codegame/SaveCodec.java`
- `src/com/example/codegame/SaveService.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/arquitectura.md`
- `docs/MASTER_PLAN.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- Revisión documental de límites y puntos de extensión.

### Evidencias

- `PASS perfil tiene identidad por defecto`
- `PASS save v2 escribe identidad de jugador y personaje`
- `PASS save v2 restaura identidad de jugador y personaje`
- `PASS slots de guardado separados`
- `PASS slot invalido cae al por defecto`

### Decisiones tomadas

- No hay red, multijugador, PvP ni facciones; solo se evita cerrar la puerta.
- El guardado local no se considera seguro ni autoritativo.
- Los slots y la identidad doble preparan varios personajes sin rehacer el formato.

### Pendiente / siguiente paso

- Continuar con **T-106 — Verificación de paridad de la Etapa 1**.

---

## S-009 — Integración de SaveService en MyGame

- **Fecha:** 2026-11-07T23:43:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-105
- **Resultado:** COMPLETADO

### Resumen

Se conectó el guardado v2 con el ciclo real del juego:

- `MyGame.create()` crea `SaveService` con `FileStorage` sobre `Gdx.files.local("save")` y el `Upgrades` de la sesión.
- `loadRun()` usa `SaveService.load()`: v2, backup o migración `run2`. Si carga, entra en `PAUSED`.
- `saveRun()` escribe v2 en pausa, al abrir la tienda y en `dispose()`.
- `clearRun()` borra v2, backup y la clave legacy, y desactiva la sesión.
- `SaveCodec` marca la sesión como activa al aplicar un v2 válido.
- No se tocaron la navegación, el HUD, la secuencia de muerte ni la economía.

### Archivos modificados

- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/GameSession.java`
- `src/com/example/codegame/SaveCodec.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- `tools/run-tests.sh --no-build 02_navigation 03_controls 04_autopilot 05_death`: **4/4 OK**.

### Evidencias

- Reinicio real: tras `am force-stop`, relanzar la app entra directamente en `PAUSED` con `fuel=82.2` y posición `x=0.0, y=454.7`, restaurados desde v2.
- `03_controls` sigue pasando por menú → partida → pausa → tienda → partida.
- `05_death` sigue pasando por `DYING → OVER → REINTENTAR`, y al morir se limpia el guardado.

### Decisiones tomadas

- `MyGame` es el propietario de `SaveService`; `GameSession` conserva el formato legacy solo para migración y pruebas.
- `SaveService.load()` decide entre v2, backup y `run2`; el juego no duplica esa lógica.
- `dispose()` guarda antes de liberar recursos, con guarda por si `create()` no llegó a completarse.

### Pendiente / siguiente paso

- Continuar con **T-107 — Revisar arquitectura para identidad, facciones y online futuro**.
- Después, **T-106** cierra la Etapa 1.

---

## S-008 — SaveService v2 y migración

- **Fecha:** 2026-11-07T23:41:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-104
- **Resultado:** COMPLETADO

### Resumen

Se sustituyó el guardado monolítico por una capa versionada:

- `SaveCodec` convierte `GameSession` a JSON v2 con `version`, `profile`, `pilot`, `ship` y `sector`.
- `SaveService` abstrae el almacenamiento (`FileStorage` en disco, `MemoryStorage` en tests).
- `FileStorage` escribe a un archivo temporal y hace rename atómico; conserva `slot1.json.bak`.
- Ante JSON inválido, `load()` prueba el backup. Si no hay v2 válido, migra `run2` y limpia la clave antigua.
- `GameSession.restoreLegacy()` centraliza la lectura del formato anterior.
- `PlayerProfile` puede restaurar identidad; `Upgrades` permite fijar monedas y recargas.
- `GameWorld` expone sus chunks generados para que el códec restaure el sector sin regenerarlo.

`MyGame` todavía no usa `SaveService`; esa integración es T-105.

### Archivos modificados

- `src/com/example/codegame/SaveCodec.java` (nuevo)
- `src/com/example/codegame/SaveService.java` (nuevo)
- `src/com/example/codegame/GameSession.java`
- `src/com/example/codegame/GameWorld.java`
- `src/com/example/codegame/PlayerProfile.java`
- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- Guardado v2 completo, carga, backup, corrupción y migración `run2` → v2.

### Evidencias

- `build/test-logs/selftest.txt`:
  - `PASS save v2 incluye version, perfil, piloto, nave y sector`
  - `PASS save v2 conserva el perfil / piloto / nave / sector`
  - `PASS save v2 escribe backup`
  - `PASS save v2 cae al backup`
  - `PASS el texto corrupto no borra el backup`
  - `PASS migracion run2 a v2`
  - `PASS migracion conserva casco y combustible`
  - `PASS migracion conserva partes y monedas`
  - `PASS migracion conserva asteroides`
  - `PASS migracion crea el archivo v2`
  - `PASS migracion limpia run2`

### Decisiones tomadas

- El guardado v2 es JSON local con backup y rename atómico (D-008).
- El códec valida todo el texto antes de aplicar la partida; un guardado corrupto no deja media sesión cargada.
- `run2` solo se usa como fuente de migración y se elimina tras crear el v2.
- La integración con el ciclo de `MyGame` queda para T-105.

### Pendiente / siguiente paso

- Continuar con **T-105 — Integrar GameSession y guardado en MyGame/GameWorld**.

---

## S-007 — ShipLoadout modular

- **Fecha:** 2026-11-07T23:37:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-103
- **Resultado:** COMPLETADO

### Resumen

Se extrajo el estado de mejoras de la nave a un loadout modular:

- `ShipLoadout` es la fuente única de los niveles `MOTOR`, `DEPOSITO`, `CANON`, `BLINDAJE` y `GASOLINA`.
- Incluye `MODULE_SLOTS` ranuras genéricas, con instalación, retirada, búsqueda y persistencia.
- `AUTOPILOT_MODULE` queda definido como capacidad instalable; no se exige todavía para volar, así que el comportamiento visible no cambia.
- `Upgrades` actúa como fachada: `level()` y `buy()` delegación en el loadout, sin guardar una segunda copia.
- Las claves de persistencia (`lvl0`…`lvl4`) y las fórmulas de movimiento/combate se mantienen intactas.

### Archivos modificados

- `src/com/example/codegame/ShipLoadout.java` (nuevo)
- `src/com/example/codegame/Upgrades.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- Comprobaciones nuevas: nivel comprado visible en ambas fachadas, cinco sistemas, módulo de piloto automático, ranuras genéricas, límites de ranura y relectura desde el almacén.

### Evidencias

- `build/test-logs/selftest.txt`:
  - `PASS loadout refleja el nivel comprado`
  - `PASS loadout conserva los cinco sistemas`
  - `PASS piloto automatico no es innato`
  - `PASS loadout instala el modulo de piloto automatico`
  - `PASS loadout relee niveles del almacen`
  - `PASS loadout relee modulos del almacen`
- `TODO OK` al final del SelfTest.

### Decisiones tomadas

- `ShipLoadout` conserva los niveles en memoria y escribe a través de su `Store`; `Upgrades` no duplica niveles.
- Las fórmulas de movimiento y combate siguen en `Upgrades` para garantizar paridad en esta tarea.
- El módulo de piloto automático se prepara, pero no se impone todavía.
- El catálogo de naves/piezas no se implementa en esta tarea.

### Pendiente / siguiente paso

- Continuar con **T-104 — Implementar SaveService v2 y migración v1**.

---

## S-006 — Pilot y persistencia de salud/O2

- **Fecha:** 2026-11-07T23:31:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-102
- **Resultado:** COMPLETADO

### Resumen

Se separó al piloto de la nave:

- `Pilot` tiene `health`, `maxHealth`, `oxygen`, `o2Max`, `status` (`OK`/`HYPOXIC`/`INCAPACITATED`/`DEAD`) y `aboard`.
- Los valores iniciales son 100/100 y el piloto permanece a bordo.
- `GameSession` agrupa el piloto, lo reinicia al empezar y lo guarda/carga junto al sector.
- El guardado añade una línea `pilot <health> <maxHealth> <oxygen> <o2Max> <status>`. Los guardados anteriores sin esa línea cargan con 100/100.
- `MyGame` expone los valores en la telemetría de depuración, sin HUD ni consumo.
- `Ship` documenta que la integridad del casco no es la salud del piloto.

No se implementó consumo de O2, asfixia, fugas ni HUD: eso es Etapa 2.

### Archivos modificados

- `src/com/example/codegame/Pilot.java` (nuevo)
- `src/com/example/codegame/GameSession.java`
- `src/com/example/codegame/Ship.java`
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**.
- Comprobaciones nuevas del piloto: 100/100 inicial, independencia del casco, guardado/carga y compatibilidad con guardados antiguos.
- `tools/run-tests.sh --no-build 03_controls`: **OK** como smoke test tras cambiar la telemetría.

### Evidencias

- `build/test-logs/selftest.txt`:
  - `PASS piloto inicia con salud y O2 al 100`
  - `PASS salud del piloto no modifica el casco`
  - `PASS sesion guarda el piloto`
  - `PASS sesion carga el piloto`
  - `PASS guardado antiguo carga piloto 100/100`
- Telemetría real del dispositivo:
  `pilotHealth=100.0, pilotMaxHealth=100.0, pilotOxygen=100.0, pilotO2Max=100.0, pilotStatus=OK`.

### Decisiones tomadas

- El piloto vive en `GameSession`, no en `Ship` ni en `GameWorld`.
- El estado del piloto se serializa como una línea independiente del mundo; el formato `run2` sigue siendo legible.
- La telemetría puede mostrar salud/O2, pero el HUD y el consumo quedan para la Etapa 2.
- Los estados del piloto se definen ahora, pero solo `OK` se usa en esta tarea.

### Pendiente / siguiente paso

- Continuar con **T-103 — Separar ShipState y ShipLoadout modular**.
- No conectar todavía O2, fugas ni asfixia.

---

## S-005 — PlayerProfile y GameSession

- **Fecha:** 2026-11-07T23:27:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-101
- **Resultado:** COMPLETADO

### Resumen

Se creó la primera composición de la Etapa 1 sin cambiar el comportamiento visible:

- `PlayerProfile` agrupa la identidad del jugador y la referencia al progreso existente (`Upgrades`). No se movió la economía ni se añadieron monedas nuevas.
- `GameSession` agrupa el perfil, el sector activo (`GameWorld`, que contiene la nave) y los servicios de guardado heredados. Conserva temporalmente la clave `run2` y delega en `GameWorld.serialize()`/`restore()`.
- `MyGame` crea una instancia de `GameSession` en `create()`, usa su sector como mundo activo, arranca/carga/guarda a través de ella y actualiza el sector mediante `session.update()`.
- `SelfTest` añade comprobaciones de identidad del perfil, composición de la sesión y guardado/carga/limpieza.

La clase `Pilot` y el loadout modular no se adelantan: llegan en T-102 y T-103.

### Archivos modificados

- `src/com/example/codegame/PlayerProfile.java` (nuevo)
- `src/com/example/codegame/GameSession.java` (nuevo)
- `src/com/example/codegame/MyGame.java`
- `src/com/example/codegame/SelfTest.java`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `tools/run-tests.sh 01_logic`: **TODO OK**, incluidas las comprobaciones nuevas.
- `tools/run-tests.sh --no-build 03_controls`: **OK**.
- `tools/run-tests.sh --no-build 04_autopilot`: **OK**.

Nota: el primer intento de `03_controls` falló con «no empezó la partida» por un toque perdido durante la carga; al repetirlo pasó. No hay traza de excepción ni cambio de layout.

### Evidencias

- `build/test-logs/selftest.txt` termina en `TODO OK`.
- `03_controls`: PLAYING (inmortal, consume combustible) → PAUSED → SHOP → PLAYING.
- `04_autopilot`: ASTEROID → STOP → NONE, velocidad final < 12 u/s y acelerador 0.
- `GameSession` conserva la partida en `run2`; T-104 la sustituirá por `SaveService` v2.

### Decisiones tomadas

- `PlayerProfile` contiene la identidad y una referencia a `Upgrades`; no se duplica el estado ni se crea economía nueva.
- `GameSession` asume temporalmente el arranque, la actualización y el guardado heredado; la migración a `SaveService` v2 queda para T-104.
- `MyGame` mantiene alias (`upgrades`, `world`) que apuntan a los objetos de la sesión; no hay estado duplicado.
- La entidad `Pilot` se crea en T-102, tal como marca el plan.

### Pendiente / siguiente paso

- Continuar con **T-102 — Crear Pilot y migrar salud/O2**.
- No implementar todavía O2, fugas ni supervivencia (Etapa 2).

---

## S-004 — Tema oscuro del panel

- **Fecha:** 2026-11-07T23:16:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** mantenimiento visual del panel
- **Resultado:** COMPLETADO

### Resumen

Se cambió `docs/PROJECT_DASHBOARD.html` a un tema oscuro completo: fondo negro/azul profundo, tarjetas oscuras, texto claro, badges y estados con contraste adaptado. Se mantuvieron los datos, filtros, detalles, enlaces, recarga manual y auto-recarga.

### Archivos modificados

- `docs/PROJECT_DASHBOARD.html`
- `docs/IMPLEMENTATION_TRACKER.json` (sesión S-004)
- `docs/IMPLEMENTATION_LOG.md`

### Pruebas ejecutadas

- `python3 tools/sync-dashboard.py --check`: correcto.
- Firefox headless con `file://`: render correcto.
- Marionette: 14/14 comprobaciones (render, filtros, búsqueda, detalle, recarga manual y auto-recarga).

### Evidencias

- Panel oscuro abierto en Brave mediante `file://`.
- Sin recursos externos ni red.
- La primera tarea de implementación sigue siendo `T-101`.

### Pendiente / siguiente paso

- Ninguno para el panel.
- Continuar con `T-101` cuando se pida implementar.

---

## S-003 — Cierre de decisiones de diseño y reorganización

- **Fecha:** 2026-11-07T22:30:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** ninguna de implementación; revisión de planificación
- **Resultado:** COMPLETADO

### Resumen

Se aplicaron las respuestas del usuario a las decisiones pendientes:

- D-001: muerte permanente, mundo persistente y nave legado; modo de desarrollo para recuperación.
- D-002: eliminar la tienda económica de la pausa.
- D-003: separar láser, misiles y rayo tractor.
- D-004: viaje warp libre con carga dinámica de sectores, sin puertas obligatorias.
- D-005: estación personal inicial vacía y modular.
- D-006: interiores de estaciones y naves a largo plazo.
- D-009: audio completo, música y ambientes.
- D-010: naves distintas y personalización por módulos.
- D-011: galaxias reales y ficticias.
- D-012: facciones/combate/online aplazados; arquitectura abierta.

No se implementó ninguna mecánica del juego.

### Archivos modificados

- `docs/DECISIONS.md`
- `docs/PENDING_DECISIONS.md`
- `docs/MASTER_PLAN.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/PROJECT_DASHBOARD.html`
- `AGENTS.md` (referencia al archivo de decisiones cerradas)

### Tareas reorganizadas

- Nuevas tareas: `T-107`, `T-209`, `T-210`, `T-409`, `T-410`, `T-808`, `T-907`, `T-908`, `T-1007`, `T-1108`, `T-1109`.
- Reactivadas: `T-208`, `T-406`, `T-504`, `T-804`, `T-903`, `T-1001`…`T-1006`, `T-1104`.
- Pospuestas: `T-1103`, `T-1007`.
- Dependencias actualizadas: `T-106`, `T-408`, `T-807`, `T-906`, `T-1107`.

### Pruebas ejecutadas

- `python3 tools/sync-dashboard.py --check`: correcto.
- Validación de IDs, etapas, dependencias y decisiones.
- Sin decisiones pendientes.

### Evidencias

- Tracker con 91 tareas, 12 etapas y 12 decisiones.
- Cero tareas en `REQUIERE_DECISION`.
- Panel sincronizado.

### Pendiente / siguiente paso

- La primera tarea de implementación sigue siendo `T-101`.
- No implementar `T-1103` ni `T-1007` hasta que el usuario lo pida.

---

## S-002 — Pendientes para conversar y apertura del panel

- **Fecha:** 2026-11-07T22:15:00-06:00
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-003, T-006 (mantenimiento de continuidad)
- **Resultado:** COMPLETADO

### Resumen

Se creó `docs/PENDING_DECISIONS.md` con las diez decisiones pendientes, contexto, opciones, recomendación, tareas afectadas y una plantilla para devolver respuestas. Se enlazó desde `AGENTS.md` y `docs/DECISIONS.md`. Se abrió `docs/PROJECT_DASHBOARD.html` en Brave mediante `file://`.

### Archivos modificados

- `docs/PENDING_DECISIONS.md` (nuevo)
- `AGENTS.md`
- `docs/DECISIONS.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/PROJECT_DASHBOARD.html` (sincronizado)

### Pruebas ejecutadas

- `python3 tools/sync-dashboard.py --check`: correcto.
- Apertura de `file:///home/leonisauro/Develop/Android/GalacticIndustry/docs/PROJECT_DASHBOARD.html` con Brave: proceso activo.

### Evidencias

- El panel abrió la URL `file://` en Brave.
- El tracker mantiene 80 tareas, 12 etapas y 12 decisiones.
- La primera tarea de implementación sigue siendo `T-101`.

### Pendiente / siguiente paso

- El usuario conversará las decisiones con otro agente y devolverá respuestas.
- Al recibirlas, actualizar `DECISIONS.md`, el tracker y `PENDING_DECISIONS.md`.
- No implementar mecánicas hasta que el usuario lo pida.

---

## S-001 — Infraestructura de continuidad

- **Fecha:** 2026-11-07T21:41:24-06:00 (inicio) — 2026-11-07T22:06:00-06:00 (cierre)
- **Agente:** OpenCode / DeepSeek V4.1 Flash
- **Tarea(s):** T-003, T-006
- **Resultado:** COMPLETADO

### Resumen

Se construyó la infraestructura persistente de continuidad:

- `AGENTS.md` actualizado como punto de entrada y protocolo.
- `docs/MASTER_PLAN.md` con visión, arquitectura, etapas, fórmulas y riesgos.
- `docs/IMPLEMENTATION_TRACKER.json` con etapas, tareas, dependencias, criterios, decisiones, sesiones y problemas.
- `docs/IMPLEMENTATION_LOG.md` (este archivo).
- `docs/DECISIONS.md` con las decisiones de producto pendientes y las técnicas resueltas.
- `docs/PROJECT_DASHBOARD.html` autocontenido para abrir con `file://`.
- `tools/sync-dashboard.py` para validar el tracker y sincronizar los datos integrados en el HTML.

### Archivos modificados/creados

- `AGENTS.md`
- `docs/MASTER_PLAN.md`
- `docs/IMPLEMENTATION_TRACKER.json`
- `docs/IMPLEMENTATION_LOG.md`
- `docs/DECISIONS.md`
- `docs/PROJECT_DASHBOARD.html`
- `tools/sync-dashboard.py`

### Pruebas ejecutadas

- Validación de estructura del JSON.
- Comprobación de IDs únicos.
- Comprobación de dependencias existentes.
- Comprobación de ausencia de ciclos.
- Sincronización JSON → HTML.
- Comprobación de que el HTML no referencia recursos externos (CDN, fetch, XHR, WebSocket).
- Firefox 159 headless con `file://`: render completo del panel sin servidor.
- Marionette: 14 comprobaciones (render inicial, filtros por estado, búsqueda, detalle de tarea, clic de etapa, limpieza de filtros, activación/desactivación de auto-recarga, recarga manual y auto-recarga real de 10 s). Resultado: **14/14 PASS**.
- Comprobación estructural de que el panel puede abrirse con `file://`.

### Evidencias

- `python3 tools/sync-dashboard.py --check` termina sin errores.
- El panel contiene el bloque `tracker-data` con la copia del tracker.
- El panel se renderizó en Firefox headless desde `file://`.
- No se modificó ningún archivo de mecánicas del juego.

### Decisiones tomadas

- El tracker JSON es la fuente canónica.
- El HTML contiene una copia integrada porque `fetch()` no es fiable con `file://`.
- La auto-recarga del panel es opcional y se puede pausar.
- Las tareas de preparación no cuentan para el progreso de implementación.
- La primera tarea de implementación será `T-101`.

### Pendiente / siguiente paso

- Iniciar `T-101` en una sesión nueva: crear `PlayerProfile` y `GameSession`.
- Antes de modificarlo, contrastar el estado real del código.
- No implementar O2, carga, minería ni estación en esa sesión.

---

## S-000 — Auditoría y plan maestro

- **Fecha:** 2026-11-07T21:41:24-06:00
- **Agente:** OpenCode — sesión de arquitectura
- **Tarea(s):** T-001, T-002, T-004, T-005
- **Resultado:** COMPLETADO

### Resumen

Se auditó el proyecto existente y se redactó el plan maestro de transformación. Se distinguieron sistemas existentes, parciales y ausentes. Se documentaron fórmulas, valores actuales, guardado, tests, riesgos y decisiones pendientes.

### Archivos revisados

- `AGENTS.md`
- `docs/arquitectura.md`
- `docs/build.md`
- `docs/pruebas.md`
- `docs/convenciones.md`
- `src/com/example/codegame/*.java`
- `tools/tests/*`
- `AndroidManifest.xml`, `deps.json`, `app/module.toml`

### Pruebas ejecutadas

- `SelfTest` sobre `build/classes`: 5 ejecuciones.
- Resultado: 4 ejecuciones `TODO OK`; 1 ejecución con `1 prueba(s) fallida(s)` no identificada.
- No se ejecutaron los tests de dispositivo `02–09`.
- No se midió FPS, memoria ni batería.

### Evidencias

- Auditoría incorporada a `docs/MASTER_PLAN.md`.
- Lista de decisiones en `docs/DECISIONS.md`.
- Problema `I-001` registrado en el tracker por la flakiness.

### Decisiones tomadas

- Mantener Java + LibGDX y la base existente.
- No reescribir el juego.
- Separar piloto/nave antes de añadir supervivencia.
- Introducir guardado versionado antes de añadir datos nuevos.
- Usar sectores finitos en lugar de un infinito uniforme.
- Usar agentes ligeros y deterministas, no LLM ni servicios externos.

### Pendiente / siguiente paso

- Crear la infraestructura de continuidad (hecho en S-001).
- Resolver decisiones de producto cuando las etapas las necesiten.

---

## Plantilla para la próxima sesión

```markdown
## S-00X — Título

- **Fecha:**
- **Agente:**
- **Tarea(s):**
- **Resultado:**

### Resumen

### Archivos modificados

### Pruebas ejecutadas

### Evidencias

### Decisiones tomadas

### Pendiente / siguiente paso
```

---

## Nota sobre interrupciones

Si una sesión se interrumpe:

1. El tracker debe dejar la tarea en `EN_PROGRESO`.
2. Este log debe anotar qué se hizo y qué falta.
3. El siguiente agente debe leer el log, inspeccionar el código y continuar la misma tarea.
4. No debe asumir que la tarea está terminada ni empezar otra.
5. Si no puede continuar, debe marcarla `BLOQUEADO` con el motivo exacto.
