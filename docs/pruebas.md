# Pruebas

## SelfTest (lógica, sin dispositivo)

No usa Android ni abre ventana. Requiere que `apkc build` haya creado
`build/classes`:

```sh
apkc build --inc
java -cp "build/classes:jar/*" com.example.codegame.SelfTest
```

Imprime `PASS`/`FAIL` y termina en `TODO OK` si todo pasa. Incluye:

- Economía de combustible, disparo, colisiones, división de meteoritos,
  guardado/restauración y tienda.
- Partes de la nave: casco a 0 termina la partida, el motor dañado acelera
  menos, el depósito destruido pierde capacidad y fuga, el cañón destruido no
  dispara, el timón dañado gira menos, un ala perdida desvía el rumbo y reduce
  el giro, y reparar el depósito recupera capacidad.
- Piloto automático: llegada a bidones (incluido uno muy cercano), destrucción
  de meteoritos, ausencia de golpes y **frenada tras perder el objetivo**
  (incluida una frenada desde 160 u/s, que debe terminar por debajo de
  `Autopilot.STOP_SPEED`). También comprueba que reiniciar la partida cancela el
  piloto anterior.
- Benchmarks de eficiencia del piloto (`BENCH`/`TRAZA`).

## Tests en `tools/tests/`

Los tests son módulos independientes. No se ejecuta la suite completa por
defecto: el runner, sin argumentos, solo lista.

- `01_logic` — SelfTest de lógica, sin dispositivo.
- `02_navigation` — menú y tienda, sin partida.
- `03_controls` — partida, acelerador y pausa (sin tienda).
- `04_autopilot` — frenada `STOP`, velocidad final y acelerador a 0.
- `05_death` — explosión, diálogo `OVER` y reintento.
- `06_visual_menu` — capturas de menú y tienda.
- `07_visual_play` — capturas de partida y pausa.
- `08_visual_death` — capturas de la explosión y el diálogo.
- `09_wing_damage` — ala perdida, humo y pestaña de reparaciones.
- `10_survival` — HUD de O2/salud, aviso de O2 bajo y fuga/brecha.
- `11_cargo` — bodega, masa, HUD de carga y persistencia al pausar.
- `12_mining` — piloto MINE, haz y energía contextual, extracción a bodega y roca agotada.
- `13_tractor` — rayo tractor: atracción, energía, captura de fragmentos y HUD.
- `13_station` — nave nodriza: escala, zona segura, Autopilot.STATION, acople, servicios, persistencia, rescate y despegue.
- `14_market` — mercado: stock, objetivo, spread, cantidades, venta parcial/total, `NADA`, compra, confirmación y persistencia tras reiniciar. Anti-abuso y 30 min simulados en `01_logic`.
- `16_universe` — nuevo personaje, mundo persistente y recuperación de la nave legado.

```sh
tools/run-tests.sh --list
tools/run-tests.sh 03_controls
tools/run-tests.sh 01_logic 04_autopilot
tools/run-tests.sh 06 07           # por prefijo
tools/run-tests.sh --no-build 04_autopilot
tools/run-tests.sh --all           # todos, solo si lo pides
```

El runner compila e instala **una vez** para los tests seleccionados; cada
módulo limpia los datos de la app pero no reinstala. Si se lanza un módulo
suelto (`bash tools/tests/04_autopilot.sh`), se prepara él mismo.

`tools/tests/testlib.sh` contiene las utilidades compartidas (geometría, gestos,
telemetría, capturas y validación). No se ejecuta directamente.

Salidas:

- `build/screens/*.png` — capturas de diseño.
- `build/test-logs/selftest.txt` y `build/test-logs/crash.txt`.
- Las capturas y logs están dentro de `build/`, que no se versiona.

Los tests de dispositivo activan temporalmente **No Molestar** para que no
aparezcan notificaciones personales en las capturas y restauran el valor
anterior en un `trap` (también con Ctrl-C).

## Extras de arranque para pruebas

La app los lee en `MainActivity` desde el `Intent`; un arranque normal no los
usa.

```sh
# Nave inmortal + telemetría JSON (tag AsteroidsTelemetry)
adb shell am start -n com.example.codegame/.MainActivity --ez test_mode true

# Explosión final inmediata
adb shell am start -n com.example.codegame/.MainActivity --ez debug_death true

# Explosión ralentizada para capturar fotogramas de diseño
adb shell am start -n com.example.codegame/.MainActivity \
  --ez debug_death true --ez slow_death true

# Piloto automático: objetivo cercano y nave a 160 u/s para probar la frenada
adb shell am start -n com.example.codegame/.MainActivity \
  --ez test_mode true --ez autopilot_test true

# Partes dañadas: ala derecha perdida, motor/depósito/cañón tocados y 200 monedas
adb shell am start -n com.example.codegame/.MainActivity --ez wing_test true
```

- `test_mode`: `world.immortal = true`; la nave no recibe dano y los impactos
  se cuentan en `ignoredHits`. La roca que choca se consume para no quedar
  atascada dentro.
- `debug_death`: arranca una partida con 7 monedas y llama a `triggerDeath()`
  inmediatamente.
- `slow_death`: solo lo usa `08_visual_death`; escala la muerte a 0,35×. El
  juego normal tarda 2,45 s en pasar de `DYING` a `OVER`.
- `autopilot_test`: arranca en `PLAYING` con un meteorito a 420 u y la nave a
  160 u/s; el piloto lo destruye y debe pasar por `STOP` hasta `NONE` con
  velocidad final < 12 u/s. Deja a propósito un acelerador manual residual del
  50 % para comprobar que termina en 0. Lo usa `04_autopilot`.
- `wing_test`: arranca en `PLAYING` con el ala derecha destruida, el ala
  izquierda al 85 %, motor al 55 %, depósito al 45 % y cañón al 70 %, además de
  200 monedas para probar la tienda de reparaciones. La nave vira sola hacia el
  ala perdida y deja humo en el muñón.
- `survival_test`: arranca en `PLAYING` con O2 al 24 %, salud al 70 %, casco al
  50 % y brecha abierta. Sirve para revisar el HUD de supervivencia y el aviso
  de O2 bajo sin esperar minutos. Lo usa `10_survival`.
- `pilot_death_test`: arranca en `PLAYING` con el piloto a 0 HP e
  `INCAPACITATED`; `MyGame` debe pasar por `DYING` y `OVER`, marcar la nave
  legado y permitir `NUEVO PERSONAJE`. Lo usa `05_death`.
- `recovery_test`: modo de desarrollo. Guarda un punto seguro al empezar y
  cada 10 s; tras la muerte, el botón `RECUPERAR (DEBUG)` restaura ese punto.
  Sin el flag no existe y el juego normal sigue con muerte permanente.
- `cargo_test`: arranca con la bodega llena de Silicato (100/100 UM) para
  revisar masa y HUD. Lo usa `11_cargo`.
- `mining_test`: arranca con una roca C cercana y el piloto en `MINE`. Debe
  minar 15 UM, llenar la bodega y terminar sin disparar misiles. Lo usa
  `12_mining`, que captura `23_mining_beam` con el haz activo y comprueba que
  `energy` baja de `energyMax` y que `mining=true`.
- Los fragmentos minerales se guardan en v2 y en el formato legacy; el SelfTest
  cubre persistencia, migración sin `fragments` y pérdida configurable.
- `tractor_test`: arranca con tres fragmentos en alcance del tractor. Lo usa
  `13_tractor`, que comprueba `tractorActive`, consumo de energía, captura de
  las 12 UM y la captura `24_tractor`.
- `station_test`: arranca junto a la nave nodriza. Lo usa `13_station` para
  revisar escala, zona segura y telemetría (`stationX/Y`, `stationRadius`,
  `stationDistance`, `stationAsteroids`).
- `station_service_test`: arranca acoplado con 100 UM de Silicato, casco y ala
  dañados, combustible y O2 bajos y 200 créditos. Lo usan `13_station` y
  `09_wing_damage`; sustituye a la antigua tienda de pausa.
- `rescue_test`: arranca en el espacio sin combustible, con 100 UM de carga y
  200 créditos. Lo usa `13_station` para probar el rescate, su espera, coste,
  pérdida de carga y remolque.
- `market_test`: arranca en el mercado con 50 de Hierro y 500 créditos. Lo usa
  `14_market` para comprar, vender, `TODO`, `NADA` y confirmar.
- `docking_test`: arranca a 900 u con `Autopilot.STATION`; el piloto frena en
  la zona de acople y `13_station` pulsa `ACOPLAR`, comprueba `DOCKED` y
  después `DESPEGAR`.
- La nave legado se recupera con el botón `RECUPERAR NAVE` cuando el personaje
  nuevo está a ≤100 u. El desguace da 10–90 créditos según las partes. Lo cubre
  `16_universe`.

## Telemetría

`MyGame` emite por logcat con el tag `AsteroidsTelemetry`:

- `{"event":"state","state":"MENU|PLAYING|PAUSED|SHOP|OVER|DYING"}` en cada
  cambio de estado.
- `{"event":"status",...}` cada 0,4 s con fps, monedas, banco, puntos, oleada,
  casco (`hull`/`maxHull`), integridad de las partes (`parts`), combustible,
  acelerador, velocidad, asteroides, bidones, misiles, `ignoredHits`, posición,
  tamaño de pantalla, modo del piloto (`NONE`, `FUEL`, `ASTEROID`, `STOP`),
  `immortal`, `shopReturn` y `overCoins`. Incluye supervivencia:
  `pilotHealth`, `pilotOxygen`, `pilotStatus`, `pilotGrace`, `pilotLeak`,
  `pilotBreach`, `o2Alert` y `pilotO2Seconds`.
- Eventos puntuales: `death`, `shop`, `run_start`, `buy`, `refill`, `repair`.

Los scripts usan `wait_current_state` y `json_get` para esperar estados y
comprobar campos sin mirar píxeles.

## Balance de supervivencia

- Consumo: 4 O2/min; filtro 1.5 O2/min con `LIFE_SUPPORT` intacto.
- Pérdida neta sin daño: 2.5 O2/min → **75 % de O2 tras 10 min**.
- Fuga: 3 O2/min al 50 % de casco y 4.8/min al 20 %; con brecha, ×1.5.
- O2 a 0: 5 s de gracia + 100 s de salud a 1 HP/s = **105 s de margen** con
  salud completa.
- Reparar `LIFE_SUPPORT` devuelve el filtro a su valor normal (100 % si el
  filtro está sano).
- `SelfTest` imprime `BENCH expedicion 10min O2=75.0 salud=100.0`.

## Gate de la Etapa 4 — minería y recursos

Verificación de T-408; se ejecuta con `01_logic` y las pruebas de dispositivo:

| Criterio | Evidencia |
|---|---|
| C, S y M con tiempos distintos | `PASS S4: C, S y M tienen tiempos distintos` / `los tiempos caben en el plan` |
| La energía limita y recarga | `PASS el capacitor se agota`, `recarga tras 1 s`, `sin energia no se mina` |
| La bodega llena detiene la extracción | `PASS la bodega llena detiene la extraccion` |
| Los misiles fragmentan y dispersan | `PASS el misil dispersa fragmentos`, `parte del material se pierde` |
| El tractor recupera fragmentos | `13_tractor`: 3 fragmentos y 12 UM en bodega |
| Sin monedas directas | `PASS destruir/minar no da monedas`, `S4: minar no da monedas` |
| Roca y fragmentos persisten | `PASS la roca agotada no reaparece`, `los fragmentos se restauran` |
| Venta en estación | `13_station`: 100 Silicato → 200 créditos |

## Gate de la Etapa 5 — estación y ciclo económico

Verificación de T-507:

| Criterio | Evidencia |
|---|---|
| Acople y despegue | `13_station`: `ACOPLAR -> DOCKED -> DESPEGAR -> PLAYING` |
| Descarga y venta | `PASS S5: descarga y venta a precio base`; `13_station` 100 UM → 200 cr |
| Combustible y O2 | `PASS S5: repostar y O2 tienen coste`; 13_station los llena |
| Reparación y mejoras | `PASS S5: reparar parte restaura integridad`, `comprar mejora en estación` |
| Rescate sin softlock | `PASS S5: el rescate evita el softlock`; `13_station` rescate completo |
| Persistencia | `PASS la estación se restaura con su almacén y acople`; recarga tras reiniciar |
| Economía fuera de pausa | `PASS S5: la economia no depende de la pausa`; `03_controls` sin tienda |

## Gate de la Etapa 6 — mercado y anti-exploit

Verificación de T-606:

| Criterio | Evidencia |
|---|---|
| Los precios reaccionan al stock | `PASS S6: los precios reaccionan al stock`; `14_market` |
| No hay arbitraje trivial | `PASS S6: comprar y vender no da arbitraje`; chequeo de ida/vuelta en `14_market` |
| El mercado persiste | `PASS S6: el mercado persiste`; recarga de `14_market` |
| Una expedición completa da beneficio | `PASS S6: una expedición completa da beneficio` |
| La UI es legible | Captura `29_market`: precios, stock y cantidades en vertical |

## Añadir pruebas

- Lógica pura → `SelfTest`; termina en `TODO OK`.
- Estados y flujo de UI → `02_navigation`, `03_controls`, `04_autopilot` y
  `05_death`, con telemetría.
- Supervivencia → `10_survival` (telemetría + captura) y las comprobaciones de
  10 min, fuga, margen de O2 y reparación del filtro en `SelfTest`.
- Aspecto visual → `06_visual_menu`, `07_visual_play` y `08_visual_death`; no
  valides comportamiento por píxeles.

Si cambias la duración o las fases de la muerte, ajusta las esperas de
`05_death` y `08_visual_death`.
