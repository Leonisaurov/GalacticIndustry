# Índice de sistemas — GalacticIndustry / Asteroides

Mapa de navegación entre **mecánicas, código, diseño, decisiones, tareas y
pruebas**. No es un plan maestro ni una segunda fuente de verdad: es una guía
para saber dónde mirar antes de leer.

- **Código fuente:** `src/com/example/codegame/`
- **Estado operativo:** `docs/IMPLEMENTATION_TRACKER.json` (fuente canónica)
- **Resumen generado:** `docs/STATUS.md`
- **Especificación:** `docs/MASTER_PLAN.md`
- **Decisiones:** `docs/DECISIONS.md`
- **Pruebas:** `docs/pruebas.md` y `tools/tests/`

Salvo que se indique otra ruta, los archivos `.java` de este índice viven en
`src/com/example/codegame/`.

**Estados:** `Implementado` = funciona y tiene pruebas; `Parcial` = funciona con
alcance limitado; `Planificado` = diseñado sin código; `Aplazado` = fuera del
alcance actual.

## Tabla rápida

| Sistema | Estado | Código principal | Tareas | Pruebas |
|---|---|---|---|---|
| Continuidad y panel | Implementado | `tools/sync-dashboard.py`, `docs/dashboard/*` | T-003, T-006, T-007, UPD-001 | `sync-dashboard.py --check` |
| Piloto y supervivencia | Implementado | `Pilot.java`, `LifeSupport.java`, `GameSession.java` | T-102, T-201…T-210 | `01_logic`, `10_survival` |
| Nave, componentes y carga | Implementado | `Ship.java`, `ShipLoadout.java`, `Upgrades.java`, `CargoHold.java` | T-103, T-301…T-306, T-1101, T-1109 | `01_logic`, `11_cargo` |
| Movimiento y combustible | Implementado | `Ship.java`, `MassModel.java`, `FuelPickup.java` | T-303, T-304 | `01_logic`, `02_navigation`, `03_controls` |
| Daños y reparaciones | Implementado | `Ship.java`, `StationScreen.java`, `Upgrades.java` | T-205, T-206, T-504 | `09_wing_damage`, `13_station` |
| Inventario y almacenamiento | Implementado | `Cargo.java`, `CargoHold.java`, `StationStorage.java` | T-301, T-302, T-505 | `01_logic`, `11_cargo` |
| Minería láser y recursos | Implementado | `MiningLaser.java`, `EnergyCapacitor.java`, `Asteroid.java` | T-401…T-410 | `01_logic`, `12_mining` |
| Misiles y fragmentos | Implementado | `Missile.java`, `OreFragment.java`, `GameWorld.java` | T-406, T-409 | `01_logic`, `12_mining` |
| Rayo tractor | Implementado | `TractorBeam.java`, `EnergyCapacitor.java` | T-410 | `13_tractor`, `12_mining` |
| Piloto automático | Implementado | `Autopilot.java`, `MyGame.java`, `ShipModuleDef.java` | T-404, T-502, T-1109 | `04_autopilot`, `12_mining` |
| Estaciones y acoplamiento | Implementado | `Station.java`, `DockingSystem.java`, `StationScreen.java` | T-501…T-507 | `13_station`, `02_navigation` |
| Comercio y economía | Implementado | `Market.java`, `PriceModel.java`, `TradeService.java`, `MarketScreen.java` | T-601…T-606 | `01_logic`, `14_market` |
| Contratos y misiones | Implementado (básico) | `Mission.java`, `MissionService.java` | T-1102 | `01_logic` |
| Pilotos autónomos | Parcial | `PilotAgent.java`, `AgentRoster.java`, `AgentBrain.java`, `EconomySim.java` | T-701…T-707 | `15_agents` |
| Universo y viaje warp | Implementado | `UniverseState.java`, `TravelService.java`, `SectorStreamer.java`, `MapScreen.java` | T-801…T-808 | `16_universe`, `17_warp`, `18_map` |
| Estación personal | Implementado | `PlayerStation.java`, `StationModule.java`, `ConstructionService.java` | T-901…T-908 | `17_player_station` |
| Interiores y avatar | Parcial | `InteriorScene.java`, `Avatar.java`, `InteriorRenderer.java` | T-1001…T-1007 | `18_interiors` |
| Interfaz, HUD y render | Implementado | `MyGame.java`, `UiKit.java`, `PlayHud.java`, `OverlayRenderer.java` | T-007, T-1110 | `06_visual_menu`, `22_ui_polish` |
| Audio y música | Implementado (local) | `AudioService.java` | T-1104, T-1108 | `19_audio`, `21_music` |
| Persistencia y guardado | Implementado | `SaveService.java`, `SaveCodec.java`, `GameSession.java` | T-104, T-505, T-605, T-803, T-905 | `01_logic`, `13_station` |
| Accesibilidad y localización | Parcial | `MyGame.java`, `UiKit.java` | T-1106 | `20_accessibility`, `22_ui_polish` |
| Facciones, combate y online | Aplazado | — | T-1103 | — |

---

## Continuidad, documentación y panel

**Estado:** Implementado. Incluye el sistema de actualizaciones `UPD-XXX`
documentado en `docs/updates/README.md`.

**Código:** `tools/sync-dashboard.py` (valida, sincroniza el panel y genera
`docs/STATUS.md`); `docs/dashboard/index.html`,
`docs/dashboard/dashboard.css`, `docs/dashboard/dashboard.js` (panel estático
para `file://`).
**Documentos:** `docs/IMPLEMENTATION_TRACKER.json` (fuente canónica),
`docs/IMPLEMENTATION_LOG.md`, `docs/DECISIONS.md`, `docs/MASTER_PLAN.md`,
`docs/updates/README.md`, `docs/updates/UPD-XXX.md`.
**Diseño:** `docs/MASTER_PLAN.md` §18; `docs/updates/README.md`;
`docs/dashboard/README.md`. **Decisiones:** D-013.
**Tareas:** T-003, T-006, T-007, UPD-001-T01…T06. **Pruebas:**
`python3 tools/sync-dashboard.py --check`; apertura del panel con `file://`;
generación de propuestas y expedientes `.md`/`.txt` desde el navegador.
**Problemas:** el panel es de solo lectura; no escribe en el proyecto. Los
borradores de `localStorage` no son estado canónico.

## Piloto y supervivencia

**Estado:** Implementado.

**Código:** `Pilot.java` (salud, O2 y estados), `LifeSupport.java` (filtro,
fugas y brecha), `GameSession.java` (composición y `LifeSupport.update()`),
`GameWorld.java` (gas y daño de casco), `MyGame.java` (HUD, alertas y muerte).
**Diseño:** `docs/MASTER_PLAN.md` §4.1, §4.5 y §7; `docs/arquitectura.md`
§Clases y §Daño y reparaciones; `docs/pruebas.md` §Balance de supervivencia.
**Decisiones:** D-001. **Tareas:** T-102, T-201…T-210. **Pruebas:** `01_logic`,
`10_survival`, `05_death`, `08_visual_death`.
**Problemas:** los comentarios de `Pilot`, `LifeSupport` y `GameSession`
conservan referencias a etapas antiguas ("se conectará en T-202/T-203", "no
consume O2"). El comportamiento real está en `GameSession.update()` y
`LifeSupport.update()`.

## Nave, componentes y carga

**Estado:** Implementado.

**Código:** `Ship.java` (movimiento, partes, combustible y disparo),
`ShipLoadout.java` (niveles y ranuras), `Upgrades.java` (tienda y fórmulas),
`ShipClassDef.java` (clases), `ShipModuleDef.java` (módulos con efecto real),
`CargoHold.java` (capacidad en masa), `Commodity.java`/`Cargo.java` (minerales).
**Diseño:** `docs/MASTER_PLAN.md` §4.2–4.4 y §6.4; `docs/arquitectura.md`
§Clases de nave (T-1101), §Módulos de nave (T-1109) y §Carga y bodega.
**Decisiones:** D-010. **Tareas:** T-103, T-301…T-306, T-1101, T-1109.
**Pruebas:** `01_logic`, `11_cargo`, `12_mining`.
**Problemas:** el módulo `autopilot` se exige para seleccionar blancos
(`MyGame.pickTarget`), no para volar manualmente.

## Movimiento, propulsión y combustible

**Estado:** Implementado.

**Código:** `Ship.java` (giro, inercia y consumo), `MassModel.java` (efecto de
la carga), `FuelPickup.java` (bidones), `Autopilot.java` (rumbo y frenada),
`Controls.java` (entrada de un fotograma).
**Diseño:** `docs/MASTER_PLAN.md` §4.2 y §13.1; `docs/arquitectura.md` §Masa y
movimiento y §Piloto automático. **Decisiones:** ninguna específica.
**Tareas:** T-303, T-304, T-404, T-502. **Pruebas:** `01_logic`,
`02_navigation`, `03_controls`, `04_autopilot`.
**Problemas:** acelerar y disparar consumen combustible; cualquier cambio de
economía debe actualizar `SelfTest` y la documentación.

## Daños y reparaciones

**Estado:** Implementado.

**Código:** `Ship.java` (integridad de casco, motor, depósito, cañón, timón,
alas, soporte vital y láser), `GameWorld.java` (impactos y muerte),
`StationScreen.java` (reparación con monedas), `Upgrades.java` (blindaje).
**Diseño:** `docs/MASTER_PLAN.md` §4.3 y §7.3; `docs/arquitectura.md` §Daño y
reparaciones y §Secuencia de muerte. **Decisiones:** D-001, D-002.
**Tareas:** T-205, T-206, T-504. **Pruebas:** `09_wing_damage`, `05_death`,
`13_station`. **Problemas:** no hay vidas; el casco a 0 termina la partida.

## Inventario y almacenamiento

**Estado:** Implementado.

**Código:** `CargoHold.java` (bodega por masa), `StationStorage.java` (almacén),
`Cargo.java`/`Commodity.java` (entradas y minerales), `StationScreen.java`
(descarga y venta), `GameSession.java` (persistencia).
**Diseño:** `docs/MASTER_PLAN.md` §4.4 y §6.4; `docs/arquitectura.md` §Carga y
bodega. **Decisiones:** ninguna específica. **Tareas:** T-301, T-302, T-505,
T-605, T-905. **Pruebas:** `01_logic`, `11_cargo`, `13_station`, `14_market`.
**Problemas:** la capacidad se mide en masa (UM), no en unidades.

## Minería láser y recursos

**Estado:** Implementado.

**Código:** `MiningLaser.java` (extracción, potencia y penetración),
`EnergyCapacitor.java` (energía compartida), `Asteroid.java` (reserva y
agotamiento), `AsteroidType.java` (tipos C/S/M y composición), `GameWorld.java`
(objetivo, extracción y bodega), `Upgrades.java` (mejoras).
**Diseño:** `docs/MASTER_PLAN.md` §6.1–6.3 y §13.3; `docs/arquitectura.md`
§Asteroides y composición, §Minería láser y energía y §Extracción y agotamiento;
`docs/pruebas.md` §Gate de la Etapa 4. **Decisiones:** D-003.
**Tareas:** T-401…T-410. **Pruebas:** `01_logic`, `12_mining`, `04_autopilot`.
**Problemas:** el láser no consume combustible; comparte capacitor con el
tractor; el mineral no entra en la bodega hasta la recogida.

## Misiles y fragmentos

**Estado:** Implementado.

**Código:** `Missile.java` (proyectil, vida y daño), `OreFragment.java`
(fragmento con vida limitada), `GameWorld.java` (impacto, fragmentación y
dispersión), `Asteroid.java` (reserva convertida en fragmentos).
**Diseño:** `docs/MASTER_PLAN.md` §6.3.1; `docs/arquitectura.md` §Fragmentos por
misiles. **Decisiones:** D-003. **Tareas:** T-406, T-409.
**Pruebas:** `01_logic`, `12_mining`.
**Problemas:** los fragmentos se pierden si no se recogen a tiempo; el rayo
tractor facilita la captura.

## Rayo tractor

**Estado:** Implementado.

**Código:** `TractorBeam.java` (alcance, velocidad y fragmentos simultáneos),
`EnergyCapacitor.java` (consumo), `Ship.java` (integración), `GameWorld.java`
(actualización y recogida).
**Diseño:** `docs/MASTER_PLAN.md` §6.3.1; `docs/arquitectura.md` §Rayo tractor.
**Decisiones:** D-003. **Tareas:** T-410. **Pruebas:** `01_logic`, `13_tractor`,
`12_mining`. **Problemas:** comparte capacitor con el láser; si el jugador no se
acerca, los fragmentos siguen perdiéndose por tiempo.

## Piloto automático

**Estado:** Implementado.

**Código:** `Autopilot.java` (modos `FUEL`, `ASTEROID`, `STOP`, `MINE`,
`STATION`; frenada y cancelación), `MyGame.java` (`pickTarget` y entrada táctil),
`ShipModuleDef.java` (módulo `AUTOPILOT`), `ShipLoadout.java` (ranura).
**Diseño:** `docs/MASTER_PLAN.md` §4.6 y §12; `docs/arquitectura.md` §Piloto
automático; `docs/pruebas.md` §Extras de arranque. **Decisiones:** D-010.
**Tareas:** T-404, T-502, T-1109. **Pruebas:** `04_autopilot`, `12_mining`,
`13_station`. **Problemas:** `MyGame.pickTarget` exige el módulo `AUTOPILOT`
para elegir estación, asteroide o bidón; sin él, el toque en el espacio libre
dispara. El piloto frena (`STOP`) al perder el objetivo.

## Estaciones y acoplamiento

**Estado:** Implementado.

**Código:** `Station.java` (nave nodriza, posición, almacén y misiones),
`StationDef.java` (datos y servicios), `DockingSystem.java` (proximidad,
velocidad y `DOCKED`), `StationScreen.java` (servicios), `RescueService.java`
(rescate por combustible), `StationTraffic.java` (tráfico cosmético).
**Diseño:** `docs/MASTER_PLAN.md` §4.7 y §8; `docs/arquitectura.md` §Estación
nodriza, §Acoplamiento, §Servicios de estación y §Rescate por combustible.
**Decisiones:** D-001, D-002. **Tareas:** T-501…T-507. **Pruebas:** `13_station`,
`02_navigation`, `03_controls`. **Problemas:** la tienda económica de la pausa se
retiró en T-504; los servicios viven en la estación.

## Comercio y economía

**Estado:** Implementado.

**Código:** `Market.java` (stock, objetivo y precios), `PriceModel.java`
(fórmula y spread), `TradeService.java` (compraventa validada),
`MarketScreen.java` (interfaz), `EconomySim.java` (producción y deriva),
`StationStorage.java` (almacén y venta).
**Diseño:** `docs/MASTER_PLAN.md` §9 y §13.4; `docs/arquitectura.md` §Mercado y
precios, §Comercio, §Simulación económica y §Venta en estación;
`docs/pruebas.md` §Gate de la Etapa 6. **Decisiones:** ninguna específica.
**Tareas:** T-601…T-606. **Pruebas:** `01_logic`, `14_market`.
**Problemas:** el mercado es local a cada estación; no hay comercio entre
jugadores ni online.

## Contratos y misiones

**Estado:** Implementado (básico: entregar mineral antes de un plazo).

**Código:** `Mission.java` (contrato y estado), `MissionService.java` (ofertas
deterministas, plazos y entrega), `Station.java` (servicio por estación),
`MyGame.java` (aceptación y seguimiento).
**Diseño:** `docs/arquitectura.md` §Contratos de estación (T-1102);
`docs/MASTER_PLAN.md` §8.3. **Decisiones:** ninguna específica.
**Tareas:** T-1102. **Pruebas:** `01_logic`, `13_station`, `14_market`.
**Problemas:** no hay facciones, reputación ni cadenas narrativas (D-012
aplazada).

## Pilotos autónomos

**Estado:** Parcial: simulación económica abstracta, sin posición 2D ni render.

**Código:** `PilotAgent.java` (identidad, rol, carga y estado), `AgentRoster.java`
(plantilla de 8, máximo 24), `AgentBrain.java` (estados y utilidad),
`EconomySim.java` (tick, LOD y transacciones), `SimClock.java` (reloj).
**Diseño:** `docs/MASTER_PLAN.md` §10 y §13.5; `docs/arquitectura.md` §Pilotos
autónomos (datos) y §Tráfico cosmético; `docs/pruebas.md` §Gate de la Etapa 7.
**Decisiones:** D-007. **Tareas:** T-701…T-707. **Pruebas:** `01_logic`,
`15_agents`. **Problemas:** los agentes no tienen posición 2D ni se renderizan;
la simulación es por tiempo, no pathfinding.

## Universo, galaxias y viaje warp

**Estado:** Implementado.

**Código:** `UniverseState.java` (universo → galaxia → sistema → sector),
`GalaxyDef.java`/`SystemDef.java`/`SectorDef.java` (datos), `SectorRuntime.java`
(snapshot), `SectorStreamer.java` (carga/descarga), `TravelService.java` (warp y
coste), `JumpGate.java` (puerta opcional), `MapScreen.java` (mapa y destinos).
**Diseño:** `docs/MASTER_PLAN.md` §5 y §11.3; `docs/arquitectura.md` §Universo y
galaxias (datos), §Sector finito (runtime), §Streaming de sectores y regiones,
§Viaje warp y TravelService y §Mapa de sectores. **Decisiones:** D-004, D-008,
D-011. **Tareas:** T-801…T-808. **Pruebas:** `01_logic`, `16_universe`,
`17_warp`, `18_map`. **Problemas:** los sectores no visitados no se guardan en
detalle; solo se conserva su snapshot resumido.

## Estación personal y módulos

**Estado:** Implementado.

**Código:** `PlayerStation.java` (plataforma vacía y servicios derivados),
`ModuleDef.java` (catálogo construible), `StationModule.java` (módulo y nivel),
`ConstructionService.java` (coste y colocación), `StationManagementScreen.java`
(gestión), `StationStorage.java`/`Market.java` (servicios).
**Diseño:** `docs/MASTER_PLAN.md` §8.5; `docs/arquitectura.md` §Estación personal
(datos), §Sistema modular, §Almacén y taller (T-907), §Refinería y mercado
(T-908) y §Pantalla de gestión (T-904). **Decisiones:** D-005.
**Tareas:** T-901…T-908. **Pruebas:** `01_logic`, `17_player_station`,
`14_market`. **Problemas:** la plataforma nace vacía; sin módulos no hay
almacén, reparación, refinería ni mercado.

## Interiores y avatar

**Estado:** Parcial: interiores de la estación personal; naves y estaciones no
propias quedan aplazadas (T-1007).

**Código:** `InteriorScene.java` (salas, puertas e interactuables), `Room.java`,
`Door.java`, `Interactable.java` (geometría y puntos), `Avatar.java` (personaje,
colisión y salas), `InteriorInput.java` (entrada), `InteriorRenderer.java`
(dibujo cenital).
**Diseño:** `docs/MASTER_PLAN.md` §8.5; `docs/arquitectura.md` §Marco de
interiores (T-1001), §Avatar y movimiento interior (T-1002), §Render de
interiores (T-1003), §Interacción por proximidad (T-1004) y §Entrada, salida y
guardado del interior (T-1005). **Decisiones:** D-006.
**Tareas:** T-1001…T-1007. **Pruebas:** `01_logic`, `18_interiors`.
**Problemas:** T-1007 está pospuesta; no hay interiores de naves ni de
estaciones no propias.

## Interfaz, HUD y render

**Estado:** Implementado.

**Código:** `MyGame.java` (estados, entrada, sesión, guardado y telemetría),
`UiKit.java` (paneles, botones, iconos y texto), `PlayHud.java` (HUD de partida),
`OverlayRenderer.java` (menú, pausa, fin y fundido), `SpaceBackground.java`
(fondo), `HudView.java` (estado efímero), `Visuals.java`/`Effects.java` (paleta,
texturas y partículas).
**Diseño:** `docs/arquitectura.md` §Estructura de la interfaz y §Render y aspecto
visual; `docs/convenciones.md`. **Decisiones:** D-002. **Tareas:** T-007, T-504,
T-1110. **Pruebas:** `06_visual_menu`, `07_visual_play`, `08_visual_death`,
`22_ui_polish`. **Problemas:** `MyGame` sigue siendo el orquestador central; no
se crean objetos `Color` ni asignaciones en `render()`.

## Audio y música

**Estado:** Implementado (audio local, sin red).

**Código:** `AudioService.java` (efectos, bucle de motor, música y silencio),
`assets/audio/` (recursos locales), `MyGame.java` (opciones de audio).
**Diseño:** `docs/MASTER_PLAN.md` §11.5; `docs/arquitectura.md` §Audio y efectos
(T-1104) y §Música y ambientes dinámicos (T-1108). **Decisiones:** D-009.
**Tareas:** T-1104, T-1108. **Pruebas:** `19_audio`, `21_music`.
**Problemas:** no hay streaming ni audio online; los recursos deben vivir en
`assets/`, no dentro de jars.

## Persistencia y guardado

**Estado:** Implementado.

**Código:** `SaveService.java` (archivo local, escritura atómica, backup y
migración), `SaveCodec.java` (JSON v2: `profile`, `pilot`, `ship`, `sector`),
`GameSession.java` (composición), `PlayerProfile.java` (identidad y progreso),
`SectorRuntime.java` (snapshot).
**Diseño:** `docs/MASTER_PLAN.md` §11.3; `docs/arquitectura.md` §Persistencia y
descarga de sectores. **Decisiones:** D-008. **Tareas:** T-104, T-505, T-605,
T-803, T-905. **Pruebas:** `01_logic`, `13_station`, `14_market`, `16_universe`.
**Problemas:** el guardado es local; no hay nube. La migración desde `run2` se
conserva por compatibilidad.

## Accesibilidad y localización

**Estado:** Parcial: UI en español y fuente ASCII; la localización completa
queda pendiente.

**Código:** `MyGame.java` (avisos y navegación Atrás), `UiKit.java` (botones
táctiles y texto ajustado), `docs/convenciones.md` (reglas).
**Diseño:** `docs/arquitectura.md` §Accesibilidad y localización (T-1106);
`docs/convenciones.md` §Accesibilidad (T-1106). **Decisiones:** ninguna
específica. **Tareas:** T-1106. **Pruebas:** `20_accessibility`,
`22_ui_polish`. **Problemas:** la fuente `lsans-15` es ASCII; no se introducen
acentos sin comprobar el glifo. No hay traducción a otros idiomas.

## Facciones, combate y online

**Estado:** Aplazado (D-012). No hay código implementado.

**Código previsto:** `Faction.java`, `Reputation.java`, `CombatSystem.java` (no
existen todavía).
**Diseño:** `docs/MASTER_PLAN.md` §11.6; `docs/DECISIONS.md` D-012.
**Decisiones:** D-012. **Tareas:** T-107, T-1103 (pospuesta).
**Pruebas:** no hay pruebas porque no hay implementación.
**Problemas:** no presentes como funcionalidad; no deben tratarse como
implementados.

---

## Mantenimiento del índice

- Al añadir o mover un sistema, actualiza la fila de la tabla rápida y su
  sección.
- Usa rutas reales; no inventes clases ni archivos.
- Enlaza a la sección concreta de `docs/MASTER_PLAN.md` y
  `docs/arquitectura.md`, no al documento completo.
- Si un sistema cambia de estado, actualiza `Estado` y las pruebas.
- `docs/STATUS.md` es generado: no lo edites a mano.
- El tracker sigue siendo la fuente canónica; este índice no duplica su estado.
