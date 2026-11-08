# Arquitectura, render y piloto automático

## El juego

- Objetivo: sobrevivir, destruir meteoritos para ganar monedas y comprar
  mejoras entre partidas.
- Táctil: joystick izquierdo para girar, barra derecha para acelerar, toque en
  el vacío para disparar y toque sobre una roca o bidón para activar el piloto
  automático.
- Teclado: flechas para girar/acelerar y espacio para disparar.
- Economía: acelerar y disparar gastan combustible; los bidones lo recargan y
  las monedas se conservan entre partidas. La tienda vende mejoras, recarga de
  combustible y reparaciones de las partes dañadas.

## Clases

- `MainActivity` → `MyGame` (`ApplicationListener`) y lectura de los extras de
  prueba (`test_mode`, `debug_death`, `slow_death`).
- `MyGame`: máquina de estados `MENU`/`PLAYING`/`PAUSED`/`SHOP`/`OVER`/`DYING`,
  entrada multitáctil, HUD/menús, telemetría JSON y secuencia de muerte. Crea
  una `GameSession` y un `SaveService` v2; carga, guarda y limpia la partida a
  través de ellos.
- `GameSession`: composición de `PlayerProfile`, el sector activo (`GameWorld`,
  que contiene la nave) y los servicios de guardado de la partida. Mantiene un
  alias de `Upgrades` a través del perfil; no duplica estado.
- `PlayerProfile`: identidad estable del jugador/personaje y referencia al
  progreso persistente (`Upgrades`).
- `ShipLoadout`: fuente única de niveles de mejora y ranuras genéricas de
  módulos. `Upgrades` es su fachada de tienda; el módulo de piloto automático
  está previsto pero no se exige todavía para volar.
- `SaveCodec`: serializa/deserializa el guardado v2 con `version`, `profile`,
  `pilot`, `ship` y `sector`. Valida todo el texto antes de aplicarlo.
- `SaveService`: guardado JSON local con `FileStorage` (temporal + rename
  atómico) o `MemoryStorage` en tests. Conserva `slot1.json.bak`, cae al backup
  ante corrupción y migra `run2`. `MyGame` lo usa en pausa, tienda y `dispose()`.
- `Pilot`: salud, O2, máximos, estado (`OK`/`HYPOXIC`/`INCAPACITATED`/`DEAD`) y
  presencia a bordo. Vive en `GameSession`; no pertenece a `Ship` y todavía no
  consume O2 ni tiene HUD.
- `LifeSupport`: filtro, fuga y brecha del soporte vital. Vive en `GameSession`;
  la integridad de `Ship.LIFE_SUPPORT` modulará el filtro en T-202.
- Consumo actual de O2: 4/min; filtro efectivo 1.5/min con soporte intacto,
  modulado como `(0.20 + 0.80·LIFE/100) · filter/100`. Reserva neta ~40 min.
  `LifeSupport.update()` se aplica desde `GameSession.update()`.
- Fuga: `6·(1 − casco/cascoMax)·(1 + 0.5·brecha)` O2/min. Brecha por impacto
  fuerte con casco ≤70 % o por casco ≤20 %. `GameWorld` emite gas y `MyGame`
  muestra `FUGA O2`/`BRECHA O2`; T-205 ampliará el HUD.
- Asfixia: con O2 a 0 hay 5 s de gracia y después 1 HP/s. Salud 0 deja al
  piloto `INCAPACITATED`; la salud es independiente del casco. La muerte
  permanente se conecta en T-208.
- HUD: dos barras compactas bajo el panel superior (`O2` y `HP`) con iconos
  ASCII. Aviso `O2 BAJO` al 25 %, `O2 CRITICO` al 10 % y `FUGA O2`/`BRECHA O2`
  cuando el soporte pierde gas. `survival_test` permite revisar el estado sin
  esperar 40 minutos; `tools/tests/10_survival.sh` valida telemetría y captura.
- Tienda temporal: tarjeta `OXIGENO` a 0,5 créditos/unidad con mínimo 5;
  `SOPORTE` (LIFE_SUPPORT) se repara como las demás partes. Este servicio se
  moverá a la estación física en la Etapa 5.
- Balance verificado: 10 min sin daño dejan 75 % de O2; O2 a 0 con salud completa
  da 105 s de margen; casco al 50 % genera 3 O2/min de fuga. La telemetría
  expone `pilotO2Seconds`.
- `GameWorld`: simulación. Mundo infinito por *chunks* de 800 unidades con
  semilla derivada de las coordenadas; dueño de `Ship`, arrays de
  `asteroids`/`missiles`/`fuel`, `Effects` y `Autopilot`. `serialize()`/
  `restore()` usan texto plano (incluye la integridad de las partes).
  Gestiona `immortal`, `ignoredHits`, temblor de cámara y
  `beginDeath()`/`updateDeath()`. La densidad base es de 2–4 meteoritos por
  *chunk* (más hasta 2 extra según la oleada), ligeramente superior a la
  original para que el espacio no se vea vacío.
- `Visuals`: paleta, texturas generadas en runtime, brillos aditivos y
  `roundRect()`. No tiene estado de partida.
- `Upgrades`: tienda, monedas y niveles. `Upgrades(true)` usa memoria para
  tests; `Upgrades()` usa `Preferences`. `Blindaje` ya no da vidas: aumenta la
  integridad máxima del casco.
- `Autopilot`: lleva la nave a un bidón o meteorito; cualquier mando manual lo
  cancela.
- `Effects`: partículas con estela, explosiones y ondas expansivas; una pasada
  de formas y otra aditiva para los brillos.
- Entidades/UI auxiliares: `Ship`, `Asteroid`, `Missile`, `FuelPickup`,
  `Button`, `Controls`.
- La app está bloqueada en vertical, en pantalla completa y con
  `windowLayoutInDisplayCutoutMode=shortEdges`.

## Render y aspecto visual

- `Visuals` genera en runtime las texturas suaves (brillo, estrella, nube de
  nebulosa y viñeta). No hay imágenes nuevas en `assets/`: todo se dibuja con
  `Pixmap` al arrancar y se libera en `dispose()`.
- Fondo: gradiente con `ShapeRenderer`, estrellas en tres capas de parallax y
  nebulosas aditivas. El menú añade un planeta con bandas y la nave en vista
  previa.
- Entidades: nave con casco facetado, cabina, alas y llama del motor; meteoritos
  con sombreado por caras, cráteres y borde iluminado; bidones con anillo
  pulsante; misiles y partículas con estelas y `Effects.shock()`.
- La nave se dibuja a 1,28× su radio de colisión para que se lea mejor; el
  hitbox sigue siendo `Ship.RADIUS`.
- HUD y menús: paneles redondeados, chips, iconos con formas, barras con
  degradado y texto con sombra. El layout usa la densidad real
  (`u = Gdx.graphics.getDensity()`) y está pensado para vertical.

Orden de las pasadas:

1. `ShapeRenderer` con fondo y entidades.
2. `SpriteBatch` con mezcla aditiva para nebulosas, estrellas y brillos.
3. `SpriteBatch` normal para texto y viñeta.

**Invariante:** después de una pasada aditiva hay que llamar a
`Visuals.normal(batch)` **y** a `beginShapes()`. `ShapeRenderer` usa el estado de
OpenGL directamente, no el `blendFunction` interno de `SpriteBatch`; si no se
restaura, los botones y paneles se dibujan con alfa aditiva (brillos blancos y
sombras que desaparecen).

`Visuals.roundRect()` usa rectángulos + cuartos de arco, no círculos completos,
para que las esquinas no dupliquen el alfa. Evita crear `Color` dentro de
`render()`: usa constantes de `Visuals` o de `MyGame`.

## Piloto automático

Modos: `NONE`, `FUEL`, `ASTEROID` y `STOP`.

- `FUEL`/`ASTEROID`: calcula la velocidad deseada con distancia de frenado y se
  deja llevar por la inercia (`COAST_BAND`); solo empuja cuando la velocidad que
  le falta supera el umbral y está alineado (`THRUST_CONE`). Frente a meteoritos
  se detiene a `FIRE_DIST` y dispara con anticipación.
- `STOP`: se activa cuando el objetivo desaparece (destruido, recogido,
  caducado o fuera del mundo). Apunta en contra de la velocidad, modula el
  empuje (`BRAKE_TAPER = 45 u/s`) y se cancela al bajar de `STOP_SPEED =
  10 u/s` o tras `STOP_TIMEOUT = 5 s`. Si no hay combustible para frenar, el
  timeout evita quedarse bloqueado.
- `cancel()` (mando manual) desactiva el piloto **sin** frenar; la frenada solo
  ocurre al perder el objetivo.
- Al tomar un objetivo, `MyGame` pone el acelerador manual a 0; al terminar el
  piloto y no haber mando manual, lo vuelve a dejar a 0. Así no se reactiva el
  empuje residual en la dirección de frenada.
- Los bidones tienen `CAPTURE_PAD = 6 u` de margen de recogida y, cerca de
  ellos, el piloto fuerza `FUEL_MIN_APPROACH = 55 u/s` para no quedarse corto y
  tener que volver a acelerar.
- `startRun()` y `GameWorld.start()`/`restore()` cancelan el piloto para no
  arrastrar objetivos de una partida anterior.
- Mientras frena se dibuja un anillo cálido alrededor de la nave.

`SelfTest` comprueba la frenada tras destruir un meteorito a velocidad alta y
tras recoger un bidón.

## Daño y reparaciones

La nave no tiene vidas: se dañan sus ocho partes y cada una afecta al juego.
`Ship.part[]` guarda la integridad (0 = destruida):

| Parte | A 0 % |
|---|---|
| `HULL` (casco) | la nave explota y termina la partida |
| `ENGINE` (motor) | 10 % de empuje y de velocidad máxima |
| `TANK` (depósito) | 40 % de capacidad y fuga de combustible (2,2 u/s) |
| `CANNON` (cañón) | no dispara; con daño, menos daño y más recarga |
| `STEERING` (timón) | 20 % de velocidad de giro |
| `WING_LEFT` / `WING_RIGHT` | se pierde el ala; menos giro y velocidad, y la nave vira sola hacia ese lado |
| `LIFE_SUPPORT` (soporte) | el filtro de O2 cae al 20 % (se conecta en T-202) |

- Cada impacto siempre resta casco y además daña un sistema o un ala al azar.
  El daño escala con el tamaño del meteorito (14/24/36 de base).
- Las alas se dibujan con su integridad: al 100 % están completas, con daño se
  encogen y oscurecen, y a 0 % desaparecen y queda un muñón con humo y chispas
  (`GameWorld.emitWingDamage`). El ala perdida también reduce el giro, la
  velocidad máxima y añade un viraje automático hacia el lado dañado.
- La integridad máxima del casco es `100 + 25 × nivel de Blindaje`; los
  sistemas siempre tienen 100. El casco dañado no reduce el máximo, solo la
  integridad actual.
- `Ship.update()` aplica los multiplicadores, la capacidad efectiva del
  depósito y la fuga. `restore()` reconstruye la nave con sus partes.
- La tienda tiene dos pestañas cuando hay partida: `MEJORAS` y `REPARAR`. La
  segunda muestra una tarjeta por parte con una barra de integridad y un botón
  `REPARAR <coste>`; el coste es proporcional a la integridad que falta
  (`0,70` monedas por punto, mínimo 5). Al reparar el depósito se conserva el
  combustible que cabe en la capacidad recuperada.
- Las reparaciones y la recarga solo aparecen si hay una partida activa; las
  monedas de la partida se ingresan antes de pagar, igual que la recarga.
- El HUD sustituye los corazones por un indicador de casco con escudo y barra;
  los sistemas por debajo del 72 % generan avisos a la derecha y un aviso
  temporal `GOLPE EN <PARTE>`.

## Secuencia de muerte

- `updatePlaying()` detecta `world.isOver()` y llama a `triggerDeath()`.
- `triggerDeath()` guarda las monedas en `overCoins`, las ingresa en la cuenta,
  borra la partida guardada, pone la integridad del casco a 0, lanza
  `world.beginDeath()` y pasa a `DYING`.
- `triggerPilotDeath()` se ejecuta cuando el piloto llega a 0 HP o muere
  asfixiado. Cierra la partida sin explosión, deja el casco intacto y también
  guarda el mundo/nave legado.
- Antes de cerrar, `finishRun()` marca la nave como `legacyWreck`, guarda
  `legacy.json` y limpia la partida del personaje. El botón `NUEVO PERSONAJE`
  carga ese mundo y deja la nave anterior en el punto de muerte.
- Con el nuevo personaje a ≤100 u aparece `RECUPERAR NAVE`. Al recuperarla se
  obtiene un desguace de 10–90 créditos y `legacyWreck` pasa a `false`; el
  estado se guarda para no duplicar la nave.
- `recovery_test` es una herramienta de desarrollo, no una mecánica normal:
  guarda un punto seguro cada 10 s y permite `RECUPERAR (DEBUG)` tras morir.
  Sin el flag, la muerte es permanente.

## Carga y bodega

- `Commodity`: Hielo (0.80 UM, 1.5), Silicato (1.00, 2.0), Hierro (1.20, 3.0),
  Cobre (1.50, 5.0), Titanio (1.80, 12.0) y Oro (2.50, 30.0).
- `Cargo` es una entrada inmutable (mineral + cantidad) y calcula masa/valor.
- `CargoHold` mide la capacidad en UM (100 iniciales), suma la masa real,
  rechaza el exceso y expone añadir/quitar/consultar y entradas ordenadas.
- `GameData` crea la bodega inicial. `Ship` incorpora y expone su `CargoHold`;
  `GameSession.cargo()` delega en la nave. El guardado v2 conserva las entradas
  con masa y las partidas antiguas sin campo `cargo` cargan vacías.
  La minería llega en S4; la masa en movimiento, en T-303.
- `BODEGA` es la sexta mejora del loadout: +25 UM por nivel desde 100 base.
  `Ship.reset()`/`syncCargo()` aplican la capacidad y el HUD muestra
  `CARGA usado/capacidad UM` bajo el combustible.

## Asteroides y composición

- `AsteroidType` C/S/M: dureza 2/5/8, reserva 12–18 / 28–42 / 60–90 y
  composición mineral del plan maestro.
- Cada `Asteroid` guarda tipo, reserva y máximo; expone dureza y fracción por
  mineral. `takeReserve()` descuenta UM (la minería llega en T-402/T-403).
- La generación usa un `Random` sembrado por chunk; la semilla de cada roca
  deriva de posición, tamaño y tipo, así que el mismo mundo produce las mismas
  rocas.
- El tipo no lleva recompensa en monedas. La recompensa legacy de `Asteroid`
  se elimina en T-405.

## Minería láser y energía

- `EnergyCapacitor`: 100 de base, consumo 3/s minando y recarga 8/s tras 1 s.
- `MiningLaser`: potencia 6 + 1.5·nivel, penetración 3 + 1·nivel, rango 260 u
  y resistencia `clamp(1 − max(0, dureza − penetración)·0.10, 0.15, 1)`.
- La tasa es `potencia · integridadLaser · resistencia`; con equipo inicial:
  C 6 UM/s, S 4.8 y M 3.
- La parte `MINING_LASER` es la novena y su integridad multiplica la tasa.
- El láser consume energía, no combustible, y se detiene al agotar la roca.
  T-403 conecta la extracción con la bodega.

## Masa y movimiento

- `MassModel.load()` usa `usado/capacidad` (0–1) y aplica topes.
- A carga completa: aceleración ×0.75, velocidad ×0.88, giro ×0.90 y consumo
  ×1.35.
- `Ship.update()` aplica los cuatro factores; la masa del combustible no se
  incluye todavía.
- Depósito base 160, consumo base 10/s. Ida y vuelta de 800 u con carga completa
  usa ~112 y deja ~48 de margen.
- La recarga de combustible cuesta 1 crédito/unidad con mínimo 5; un bidón de
  40 sigue siendo útil (25 % del depósito base).
- `SaveCodec` v2 incluye la posición, rumbo, casco y partes de la nave legado,
  así que el resto persiste tras reiniciar.
- `GameWorld.beginDeath()` oculta la nave, limpia efectos y crea varias ondas y
  ráfagas; `updateDeath()` congela la simulación y solo avanza partículas,
  temblor y explosiones escalonadas.
- `updateDying()` avanza `deathTimer`; a los 2,45 s pasa a `OVER`. Durante
  `DYING` no se dibuja el HUD: queda el mundo congelado, el destello blanco, la
  viñeta roja y el fundido.
- La cámara hace zoom-out con `deathTimer`. El diálogo final usa `overCoins`, no
  `world.coinsEarned` (que ya vale 0 tras ingresar las monedas).
- `slow_death` escala el tiempo a 0,35× solo para capturas de diseño.

## Extensión futura: identidad, facciones y online

No hay red, multijugador, PvP ni facciones implementadas. La arquitectura solo
evita cerrar esa puerta:

- `PlayerProfile` separa `playerId` (cuenta/jugador) de `id` (personaje). Un
  jugador puede tener varios personajes sin compartir identidad.
- El guardado v2 incluye ambos identificadores. `SaveService` acepta un nombre
  de slot (`slot1`, `slot2`…) y deriva `<slot>.json` y `<slot>.json.bak`, así
  que varios personajes pueden coexistir sin mezclar archivos.
- `GameSession` es una instancia, no un singleton global. La sesión activa no
  asume un único jugador en la capa de datos.
- Límites actuales de la simulación local: un sector activo, sin autoridad de
  servidor, sin red, sin anti-cheat, sin facciones ni combate. El guardado es
  local y el jugador puede editarlo.
- Puntos de extensión previstos: ids de facción en entidades, transacciones
  autoritativas, registro de eventos y separación entre simulación y
  presentación. No se implementan ahora.
