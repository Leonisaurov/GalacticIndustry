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

## Estructura de la interfaz

Tras T-007, `MyGame` es un orquestador de flujo (estados, entrada, guardado y
telemetría) y el dibujo se reparte en módulos:

| Clase | Responsabilidad |
|---|---|
| `UiKit` | Paneles, botones, iconos, pips de nivel, barras y texto (con `GlyphLayout`). |
| `PlayHud` | Panel superior, combustible, energía, casco, supervivencia, avisos, joystick y acelerador. |
| `StationScreen` | Servicios de estación: descarga, venta, combustible, O2, reparaciones y mejoras. |
| `OverlayRenderer` | Menú principal, pausa, fin de partida y fundido de muerte. |
| `SpaceBackground` | Degradado, estrellas con parallax, nebulosas y planeta. |
| `HudView` | Estado efímero reutilizable que `MyGame` pasa al HUD sin asignar objetos por frame. |
| `MyGame` | Estados, entrada, sesión, guardado, telemetría y coordinación de los módulos. |

No se crean objetos en `render()`: `HudView`, `GlyphLayout` y los colores
estáticos se reutilizan.

## Estación nodriza

- `StationDef` define nombre, radio visual (200), radio de acople (260), zona
  segura (480) y servicios (reparación, combustible, O2, mejoras, mercado y
  almacén).
- `Station` instancia la nodriza en `(0, 480)` con un `CargoHold` de 2000 UM
  como almacén. Gira lentamente y se dibuja a gran escala con anillo, brazos,
  núcleo y bahía de acople.
- `GameWorld.generateChunk()` descarta cualquier asteroide dentro de la zona
  segura: `GameData.MOTHERSHIP.safeRadius`.
- El acople, los servicios y el almacén persistente llegan en T-502..T-505.
- Telemetría: `stationX`, `stationY`, `stationRadius`, `stationDistance`,
  `stationAsteroids`, `docked` y `canDock`.

## Acoplamiento

- `DockingSystem` exige estar dentro de `dockRadius` (260) y a menos de **30
  u/s** para mostrar `ACOPLAR`.
- Al acoplar: la nave se detiene, el piloto/láser/tractor se cancelan y el
  estado pasa a `DOCKED`, con una pantalla mínima de estación.
- `DESPEGAR` (o ESC) devuelve a `PLAYING` con velocidad cero.
- `Autopilot.STATION` vuela a la estación y frena a ~240 u del centro, dentro
  de la zona de acople y fuera del casco visual.
- Los servicios completos y la persistencia del acople quedan para T-503 y
  T-505.

## Servicios de estación

- `StationScreen` sustituye el panel mínimo de acople y ofrece dos pestañas:
  - **SERVICIOS**: descargar bodega al almacén, vender el almacén a precio base,
    repostar combustible, rellenar O2 y reparar todas las partes.
  - **MEJORAS**: las ocho mejoras del loadout.
- `StationStorage` mide la capacidad en UM. `depositAll()` mueve lo que cabe y
  deja el resto en la nave; `sellAll()` paga el precio base y vacía el almacén.
- Los créditos de la partida se ingresan al acoplar (`bankCoins`).
- Los precios usan las mismas fórmulas que la antigua tienda: combustible
  1 cr/UM (mín. 5), O2 0,5 cr/UM (mín. 5) y reparación 0,7 cr por punto.
- `DESPEGAR` cierra la pantalla y devuelve la nave al exterior.
- Persistencia (T-505/T-605): el guardado v2 incluye posición/nombre de
  estación, almacén, mercado (stock, objetivo, producción, consumo y spread) y
  el flag `docked`. Los guardados antiguos sin esos campos cargan con la
  nodriza por defecto, almacén vacío, mercado por defecto y sin acople.
- Al reanudar una partida acoplada, `CONTINUAR` abre directamente la estación.

## Rescate por combustible

- `RescueService` se activa cuando la nave no está acoplada, el piloto vive y
  queda combustible (≤0,5). No se ofrece dentro de la zona de acople: allí se
  puede entrar gratis.
- El jugador pide el rescate desde el HUD. Tarda **3 s** y después:
  - cobra **100 créditos** o todo el saldo si no llega;
  - descarta **25 %** de la carga (50 % si no pudo pagar);
  - remolca la nave a `dockRadius + 40` de la estación con **20 de combustible**;
  - cancela piloto, láser y tractor.
- No revive al piloto ni reinicia el mundo: si el piloto muere durante la
  espera, el rescate se cancela y la muerte sigue su curso.
- Se registran `rescue_request` y `rescue` en el log; telemetría
  `canRescue`, `rescueActive` y `rescueSeconds`.

## Universo y galaxias (datos)

- Jerarquía: `UniverseState` → `GalaxyDef` → `SystemDef` → `SectorDef`.
- `GalaxyDef`: id, nombre, semilla, paleta (`nebulaColor`/`starColor`) y sesgos
  de minerales por galaxia.
- `SystemDef`: id, galaxia, semilla y de **1 a 3 sectores**.
- `SectorDef`: id, sistema, semilla, límites (`12000 × 12000` por defecto),
  perfil de generación (`INICIAL`, `CINTURON`, `METALICO`, `HELADO`), radio
  seguro y estaciones.
- IDs y semillas son estables y se construyen en `GameData.newUniverse()`:
  - `milky-way` / `sol` / `sol-prime` (arranque, nodriza);
  - `andromeda` (metales y titanio);
  - `nebula-draco` (ficticia, hielo y silicato).
- `UniverseState.validate()` comprueba IDs únicos, semillas, paletas, sesgos,
  límites, perfiles, número de sectores y estaciones.
- El runtime finito y la carga/descarga por sector llegan en T-802.

## Sector finito (runtime)

- `GameWorld` recibe un `SectorDef` y usa sus límites: el mundo está centrado en
  (0,0) y la nave queda dentro de `±width/2`, `±height/2`; al tocar el borde se
  anula la velocidad hacia fuera y no hay wraparound.
- El borde se dibuja como cuatro franjas finas (`drawSectorBounds`), visible al
  acercarse.
- La generación usa el **perfil** del sector:
  - `HELADO`: mayoría de asteroides C y más bidones;
  - `METALICO`: más asteroides M y menos bidones;
  - `CINTURON`: mezcla con predominio S;
  - `INICIAL`: tabla base del juego.
- Solo se generan chunks alrededor de la nave (`RING = 2`), y una zona visitada
  queda marcada: **los recursos minados no reaparecen**. El coste no depende del
  tamaño total del sector.
- La carga/descarga efectiva entre sectores y el warp llegan en T-803.

## Persistencia y descarga de sectores

- `SectorRuntime` captura/restaura un estado resumido por sector:
  - IDs de chunks ya generados;
  - asteroides activos con posición, velocidad, tipo, reserva y semilla;
  - bidones con su vida restante.
- **No se guardan los chunks no visitados**: al volver se regeneran desde la
  semilla del sector. El guardado no crece con el tamaño total.
- `GameSession.captureCurrentSector()` guarda el sector actual en
  `UniverseState` antes de salir; `SectorRuntime.restore()` lo reconstruye sin
  duplicar minerales y sin regenerar las rocas ya minadas.
- El guardado v2 escribe `sectorSnapshots` dentro del sector; un guardado sin
  ese campo sigue siendo válido y carga sin snapshots.
- Los snapshots se validan con `SaveCodec.validate` y se restauran en
  `UniverseState` al cargar.

## Streaming de sectores y regiones

- `SectorStreamer` mantiene cargado el detalle del sector activo y de los que
  están a **≤2,5 unidades** del mapa de universo (radio de carga).
- Los sectores lejanos se **descargan**: solo queda su snapshot en
  `UniverseState`; al volver se recargan con `SectorStreamer.load()`.
- `SectorDef` incluye posición abstracta de mapa (`mapX`, `mapY`) para medir
  distancias entre sectores, sistemas y galaxias.
- El coste depende del número de sectores dentro del radio, **no** del tamaño
  total del universo: un universo de prueba con 200 sectores solo carga 1.
- El jugador no ve cortes porque el sector activo sigue siendo el mismo runtime;
  el cambio efectivo de sector/usuario llega con T-804.
- La simulación lejana sigue siendo agregada (mercados/agentes cada 2–10 s);
  no se mantiene detalle de asteroides fuera de carga.

## Viaje warp y TravelService

- **Sin puertas obligatorias**: `TravelService` permite viajar a cualquier
  sector del universo con motor warp.
- Coste:
  - `combustible = ceil(distancia · 2 / (1 + 0,15 · nivelMotor))`;
  - `energía = ceil(distancia · 4 / (1 + 0,15 · nivelMotor))`;
  - mínimo 2 de combustible y 4 de energía.
- Requiere **nivel de motor ≥ 1**; sin él, el viaje se rechaza.
- `JumpGate` es un atajo **opcional**: si conecta origen y destino reduce el
  coste a la mitad; sin puerta el viaje sigue funcionando.
- Al viajar:
  - se captura el sector de origen como snapshot;
  - se descarga/carga el destino con `SectorStreamer`;
  - la nave conserva carga, combustible restante, daños y energía;
  - se guarda la partida.
- El sector actual se persiste; al cargar se restaura con `switchSector`.
- `T`/`Y` y dos zonas invisibles de prueba permiten viajar y volver en
  `warp_test`.

## Galaxias: contenido diferenciado

- Cada galaxia define su propio **contexto económico** en `GalaxyDef`:
  - objetivos de stock por mineral;
  - fracción inicial y spread del mercado.
- La **Vía Láctea** es la galaxia de arranque, equilibrada.
- **Andrómeda** es rica en metales: más objetivo de hierro, cobre, titanio y oro,
  spread del 10 %.
- **Nebulosa Draco** (ficticia) es helada: mucho hielo/silicato, poco oro y
  spread del 6 %.
- Cada galaxia tiene su **nave nodriza** en el sector principal:
  - `NODRIZA`, `NODRIZA ANDROMEDA` y `NODRIZA DRACO`, con tamaño y servicios
    propios.
- `GameWorld` crea la estación y el mercado a partir del `SectorDef` y la
  `GalaxyDef` del sector, así que al viajar cambian nodriza, precios y stock.
- Añadir una galaxia es añadir datos en `GameData.newUniverse()`.

## Mapa de sectores (MapScreen)

- Se abre con el botón `MAPA` del HUD en `PLAYING` y se cierra con `VOLVER`.
- Muestra la posición actual (`galaxia / sistema / sector`) y todos los
  sectores del universo en filas.
- **Descubrimiento**:
  - al empezar se descubren todos los sectores del sistema inicial;
  - se añade un faro en la primera estación de cada otra galaxia;
  - al llegar a un sistema se descubren todos sus sectores.
- Los sectores no descubiertos aparecen como `???` / `SIN DATOS`, no revelan
  distancia ni coste y no se pueden seleccionar ni viajar a ellos.
- Para cada destino descubierto se muestra distancia, coste de combustible (`C`)
  y energía (`E`), calculados por `TravelService`.
- `VIAJAR` ejecuta el warp y cierra el mapa; si falta motor, combustible o
  energía, muestra el error sin moverse.
- Se dibuja con formas y texto, funciona en vertical y no usa red ni imágenes.

## Estación personal (datos)

- `PlayerStation` es una plataforma **vacía y acoplable**, distinta de la nave
  nodriza: sin almacén, reparación, refinería ni mercado de serie.
- Tiene **6 ranuras** y un `StationDef` propio (`PLATAFORMA`, radio 60 u).
- `ModuleDef` define el catálogo construible:
  - `ALMACEN` (STORAGE), `TALLER` (REPAIR), `REFINERIA` (REFINERY) y
    `MERCADO` (MARKET);
  - cada módulo tiene coste en créditos, materiales, requisito previo y
    servicio.
- Los servicios se derivan de los módulos instalados (`serviceMask()`); una
  plataforma vacía devuelve **0**.
- `install()` valida ranura, plaza libre, plataforma colocada y requisito. La
  compra/construcción con créditos y materiales llega en T-903.
- `ConstructionService` compra y coloca el núcleo:
  - **300 créditos** y **25 Hierro + 10 Cobre + 20 Silicato**;
  - rechaza fuera del sector, a menos de `dockRadius + 200` de la nodriza o si
    ya existe una base;
  - la plataforma nace vacía, sin servicios.
- La plataforma se dibuja como un cuadrado con borde luminoso y zona segura
  propia; los asteroides no se generan dentro.
- La posición y el estado de la base se guardan en el v2 (`playerStation`).
- El acople lógico está disponible (`PlayerStation.canDock`); la pantalla de
  gestión modular llega en T-904.

### Sistema modular

- `StationModule` representa un módulo instalado con **nivel 1–3**.
- `PlayerStation.build()` es la única vía para obtener un servicio:
  - valida ranura, módulo previo, créditos y materiales;
  - descuenta de forma atómica;
  - crea el `StationModule` en la ranura.
- `upgrade()` amplía un módulo existente con coste creciente; no cambia el tipo
  de servicio.
- `serviceMask()` se deriva de los módulos; una plataforma vacía devuelve 0.
- Añadir un módulo nuevo es añadir un `ModuleDef`, sin tocar el núcleo.

### Almacén y taller (T-907)

- `ALMACEN` aporta **200 UM por nivel** de capacidad; sin módulo la capacidad es
  **0** y `depositAll()` no mueve nada.
- `TALLER` habilita `repairAll()`: coste `max(5, 0,70 · daño)` por parte, cobra
  créditos y repara casco/sistemas.
- El almacén de la estación es un `StationStorage` con `CargoHold` propio; la
  capacidad se recalcula al construir/ampliar/restaurar módulos.
- Contenido del almacén y módulos se guardan en el v2.

### Pantalla de gestión (T-904)

- Al acoplarse a la plataforma, `DockingSystem` marca `dockedAtPlayer` y
  `MyGame` abre `StationManagementScreen` en vez de la pantalla de la nodriza.
- La pantalla muestra:
  - servicios activos y ocupación del almacén;
  - 6 ranuras con módulo y nivel (o vacías);
  - catálogo de módulos, `CONSTRUIR`, `MEJORAR`, `DESCARGAR`, `REPARAR` y
    `DESPEGAR`.
- Construir/mejorar cobra créditos y materiales de la bodega; descargar mueve
  carga al almacén; reparar exige TALLER.
- Todo se dibuja con formas y texto, en vertical, sin menús anidados.
- La UI de la nodriza sigue siendo independiente.

### Persistencia de la estación

- El guardado v2 incluye `playerStation`:
  - posición, sector, estado construido;
  - módulos instalados por ranura, tipo y nivel.
- Al cargar se restauran los módulos y sus niveles; `serviceMask()` se vuelve a
  derivar, no se guarda como verdad paralela.
- Un guardado sin `playerStation` sigue siendo válido: la plataforma queda sin
  construir.
- No se duplican créditos, materiales ni módulos al cargar.

## Pilotos autónomos (datos)

- `PilotAgent` resume la nave de un agente: id, nombre, rol (`MINER`/`HAULER`),
  capacidad de carga, combustible, velocidad, tasa de minado, estado, objetivo
  y bodega propia.
- Estados previstos: `DOCKED`, `TRAVEL`, `MINE`, `RETURN`, `SELL`, `REFUEL`,
  `IDLE`.
- `AgentRoster` crea **8 mineros** deterministas por defecto y admite hasta
  **24**. La plantilla es configurable.
- Los agentes viven en el sector (`GameWorld.agents`), se guardan en v2 y se
  restauran; un guardado sin agentes recibe la plantilla inicial.
- `AgentBrain` implementa el ciclo `DOCKED → TRAVEL → MINE → RETURN → SELL →
  REFUEL → DOCKED`. El viaje es tiempo abstracto (2 s), sin pathfinding.
- La decisión es determinista por utilidad:
  `valorCarga · fill − combustible − tiempo · oportunidad − riesgo`.
  Un minero básico solo considera Hielo, Silicato, Hierro y Cobre.
- Si la mejor utilidad no es positiva, pasa a `IDLE` y espera.
- `Market.sellTo()` y `Market.buyTo()` son las transacciones de agentes:
  - vender sube stock, baja el precio y paga créditos solo si hay carga;
  - comprar baja stock, sube el precio y exige créditos y capacidad.
- Al vender, el stock de la estación sube y el precio baja; el agente cobra y
  reposta con sus créditos (sin saldo negativo).
- El tick/LOD de agentes llega en T-703; el render limitado, en T-706.

## Tráfico cosmético

- `StationTraffic` dibuja **3 naves** pequeñas entrando/saliendo de la nodriza.
- Solo se actualizan y dibujan si el jugador está a **≤1400 u** de la estación.
- No colisionan, no entran en asteroides/misiles ni tocan el mercado.
- Usan el mismo `Ship.drawBody` y dejan una estela ligera en `Effects`.
- Se reinician al empezar una partida y no se guardan.

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

Modos: `NONE`, `FUEL`, `ASTEROID`, `MINE` y `STOP`.

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
- `MINE` aproxima a la roca, aparca a 180 u y mantiene el láser si hay
  energía, parte operativa y espacio en bodega. No dispara misiles; termina al
  agotar la roca o llenar la bodega.

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
- Mejora `LASER` (séptima): +1 nivel sube potencia (+1.5), penetración (+1) y
  capacitor (+25, hasta 225). `Ship.syncLaser()` conserva la carga relativa.
- HUD contextual: la barra de energía y el porcentaje solo aparecen con el haz
  activo o mientras el capacitor no está lleno. El icono y el color cambian a
  ámbar/rojo con poca energía.
- Cada tipo de roca tiene haz e impacto propios: C cian, S azul claro, M ámbar.
  El impacto emite chispas del color del mineral; todo se dibuja sin crear
  objetos en `render()` (colores estáticos y solo floats).

## Extracción y agotamiento

- Cada roca precalcula sus unidades por mineral con el método de mayor resto.
- `MiningLaser.tick()` reparte el lote exacto y `CargoHold.addAll()` lo acepta
  solo si toda la masa cabe; si no, el láser se detiene sin perder reserva.
- Las rocas agotadas se eliminan del mundo sin explosión ni monedas.
- Reserva, carga y chunks persisten; una roca agotada no reaparece al cargar.

## Fragmentos por misiles

- Al destruir una roca, `FRAGMENT_SHARE` (0.60) de su reserva se reparte en
  fragmentos físicos y `FRAGMENT_LOSS` (0.25) se pierde; el resto queda en
  escombros que heredan el mineral restante.
- Cada mineral se parte en uno o dos fragmentos con posición y velocidad
  propias; no entran en la bodega hasta que la nave los toca.
- Si la bodega está llena, el fragmento permanece en el espacio. Su vida es de
  90 s y desaparecen si se alejan demasiado.
- `fragmentShare` y `fragmentLoss` son ajustables por mundo para balance.
- Los fragmentos se guardan en v2 y en el formato legacy con posición,
  velocidad, mineral, cantidad y vida restante; un guardado sin ellos carga
  vacío.

## Rayo tractor

- `TractorBeam` atrae automáticamente hasta `2 + nivel` fragmentos dentro de su
  alcance; no necesita botón.
- Alcance `140 + 25·nivel`, velocidad `120 + 30·nivel` y tope de arrastre 260.
- Consume **1,5 energía/s** del capacitor mientras atrae al menos un fragmento.
  Comparte capacitor con el láser, pero solo uno de los dos drena a la vez: si
  el láser está minando, tiene prioridad.
- La captura final la hace la nave al tocar el fragmento; el tractor solo lo
  acerca. Si no se recupera, sigue perdiéndose por tiempo.
- Mejora `TRACTOR`: alcance, velocidad y fragmentos simultáneos.
- HUD: `ENERGIA xx%  TRACTOR n` mientras hay fragmentos atraídos; haces
  verde-cian distintos del láser.
- Telemetría: `tractorActive`, `tractorTargets` y `tractorLevel`.

## Mercado y precios

- Cada `Station` tiene un `Market` con stock y objetivo por mineral.
- `PriceModel` aplica:
  - `factor = clamp(sqrt(objetivo / max(stock,1)), 0,5, 2,0)`;
  - `venta = base · factor · (1 − spread)`;
  - `compra = base · factor · (1 + spread)`.
- Spread inicial del **8 %**; configurable por mercado con `setSpread`.
- Objetivos iniciales: Hielo/Silicato 200, Hierro 150, Cobre 100, Titanio 60
  y Oro 40. El stock arranca al 80 % del objetivo.
- `addStock()` mueve el stock sin bajar de cero. Las transacciones del jugador
  y la reacción de precios llegan en T-602.
- No hay producción/consumo ni NPC todavía (T-604/T-7xx).

## Comercio

- `TradeService.buy()` compra del mercado a `market.buyPrice()`:
  - valida cantidad, stock, créditos y capacidad de bodega;
  - cobra al jugador, mete la carga y **baja el stock** (sube el precio).
- `TradeService.sell()` vende carga a `market.sellPrice()`:
  - exige tener la cantidad;
  - retira la carga, paga créditos y **sube el stock** (baja el precio).
- Cada operación devuelve un `Trade` con lado, mineral, cantidad, precio unitario
  y total; el llamador puede registrarlo con `Listener` (`Trade.toJson()`).
- `MarketScreen` (T-603) muestra los seis minerales con stock/objetivo y precios
  de compra/venta. Se selecciona un mineral, se ajusta la cantidad con
  `-10/-1/0/+1/+10`, `TODO`/`NADA` y se compra o vende con confirmación.
- La pantalla se abre desde la pestaña `MERCADO` de la estación y se vuelve con
  `VOLVER`. Refleja créditos y masa de bodega en la cabecera.
- El mercado no se persiste todavía; T-605 lo guardará.

## Simulación económica

- `SimClock` define tres niveles: **cerca 2 s**, **lejos 5 s** y **offline
  60 s**. Acumula tiempo y solo dispara ticks completos.
- `EconomySim` usa cerca para **agentes** (cada 2 s) y 10 s para **mercados**;
  nunca se ejecuta trabajo por frame. `updateOffline()` limita a 12 h.
- El guardado escribe `lastSimTime` (epoch). Al cargar, si han pasado ≥60 s,
  se simulan pasos de 60 s hasta un máximo de 12 h; sin red ni hilos.
- Con 24 agentes el tick sigue siendo ligero (bench en `SelfTest`).
- `EconomySim` tickea cada **10 s** de simulación, nunca por frame.
- Cada tick: `stock += producción − consumo + round((objetivo − stock) · 0,02)`,
  acotado a `[0, objetivo · 3]`.
- La reversión al objetivo evita precios desbocados y oscilaciones infinitas.
- La nodriza consume Hielo (soporte vital), Hierro y Cobre (mantenimiento), y
  produce Silicato. El resto queda estable.
- Producción y consumo son configurables por mercado con `setProduction()` y
  `setConsumption()`.
- Los mercados se actualizan en `GameWorld.update()`; el acumulador se reinicia
  al empezar una partida.

## Venta en estación

- Destruir o minar no concede monedas; el valor está en el mineral de la bodega.
- La estación descarga el mineral al almacén y lo vende a precio base desde
  `StationScreen` (`StationStorage.sellAll()`).
- La antigua fundición de la tienda de pausa se retiró en T-504; la economía
  principal vive en la estación. El mercado con precios variables llega en S6.

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
