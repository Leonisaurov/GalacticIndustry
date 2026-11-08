# Informe de recuperación de contexto — Asteroides, misiles y rayo tractor

> Prueba de eficiencia de contexto sobre el proyecto GalacticIndustry / Asteroides.
> Sesión de solo lectura: no se modificó código, tracker, plan maestro, `AGENTS.md` ni panel.
> Fecha del informe: 2026-11-08.

---

## 1. Respuesta a la pregunta

> Todo lo marcado como **[código]** está verificado leyendo el fuente; lo marcado como **[doc]** viene de la documentación del proyecto; lo marcado como **[deducción]** es inferencia propia y se señala como tal.

### 1.1 Cómo selecciona el jugador un asteroide

No existe un "sistema de targeting" con bloqueo persistente ni ciclo de objetivos. La selección es **un toque en pantalla que arranca el piloto automático sobre ese objeto**:

- `MyGame.handleTaps()` → `MyGame.pickTarget(Vector2 p)` (`src/com/example/codegame/MyGame.java:1717-1792`). **[código]**
- `pickTarget` exige el módulo `AUTOPILOT`; si no está instalado, devuelve `false` (`MyGame.java:1745`). **[código]**
- Radio de acierto: `slop = 30` u, ampliado a `100` u con el módulo `RADAR` (`MyGame.java:1748-1749`). **[código]**
- Prioridad: primero la estación nodriza (si el toque cae dentro de `radio + slop` → `pilot.station(...)`); después compara el asteroide más cercano (distancia menos su radio) con el bidón más cercano y elige el de menor distancia (`MyGame.java:1750-1785`). **[código]**
  - asteroide → `pilot.mine(rock)` (modo `MINE`);
  - bidón → `pilot.target(cell)` (modo `FUEL`).
- Un toque en espacio libre **no** selecciona nada: cancela el piloto, apaga el láser y se interpreta como disparo (`MyGame.java:1727-1731` y `1625`: `c.fire = keyFire || freeTap`). **[código]**
- Cualquier mando manual (joystick de apuntado, izquierda/derecha, arriba, espacio o la barra de empuje) cancela el piloto (`MyGame.java:1609-1612`). **[código]**
- El objetivo se dibuja como anillo de puntos alrededor del objeto (azul para `ASTEROID`, dorado para `MINE`, verde para bidón) en `Autopilot.draw()`/`drawGlow()` (`Autopilot.java:414-487`). **[código]**

Consecuencia relevante: **el láser minero solo se apunta desde `Autopilot.MINE`**; la única llamada de producción a `ship.laser.aim(...)` está en `Autopilot.java:252` (verificado con búsqueda global de `.aim(`). **[código]**

### 1.2 Cómo funciona el piloto automático

Clase `Autopilot` (488 líneas), máquina de estados con seis modos (`Autopilot.java:18-26`): `NONE`, `FUEL`, `ASTEROID`, `STOP`, `MINE`, `STATION`. **[código]**

- `GameWorld.update()` llama a `pilot.steer(c, this, dt)` **antes** de `ship.update(...)`, y apaga el láser si el modo no es `MINE` (`GameWorld.java:472-477`). El piloto no mueve la nave directamente: rellena el objeto `Controls` del fotograma (`c.turn`, `c.thrust`, `c.aiming`, `c.aimDeg`, `c.fire`). **[código]**
- Control de velocidad: velocidad de aproximación `sqrt(2·brake·distanciaRestante)` con `BRAKE_MARGIN = 0.7`; si la corrección necesaria es menor que `COAST_BAND = 90` u/s no empuja (deja volar por inercia); solo empuja si la desalineación es menor que `THRUST_CONE = 35°` (`Autopilot.java:166-213`). **[código]**
- Evasión: `avoid()` aparta la velocidad deseada de otros asteroides (`SAFE_GAP = 40`, horizonte `LOOKAHEAD = 2.5 s`) y `avoidStation()` del casco de la estación (`Autopilot.java:346-396`). **[código]**
- `ASTEROID`: vuela hasta `FIRE_DIST = 220` u, calcula el punto de intercepción con `interceptTime(...)` (ecuación cuadrática, `Autopilot.java:318-344`) y dispara solo si el desvío lateral del encuentro es menor que `0.6·radio` y si `pendingDamage(w) < rock.hp` (no desperdicia misiles ya en vuelo) (`Autopilot.java:215-240` y `292-312`). **[código]**
- `MINE`: se detiene a `MINE_DIST = 180` u (dentro del alcance del láser, 260), comprueba bodega con masa libre, energía, integridad de `MINING_LASER` y que la roca no esté agotada; entonces `ship.laser.aim(rock)` (`Autopilot.java:242-256`). Si la bodega se llena o la roca desaparece, apaga el láser y pasa a `STOP`. **[código]**
- `STOP`: frena apuntando contra la velocidad, modula el empuje por debajo de `BRAKE_TAPER = 45`, y a menos de `STOP_SPEED = 10` (o tras `STOP_TIMEOUT = 5 s`) pone la velocidad a cero y se cancela (`Autopilot.java:259-290`). Es la salvaguarda documentada en `AGENTS.md` ("el piloto automático frena al perder el objetivo"). **[código]**
- `STATION`: aproximación lenta (< 700 u limita a 90 u/s) hasta la zona de acople y frenada. **[código]**

### 1.3 Destrucción actual con misiles

Cadena completa **[código]**:

1. `Ship.canFire()` exige `cooldown <= 0`, `fuel >= FUEL_PER_SHOT` (2 unidades) y cañón íntegro (`Ship.java:269-271`).
2. `Ship.fire(Upgrades)` descuenta combustible (`FUEL_PER_SHOT · up.fuelUse()`), calcula `damage = round(up.damage() · (0.35 + 0.65·cannonFactor))` y crea un `Missile` en la proa con la dirección y la velocidad de la nave (`Ship.java:273-279`). El misil tiene `SPEED = 560`, `LIFE = 1.1 s`, `RADIUS = 3` (`Missile.java:11-13`).
3. `GameWorld.update()` añade el misil a `missiles` si `c.fire` (`GameWorld.java:500-502`); `updateMissiles` lo avanza y lo elimina al caducar (`GameWorld.java:563-...`).
4. `collideMissilesAsteroids()` detecta `a.hit(miss.pos, Missile.RADIUS)`, resta daño a `a.hp`, marca `a.flash` y, si `hp <= 0`, llama a `destroyAsteroid(i)` (`GameWorld.java:777-795`). El `hp` depende solo del tamaño: pequeño 1, mediano 2, grande 4 (`Asteroid.java:61`); la dureza del tipo **no** afecta al misil.
5. `destroyAsteroid(index)` (`GameWorld.java:797-820`):
   - suma `score += max(10, maxReserve · 2)`;
   - genera partículas y sacudida;
   - elimina la roca del array;
   - reparte la reserva restante: `60 %` como fragmentos (`FRAGMENT_SHARE`), `25 %` se pierde (`FRAGMENT_LOSS`), el resto queda como escombros;
   - si no es pequeña y quedan ≥ 2 UM de escombros, `splitAsteroid(...)` crea dos rocas hijas (grande→mediana, resto→pequeña).
6. `spawnFragments(...)` parte cada mineral en uno o dos `OreFragment` con velocidad radial aleatoria (`GameWorld.java:826-847`). El fragmento dura 90 s, deriva y se recoge **por contacto** (`OreFragment.collect` → `CargoHold.addExact`; `GameWorld.collideShipFragments`, `GameWorld.java:607-...`). Si la bodega está llena, el fragmento sigue en el espacio. **[código]**

Las constantes `fragmentShare`/`fragmentLoss` son campos de instancia ajustables por pruebas (`GameWorld.java:30-38`), coherente con D-003: "no fijar porcentajes definitivos". **[código + doc]**

### 1.4 Extracción con láser (el otro camino, ya implementado)

- `MiningLaser` (`src/com/example/codegame/MiningLaser.java`): `aim`, `stop`, `canMine`, `tick`.
  - `rate = (6 + 1.5·nivel) · ship.laserFactor() · resistencia`, con `resistencia = clamp(1 − max(0, dureza − (3 + nivel)) · 0.10, 0.15, 1)` (`MiningLaser.java:61-75`).
  - `canMine` exige objetivo activo, roca no agotada, `ship.part[Ship.MINING_LASER] > 0`, energía y distancia ≤ `MAX_RANGE = 260` (`MiningLaser.java:81-95`).
  - `tick` acumula fracciones, pide `compositionBatch`, comprueba `cargo.canAddAll`, confirma con `commitExtraction` y consume energía (`MiningLaser.java:106-137`).
- `EnergyCapacitor`: 100 base, drena 3/s mientras mina, recarga 8/s tras 1 s de espera; el tractor drena con `drain()` (`EnergyCapacitor.java`).
- En `GameWorld.update()` el láser tiene prioridad sobre el tractor: si `laserMining`, no se ejecuta la rama de drenaje del tractor (`GameWorld.java:480-494`). **[código]**

### 1.5 Rayo tractor (también implementado)

`TractorBeam` (`src/com/example/codegame/TractorBeam.java`): cada frame atrae hasta `capacity = 2 + nivel` fragmentos dentro de `range = 140 + 25·nivel`, acelerándolos hacia la nave (`speed = 120 + 30·nivel`, tope 260) y consumiendo `1.5 energía/s` solo si atrae algo. **No recoge**: la captura sigue siendo por contacto. **[código]** Mejoras en el loadout: ranura `TRACTOR` (`ShipLoadout.java:31-33`), aplicadas por `Ship.syncTractor` (`Ship.java:260-262`).

### 1.6 Cómo se generan actualmente las recompensas

- **Destruir una roca no da monedas.** Solo da `score` (telemetría y pantalla final) y mineral disperso en fragmentos. Está explícito en el código (`GameWorld.java:800`: "destruir no da monedas; parte del mineral queda disperso") y en D-003/T-405. **[código + doc]**
- **Ruta de valor**: fragmentos/mineral → `CargoHold` → venta en la estación (`TradeService.sell`, `MarketScreen` botón "VENDER") o entrega de contratos (`MissionService`, `upgrades.addCoins(mission.reward)`). **[código]**
- El láser entrega el 100 % de lo extraído a la bodega; el misil entrega como máximo el 60 % y dispersa el resto entre pérdida (25 %) y escombros. **[código]**

### 1.7 Qué debe evolucionar para las tres herramientas aprobadas

Lo primero, un dato que suele perderse: **las tres herramientas ya están implementadas**. La etapa S4 está `COMPLETADO` con sus 10 tareas (`T-401`…`T-410`), y `MASTER_PLAN.md:703` marca el gate S4 como "Completada: minar, fragmentar y recuperar". **[doc verificado en el tracker vía consulta JSON]**

Lo que queda no es implementación nueva, sino **alineación y balance** con D-003:

| Pieza | Estado real | Qué falta según el diseño aprobado |
|---|---|---|
| Láser minero | Implementado (`MiningLaser`, `EnergyCapacitor`, integración en `CargoHold`) | Que sea la vía **principal y controlable**: hoy solo se usa vía `Autopilot.MINE`; no hay minería manual ni selector de herramienta (D-003, `MASTER_PLAN.md:389-395`) |
| Misiles | Implementados como fragmentadores (`destroyAsteroid`) con 60/25 % de reparto | Sacarlos del bucle de minería y fijar el balance por pruebas; `FRAGMENT_SHARE`/`FRAGMENT_LOSS` son "valores iniciales" (D-003 líneas 155-159) |
| Rayo tractor | Implementado (`TractorBeam`), con mejoras de alcance/velocidad/capacidad | Balance de energía y capacidad; el diseño dice "capacidad" y hoy limita fragmentos **simultáneos**, no almacenamiento (D-003) |

Evolución futura que **sí** está pendiente y afecta al sistema: **T-1103** (facciones/combate/online) está `POSPUESTO`; ahí encajaría "misiles como arma de combate", una de las opciones consideradas en D-003 (`docs/DECISIONS.md:141-143`). **[doc]**

### 1.8 Implementado vs planificado vs deducido

**Implementado y verificado en código:** tipos C/S/M con dureza/reserva/composición (`AsteroidType`), reserva y composición por roca (`Asteroid`), selección por toque (`MyGame.pickTarget`), piloto con 6 modos (`Autopilot`), láser con energía, misiles con fragmentación y división de rocas, tractor con atracción y consumo de energía, recogida por contacto, venta y misiones como fuente de monedas.

**Aprobado pero no cerrado:** porcentajes definitivos de eficiencia/pérdida (D-003 dice explícitamente que se fijarán por balance), misiles como arma de combate (aplazado a T-1103).

**Deducciones propias (no afirmadas por la documentación):**
- `[deducción]` En partida normal **no se puede minar sin el módulo `AUTOPILOT`**: el modo `MINE` solo se activa en `pickTarget`, que está bloqueado por `hasShipModule(AUTOPILOT)`; no encontré otra ruta de producción (solo `MyGame:579` en un modo de prueba y `SelfTest`).
- `[deducción]` Tocar espacio vacío dispara un misil, lo que crea fricción con "misiles fuera del bucle de minería": es fácil gastar combustible disparando sin querer.
- `[deducción]` `Upgrades.tractorRange()` solo se usa en `SelfTest`; el alcance efectivo lo calcula `TractorBeam`, así que es código prácticamente muerto y una fuente potencial de confusión.

---

## 2. Resumen estadístico

Métricas **reales** (contadas sobre las llamadas realizadas) salvo donde se indique "estimación".

| Métrica | Valor real |
|---|---:|
| Operaciones totales de herramienta | **40** |
| Búsquedas de archivos (`glob`) | 2 (1 fallida) |
| Búsquedas de contenido (`grep`/`rg`) | 17 |
| Consultas a registros de estado (tracker JSON vía script) | 2 |
| Operaciones de lectura de archivos | 18 (17 `read` + 1 lectura parcial con `sed`) |
| Otras operaciones (medición `wc -l`) | 1 |
| Archivos abiertos directamente | 15 (3 de docs + 12 de código) |
| Archivos con contenido visible (incl. solo `grep`) | ≈ 35 |
| Archivos ya presentes en el contexto inicial | 2 (`AGENTS.md` global y `AGENTS.md` del proyecto) |
| Relecturas del **mismo fragmento** | 0 |
| Consultas repetidas al **mismo archivo** | 17 operaciones sobre 5 archivos (`MyGame` 6, `GameWorld` 4, `Ship` 3, `MASTER_PLAN` 2, tracker 2) |
| Búsquedas sin información útil | 3 (más 2 de valor bajo) |
| Lecturas completas evitables | 1 clara (`MyGame.java:490-624`, 135 líneas para ~10 útiles) + 1 redundante |
| Líneas leídas con `read` | **1.931** (suma exacta de los rangos devueltos) |
| Salida de `grep` recibida | ≈ 500-700 líneas (**estimación**, varias truncadas) |
| Tokens de contexto | **No medible**: el entorno no expone contadores de tokens |
| Tiempo | **No medible de forma verificable**: no hay marcas de tiempo accesibles por operación |

Nota de honestidad: no puedo afirmar cuántos tokens se consumieron ni cuánto tiempo se tardó. Solo puedo certificar el número de llamadas, los archivos y las líneas leídas.

---

## 3. Registro de operaciones

| Nº | Operación | Archivo o recurso | Motivo | Resultado | ¿Era necesaria? |
|---:|---|---|---|---|---|
| 1 | Búsqueda de archivos | `app/src/**/*.java` | Localizar el código | 0 resultados (ruta equivocada) | **No** |
| 2 | Búsqueda de contenido | `docs/` (`tractor\|láser\|misil`) | Localizar decisiones y diseño | 120 coincidencias; apuntó a D-003 y §6.3 | Sí |
| 3 | Búsqueda de archivos | `src/**/*.java` | Localizar el código real | 79 archivos, incluidos los 6 clave | Sí |
| 4 | Lectura de documentación | `docs/DECISIONS.md` 118-167 | Leer D-003 completa | Decisión aprobada + implementación T-406 | Sí |
| 5 | Otra (medición) | `wc -l` de 14 archivos | Decidir qué leer entero | Tamaños; `Autopilot` 488, `GameWorld` 1067 | Sí (barata) |
| 6 | Búsqueda de contenido | `src/` (`target`) | Localizar el concepto de objetivo | `MiningLaser.target`, `Autopilot.target` | Sí |
| 7 | Lectura de código | `Missile.java` (66) | Entender el proyectil | Constantes y daño | Sí |
| 8 | Lectura de código | `MiningLaser.java` (142) | Fórmulas del láser | Fórmula, `canMine`, `tick` | Sí |
| 9 | Lectura de código | `TractorBeam.java` (102) | Entender el tractor | Alcance/velocidad/capacidad/drenaje | Sí |
| 10 | Lectura de código | `OreFragment.java` (112) | Entender la recompensa física | Vida 90 s, captura por contacto | Sí |
| 11 | Lectura de código | `AsteroidType.java` (85) | Tipos y composición | Tabla C/S/M | Sí |
| 12 | Lectura de código | `Asteroid.java` (274) | Reserva, hp, composición | `hp` por tamaño, `compositionBatch` | Parcial (bastaba la mitad) |
| 13 | Búsqueda de contenido | `GameWorld.java` (mapa de líneas) | Localizar misiles/fragmentos/auto | Mapa de métodos clave | Sí |
| 14 | Lectura de código | `Autopilot.java` (488) | Núcleo del piloto | Modos, frenada, disparo predictivo | Sí |
| 15 | Búsqueda de contenido | `MyGame.java` (`pilot\|MINE\|fire`) | Localizar integración | 80 líneas, mayoría sobrecargas del constructor | **Parcial** |
| 16 | Lectura de código | `GameWorld.java` 460-534 | Orden del `update` | Láser/tractor/misiles/colisiones | Sí |
| 17 | Lectura de código | `GameWorld.java` 770-864 | Destrucción y recompensas | `destroyAsteroid`, `spawnFragments`, `splitAsteroid` | Sí |
| 18 | Búsqueda de contenido | `src/` (`pilot.target/mine/station`) | Ver quién fija objetivos | `MyGame:519/579/607/1782` | Sí |
| 19 | Búsqueda de contenido | `PlayHud`, `UiKit`, `GameWorld` (`MINAR\|PILOTO`) | Buscar botones de herramienta | 0 resultados | **No** |
| 20 | Lectura de código | `MyGame.java` 490-624 | Ver arranques de prueba | 135 líneas; solo ~10 útiles | **Parcial** |
| 21 | Lectura de código | `MyGame.java` 1755-1814 | Final de `pickTarget` | `mine` vs `target` | Sí |
| 22 | Lectura de código | `MyGame.java` 1712-1761 | `handleTaps` + inicio de `pickTarget` | Módulo AUTOPILOT, slop, radar | Sí |
| 23 | Búsqueda de contenido | `Ship.fire/canFire` + constantes `GameWorld` + `score` | Coste y reparto del misil | 2 de combustible, 60/25 %, score | Sí |
| 24 | Consulta de estado | Tracker: etapas y estados | Saber si S4 está cerrada | S4 `COMPLETADO` 10/10 | Sí |
| 25 | Búsqueda de contenido | `Ship.java` (`FUEL_PER_SHOT`, factores) | Confirmar coste | Redundante con 23 | **No** |
| 26 | Consulta de estado | Tracker: tareas S4 + menciones | Qué está implementado | T-401…T-410 `COMPLETADO` | Sí |
| 27 | Lectura de documentación | `MASTER_PLAN.md` 360-404 | Fórmulas y §6.3.1 aprobada | Diseño de las tres herramientas | Sí |
| 28 | Lectura de documentación | `MASTER_PLAN.md` 255-284 | §4.6 Minería | "Misiles secundarios, no minería principal" | Sí |
| 29 | Búsqueda de contenido | `TradeService`, `MarketScreen` (`sell`) | Confirmar la fuente de monedas | Venta de mineral | Sí |
| 30 | Búsqueda de contenido | `MyGame` (`handleTaps\|c.fire`) | Ver el disparo manual | `c.fire = keyFire \|\| freeTap` | Sí |
| 31 | Lectura de código | `EnergyCapacitor.java` (77) | Consumo del láser | 3/s, recarga 8/s | Sí |
| 32 | Búsqueda de contenido | `src/` (`.aim(`) | ¿Se puede apuntar el láser a mano? | Solo `Autopilot.MINE` | Sí (decisivo) |
| 33 | Lectura de código | `MyGame.java` 1598-1642 | Cancelación por mando manual | Piloto cancelado por cualquier input | Sí |
| 34 | Búsqueda de contenido | `ShipModuleDef` (AUTOPILOT/RADAR) | Nombres e instalación | IDs y descripciones; segunda mitad vacía | **Parcial** |
| 35 | Búsqueda de contenido | `MissionService` (`coins\|reward`) | Recompensas de misiones | `mission.reward` → `addCoins` | Sí |
| 36 | Búsqueda de contenido | `ShipLoadout` (`ShipModuleDef.`) | Módulos por defecto | 0 resultados | **No** |
| 37 | Búsqueda de contenido | `src/` (`ShipModuleDef.AUTOPILOT`) | ¿Dónde se instala/exige? | Solo test y `pickTarget` | Sí |
| 38 | Lectura parcial de código | `ShipLoadout.java` 1-70 + `Upgrades` | Ranuras y niveles | `LASER=6`, `TRACTOR=7`, comentario obsoleto | Sí |
| 39 | Búsqueda de contenido | `Ship`/`Upgrades` (`laserLevel`, `tractorRange`) | Cómo se aplican las mejoras | `syncLaser`, `syncTractor`, `tractorRange` | Sí |
| 40 | Búsqueda + lectura | `Upgrades.tractorRange` uso + `Ship` 190-215 | ¿Está muerto? | Solo lo usa `SelfTest` | Sí (deducción importante) |

Clasificación agregada: búsqueda de archivos 2 · búsqueda de contenido 17 · lectura de documentación 3 · lectura de código 15 · consulta de registros de estado 2 · otras 1.

---

## 4. Diagnóstico

### A. Costes necesarios

Mínimo real para responder con fiabilidad: **~22 operaciones**.

Imprescindibles: 3 (mapa de archivos), 2 (localizar D-003 por grep), 4 (D-003), 7-11 (modelo de datos del sistema), 13 (mapa de `GameWorld`), 14 (`Autopilot`), 16-17 (bucle y destrucción), 21-22 (selección), 23 (coste/daño del misil), 24/26 (estado real de las tareas), 27 (diseño aprobado), 29 (monedas), 31 (energía), 32 (¿láser manual?).

La operación **32** fue la de mayor valor por coste: una sola búsqueda de `.aim(` demostró que la minería depende por completo del piloto automático, algo que ningún documento dice.

### B. Costes evitables

- **Op 1**: ruta inventada (`app/src`). `AGENTS.md` menciona `app/module.toml` como "puente", y eso induce a buscar el código en `app/`. Coste: 1 operación y una corrección.
- **Op 19 y 36**: búsquedas que devolvieron vacío porque se asumieron nombres que no existen (botones "MINAR"; módulos referenciados en `ShipLoadout`).
- **Op 20**: 135 líneas de constructores de prueba para extraer 10 líneas útiles. Se habría resuelto con `grep "miningTest"`.
- **Op 25**: redundante con 23.
- **Op 34**: mitad de la operación sin resultado.

### C. Problemas de organización detectados

1. **No existe un índice "mecánica → archivo"**. `AGENTS.md` lista documentación por *tipo* (plan, tracker, log, decisiones, build, pruebas, arquitectura), no por *sistema*. Para saber qué clases tocan la minería hubo que buscar por palabras clave en `src/`.
2. **Duplicación de la misma decisión en cuatro sitios**: D-003 aparece en `DECISIONS.md`, en `PENDING_DECISIONS.md` (resumen), en `MASTER_PLAN.md` (§6.3.1 y tabla final) y en el tracker (`decisions`), además de narrarse en el log (S-034, S-035, S-038). Encontrar "qué se aprobó" cuesta varias lecturas cruzadas.
3. **`IMPLEMENTATION_LOG.md` es enorme y mezcla historia con estado.** Produjo buena parte del ruido de la op 2 (más de la mitad de las 120 coincidencias venían del log, no de decisiones).
4. **El tracker canónico tiene 8.963 líneas.** Es ilegible sin script; hubo que consultarlo con Python dos veces. No hay un resumen compacto generado.
5. **El resumen de estado de `AGENTS.md` caduca.** Durante esta misma sesión llegó una actualización (T-1107 → T-1110, 81 → 82 tareas). Es decir: el "contexto inicial" puede estar desfasado sin que el agente lo sepa.
6. **Comentario de código obsoleto y contradictorio.** `ShipLoadout.java:36` dice que el módulo de autopiloto "aun no se exige para volar", pero `MyGame.pickTarget` sí lo exige para seleccionar objetivos (y por tanto para minar). Esto puede llevar a conclusiones erróneas.
7. **Tres conceptos llamados "láser"**: la clase `MiningLaser`, el nivel de loadout `ShipLoadout.LASER` y la integridad de la pieza `Ship.MINING_LASER`. Sin leer código, los documentos no aclaran cuál es cuál.
8. **Tareas del tracker sin referencias a archivos**: en S4 el campo `artifacts` está vacío para las diez tareas. No se puede ir de "T-406" a "GameWorld.destroyAsteroid" sin explorar.
9. **Ningún documento dice explícitamente que las tres herramientas ya están implementadas** de forma conjunta; hay que deducirlo cruzando el tracker, el log y `MASTER_PLAN.md:703`.

### D. Contexto inicial

- Se recibieron ~2 archivos de instrucciones (≈200 líneas). De ellos se usó realmente: el protocolo de continuidad, la lista de documentación y las "reglas críticas". El bloque "Estado actual" no aportó a la pregunta y encima estaba desactualizado.
- **Un agente nuevo podría haber respondido con ~15 operaciones** si existiera un índice de sistemas: buscar "minería" en el índice → abrir ficha → abrir 4-5 clases.
- El contexto inicial no fue excesivo en volumen, pero sí **poco accionable**: describe el proceso, no el mapa del código.

### E. Reindexación y recuperación

- **No fue necesaria ninguna reindexación.** No se construyeron índices, no se usó caché, no se regeneró nada. Todo se resolvió con `glob` + `grep` + lecturas dirigidas.
- No existe en el proyecto un índice persistente que consultar; el "índice" real fue el `glob` de `src/**/*.java` (op 3), que es una búsqueda normal de archivos, no una reindexación.
- Las 17 operaciones sobre 5 archivos no fueron relecturas del mismo fragmento (0 casos), sino consultas a rangos distintos del mismo archivo grande. Eso es señal de que **los archivos son demasiado grandes para una lectura única** (`MyGame` 2.487 líneas, `GameWorld` 1.067).

---

## 5. Propuestas de mejora

Ordenadas por impacto/prioridad. Todas son propuestas: no se modificó nada.

### P1. Índice central de sistemas (`docs/INDEX.md`)
1. **Problema:** no hay mapa mecánica → clases → decisión → tarea (ops 2, 6, 13, 15, 18, 32, 34, 37 fueron descubrimiento puro).
2. **Solución:** tabla de ~12 filas: `Minería`, `Misiles`, `Tractor`, `Piloto`, `Economía`, `Naves/módulos`… con columnas: clases principales, documento de diseño, decisión, tareas, pruebas asociadas.
3. **Archivos:** nuevo `docs/INDEX.md`; añadir enlace en `AGENTS.md` ("Documentación existente").
4. **Beneficio esperado:** elimina la mayoría de búsquedas exploratorias; estimación de **−30/−40 % de operaciones** en preguntas de este tipo.
5. **Coste:** bajo (1 archivo, ~60 líneas, una sesión corta).
6. **Inconveniente:** se desactualiza si no se mantiene; mitigable incluyéndolo en el checklist de cierre.

### P2. Resumen de estado compacto generado (`docs/STATUS.md`)
1. **Problema:** tracker de 8.963 líneas + resumen de `AGENTS.md` que caduca (observado en esta sesión).
2. **Solución:** extender `tools/sync-dashboard.py` para emitir un `STATUS.md` de ~40 líneas: etapa, tarea actual/siguiente, tareas por estado, decisiones abiertas, último commit.
3. **Archivos:** `tools/sync-dashboard.py`, nuevo `docs/STATUS.md`, referencia en `AGENTS.md`.
4. **Beneficio:** el agente lee 40 líneas en vez de consultar el JSON; elimina el riesgo de contexto inicial obsoleto.
5. **Coste:** bajo-medio (el script ya parsea y valida el tracker).
6. **Inconveniente:** un artefacto generado más que recordar commitear.

### P3. Fichas por sistema (`docs/systems/mining.md`, `autopilot.md`, …)
1. **Problema:** la información de un sistema está repartida entre 5 documentos; D-003 se repite en 4 sitios.
2. **Solución:** una ficha de 1 página por mecánica con: estado real, fórmulas, clases, decisiones aplicables, pruebas, deuda técnica conocida. D-003 se resume una vez y se enlaza.
3. **Archivos:** nuevos `docs/systems/*.md`; recortar duplicación en `MASTER_PLAN.md` §6.3.1 y `PENDING_DECISIONS.md`.
4. **Beneficio:** responder la pregunta habría costado abrir 2 archivos en vez de 15.
5. **Coste:** medio (5-8 fichas).
6. **Inconveniente:** riesgo de una cuarta copia de la verdad si no se marca cuál es canónica.

### P4. Referencias de archivos en el tracker
1. **Problema:** las tareas de S4 no tienen `artifacts`; no se puede ir de tarea a código.
2. **Solución:** rellenar `artifacts`/`files` en las tareas ya completadas y hacerlo obligatorio en las nuevas; añadir `contextFiles` en las pendientes.
3. **Archivos:** `docs/IMPLEMENTATION_TRACKER.json`, `tools/sync-dashboard.py` (validación).
4. **Beneficio:** cada tarea se vuelve autocontenida; reduce lecturas cruzadas.
5. **Coste:** medio (rellenar ~80 tareas, mecanizable parcialmente).
6. **Inconveniente:** mantenimiento; validar que las rutas existen.

### P5. Partir el log y el plan por tamaño
1. **Problema:** `IMPLEMENTATION_LOG.md` (miles de líneas) contamina las búsquedas; `MASTER_PLAN.md` (913) obliga a leer secciones.
2. **Solución:** archivar sesiones antiguas en `docs/log/AAAA.md` y dejar el log raíz con las últimas ~10; trocear el plan por capítulos (`docs/plan/06-mineria.md`) con un índice.
3. **Archivos:** `docs/IMPLEMENTATION_LOG.md`, `docs/MASTER_PLAN.md`, enlaces en `AGENTS.md`.
4. **Beneficio:** `grep` deja de devolver ruido histórico (la op 2 habría dado ~40 coincidencias en lugar de 120).
5. **Coste:** medio (mover texto, actualizar enlaces).
6. **Inconveniente:** enlaces roto si no se revisan.

### P6. Ajustes concretos en `AGENTS.md` y en el código
1. **Problema:** ruta de código no indicada (provocó la op 1 fallida); comentario obsoleto en `ShipLoadout.java:36`.
2. **Solución:** añadir una línea "Código: `src/com/example/codegame/` (no existe `app/src`)" y corregir el comentario del loadout.
3. **Archivos:** `AGENTS.md`, `src/com/example/codegame/ShipLoadout.java`.
4. **Beneficio:** evita operaciones fallidas y conclusiones erróneas.
5. **Coste:** muy bajo (2 líneas).
6. **Inconveniente:** ninguno relevante.

### P7. Plantilla de "contexto mínimo" por tarea
1. **Problema:** cada sesión vuelve a explorar el mismo subsistema.
2. **Solución:** campo `contextFiles` (3-6 rutas) + `contextDoc` en cada tarea; el protocolo de inicio los lee primero.
3. **Archivos:** tracker, `AGENTS.md` (protocolo), `tools/sync-dashboard.py`.
4. **Beneficio:** reduce el arranque a una lectura dirigida.
5. **Coste:** bajo por tarea, alto si se rellena retroactivamente todo.
6. **Inconveniente:** si las rutas no se actualizan, el agente lee archivos irrelevantes con falsa confianza.

No se recomiendan cachés ni índices persistentes adicionales: el proyecto es pequeño (79 archivos Java) y `grep` resolvió todo en milisegundos. El problema no es la velocidad de búsqueda, es **no saber qué buscar**.

---

## 6. Evaluación final

**¿El sistema actual permite que un agente nuevo encuentre rápidamente la información?**
Sí, pero **por fuerza bruta**. Con 79 archivos Java y una documentación coherente, `grep` encuentra todo. No hubo contradicciones de fondo entre código y documentación (salvo el comentario obsoleto de `ShipLoadout`). Pero el camino fue de descubrimiento, no de consulta: ningún documento indicaba "la minería está en `MiningLaser`, `Autopilot.MINE`, `GameWorld.update` y D-003".

**¿Consideras que hubo consultas excesivas?**
Ligeramente. Con un índice habrían bastado ~20-25 operaciones; se usaron 40. El exceso no vino de leer de más, sino de **buscar dónde mirar**.

**¿Qué porcentaje fue potencialmente evitable?**
- Estrictamente inútiles: 3 de 40 = **7,5 %** (ops 1, 19, 36 — contadas, no estimadas).
- Inútiles + parcialmente evitables (ops 15, 20, 25, 34): 7 de 40 = **17,5 %**.
- **Estimación**, no medición: la clasificación de "parcialmente evitable" es un juicio subjetivo. El cálculo consiste en dividir operaciones cuyo resultado aportó menos que su coste entre el total de llamadas. No incluye el coste de las lecturas grandes que sí fueron necesarias (`Autopilot`, `GameWorld`).
- Si además se contabilizara el "descubrimiento" que un índice eliminaría, el ahorro potencial sería del **30-40 %**, pero eso ya es una proyección, no un dato.

**¿Cuál es el principal cuello de botella?**
La **ausencia de un mapa sistema → código → decisión**, agravada por un tracker canónico de 8.963 líneas y un log que duplica la información de diseño. El agente sabe *qué* preguntar, pero no *dónde*.

**¿Qué cambio produciría la mayor mejora?**
`docs/INDEX.md` (P1) combinado con `docs/STATUS.md` generado (P2). Juntos convierten el arranque de "explorar 40 operaciones" en "leer 2 archivos y abrir 4 clases". Son además los dos cambios más baratos y los que menos riesgo de desactualización tienen si se integran en `tools/sync-dashboard.py`.

---

## 7. Nota de cumplimiento

No se creó, modificó ni eliminó ningún archivo del proyecto durante la investigación; no se tocó tracker, plan maestro, `AGENTS.md` ni el panel; no se implementó nada. Por eso tampoco se ejecutó el protocolo de "marcar `EN_PROGRESO`" ni `sync-dashboard.py`: la prueba fue de solo lectura y las restricciones de la tarea prevalecían sobre el protocolo normal. La única alteración externa fue la actualización de `AGENTS.md` notificada por el entorno a mitad de sesión, ajena a las acciones del agente.

Este archivo (`informe-nav.md`) es la entrega del informe solicitada a posteriori por el usuario.
