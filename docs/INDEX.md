# Índice de sistemas — GalacticIndustry / Asteroides

Mapa de navegación entre **mecánicas, código, documentación, decisiones, tareas y
pruebas**. No es un plan maestro ni una segunda fuente de verdad. La primera
tabla basta para localizar un sistema; las fichas de `docs/systems/` contienen el
detalle que necesita un agente antes de abrir código.

- **Código fuente:** `src/com/example/codegame/`
- **Estado operativo:** `docs/IMPLEMENTATION_TRACKER.json` (fuente canónica)
- **Resumen generado:** `docs/STATUS.md`
- **Especificación histórica:** `docs/MASTER_PLAN.md`
- **Decisiones:** `docs/DECISIONS.md`
- **Pruebas:** `docs/pruebas.md` y `tools/tests/`
- **Coordinación entre sesiones:** `docs/CONCURRENCY.md`

Salvo que se indique otra ruta, los `.java` viven en `src/com/example/codegame/`.

**Estados:** `Implementado` = funciona y tiene pruebas; `Parcial` = funciona con
alcance limitado; `Planificado` = diseñado sin código; `Aplazado` = fuera del
alcance actual.

## Cómo usar este índice

1. Consulta `docs/STATUS.md` para saber qué tarea está activa y cuál es la próxima.
2. Localiza el sistema en la tabla rápida.
3. Abre **solo** su ficha en `docs/systems/`.
4. Abre **solo** los archivos y pruebas que la ficha señala.
5. Si el índice no cubre el sistema, haz una búsqueda dirigida y actualiza la
   ficha al terminar.

No leas todas las fichas ni todo `MASTER_PLAN.md` para una sola tarea.

## Tabla rápida

| Sistema | Estado | Ficha | Código principal | Documentación | Decisiones | Tareas | Pruebas |
|---|---|---|---|---|---|---|---|
| Continuidad y panel | Implementado | [continuity](systems/continuity.md) | `tools/sync-dashboard.py`, `tools/continuity.py`, `docs/dashboard/*` | `dashboard/README.md`, `CONCURRENCY.md` | D-013, D-014, D-016 | T-003, T-006, T-007, UPD-001, UPD-002, UPD-005 | `sync-dashboard.py --check`, `continuity_test.py` (17), `dashboard_versions_test.js/.py` |
| Piloto y supervivencia | Implementado | [survival](systems/survival.md) | `Pilot.java`, `LifeSupport.java`, `GameSession.java`, `GameWorld.java` | `MASTER_PLAN.md` §4.1, §4.5, §7, §13.2 | D-001, D-002 | T-102, T-201…T-210 | `01_logic`, `10_survival`, `05_death` |
| Nave, componentes y carga | Implementado | [ship](systems/ship.md) | `Ship.java`, `ShipLoadout.java`, `ShipModuleDef.java`, `Upgrades.java`, `CargoHold.java` | `MASTER_PLAN.md` §4.2–4.4, §6.4 | D-010 | T-103, T-301…T-306, T-1101, T-1109, UPD-004 | `01_logic`, `11_cargo`, `23_ship_modules` |
| Movimiento y combustible | Implementado | [flight](systems/flight.md) | `Ship.java`, `MassModel.java`, `FuelPickup.java`, `Controls.java` | `MASTER_PLAN.md` §4.2, §13.1 | — | T-303, T-304, T-404, T-502 | `01_logic`, `02_navigation`, `03_controls` |
| Daños y reparaciones | Implementado | [damage](systems/damage.md) | `Ship.java`, `GameWorld.java`, `StationScreen.java`, `Upgrades.java` | `MASTER_PLAN.md` §4.3, §7.3 | D-001, D-002 | T-205, T-206, T-504 | `09_wing_damage`, `05_death`, `13_station` |
| Minería, misiles y tractor | Implementado | [mining](systems/mining.md) | `MiningLaser.java`, `EnergyCapacitor.java`, `Asteroid.java`, `Missile.java`, `OreFragment.java`, `TractorBeam.java` | `MASTER_PLAN.md` §6.1–6.3, §13.3 | D-003 | T-401…T-410 | `01_logic`, `12_mining`, `13_tractor` |
| Piloto automático | Implementado | [autopilot](systems/autopilot.md) | `Autopilot.java`, `MyGame.java`, `ShipModuleDef.java`, `ShipLoadout.java` | `MASTER_PLAN.md` §4.6, §12 | D-010 | T-404, T-502, T-1109, UPD-004 | `04_autopilot`, `12_mining`, `23_ship_modules` |
| Estaciones y acoplamiento | Implementado | [stations](systems/stations.md) | `Station.java`, `StationDef.java`, `DockingSystem.java`, `StationScreen.java`, `RescueService.java` | `MASTER_PLAN.md` §4.7, §8 | D-001, D-002 | T-501…T-507, UPD-004 | `13_station`, `02_navigation`, `23_ship_modules` |
| Comercio y economía | Implementado | [economy](systems/economy.md) | `Market.java`, `PriceModel.java`, `TradeService.java`, `MarketScreen.java`, `EconomySim.java` | `MASTER_PLAN.md` §9, §13.4 | D-002, D-005 | T-601…T-606 | `01_logic`, `14_market` |
| Contratos y misiones | Implementado (básico) | [contracts](systems/contracts.md) | `Mission.java`, `MissionService.java`, `Station.java` | `MASTER_PLAN.md` §8.3 | D-012 (aplazada) | T-1102 | `01_logic`, `13_station` |
| Pilotos autónomos | Parcial | [agents](systems/agents.md) | `PilotAgent.java`, `AgentRoster.java`, `AgentBrain.java`, `EconomySim.java` | `MASTER_PLAN.md` §10, §13.5 | D-007 | T-701…T-707 | `01_logic`, `15_agents` |
| Universo y viaje warp | Implementado | [universe](systems/universe.md) | `UniverseState.java`, `TravelService.java`, `SectorStreamer.java`, `MapScreen.java` | `MASTER_PLAN.md` §5, §11.3 | D-004, D-008, D-011 | T-801…T-808 | `16_universe`, `17_warp`, `18_map` |
| Estación personal | Implementado | [player-station](systems/player-station.md) | `PlayerStation.java`, `StationModule.java`, `ConstructionService.java` | `MASTER_PLAN.md` §8.5 | D-005 | T-901…T-908 | `17_player_station`, `14_market` |
| Interiores y avatar | Parcial | [interiors](systems/interiors.md) | `InteriorScene.java`, `Avatar.java`, `InteriorRenderer.java` | `MASTER_PLAN.md` §8.5 | D-006 | T-1001…T-1007 | `18_interiors` |
| Interfaz, HUD y render | Implementado | [ui](systems/ui.md) | `MyGame.java`, `UiKit.java`, `PlayHud.java`, `OverlayRenderer.java` | `arquitectura.md` §Interfaz, §Render; `convenciones.md` | D-002, D-015 | T-007, T-504, T-1110, UPD-003 | `06_visual_menu`, `07_visual_play`, `08_visual_death`, `13_station`, `14_market`, `17_player_station`, `18_interiors`, `18_map`, `22_ui_polish` |
| Audio y música | Implementado (local) | [audio](systems/audio.md) | `AudioService.java`, `assets/audio/` | `MASTER_PLAN.md` §11.5 | D-009 | T-1104, T-1108 | `19_audio`, `21_music` |
| Persistencia y guardado | Implementado | [persistence](systems/persistence.md) | `SaveService.java`, `SaveCodec.java`, `GameSession.java` | `MASTER_PLAN.md` §11.3 | D-008 | T-104, T-505, T-605, T-803, T-905 | `01_logic`, `16_universe` |
| Accesibilidad y localización | Parcial | [accessibility](systems/accessibility.md) | `MyGame.java`, `UiKit.java` | `arquitectura.md` §Accesibilidad; `convenciones.md` | — | T-1106 | `20_accessibility`, `22_ui_polish` |
| Facciones, combate y online | Aplazado | [deferred](systems/deferred.md) | — (no implementado) | `MASTER_PLAN.md` §11.6 | D-012 | T-107, T-1103 | — |

## Documentos de referencia

| Documento | Para qué |
|---|---|
| [`STATUS.md`](STATUS.md) | Etapa, tarea actual, próxima tarea, conteos y sesiones. |
| [`IMPLEMENTATION_TRACKER.json`](IMPLEMENTATION_TRACKER.json) | Estado canónico de tareas, decisiones y sesiones. |
| [`MASTER_PLAN.md`](MASTER_PLAN.md) | Diseño histórico y fórmulas. No estado de ejecución. |
| [`DECISIONS.md`](DECISIONS.md) | Contexto y respuesta de cada decisión. |
| [`IMPLEMENTATION_LOG.md`](IMPLEMENTATION_LOG.md) | Historial narrativo hasta S-092. |
| [`sessions/`](sessions/) | Sesiones nuevas, una por archivo. |
| [`updates/README.md`](updates/README.md) | Flujo de actualizaciones `UPD-XXX`. |
| [`CONCURRENCY.md`](CONCURRENCY.md) | Bloqueos, escrituras atómicas y coordinación. |
| [`pruebas.md`](pruebas.md) | Pruebas de dispositivo y telemetría. |

## Mantenimiento del índice

- Cada agente actualiza **su** ficha cuando cambia arquitectura, clases,
  responsabilidades, dependencias, pruebas o problemas conocidos.
- No hay un agente dedicado a mantener el índice.
- Los cambios en `INDEX.md` deben ser localizados y verificados; si otra sesión
  está editando el mismo archivo, relee antes de escribir.
- Usa rutas reales; no inventes clases, archivos ni pruebas.
- Enlaza a la sección concreta de `MASTER_PLAN.md` o `arquitectura.md`.
- No presentes una funcionalidad planificada o aplazada como implementada.
- `STATUS.md` es generado: no lo edites a mano.
- El tracker sigue siendo la fuente canónica; este índice no duplica su estado.
