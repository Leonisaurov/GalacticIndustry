# Navegación, cartografía y comienzo de partida

**Estado:** Implementado (UPD-006)  
**Decisiones:** D-017, D-018…D-023, D-025  
**Tareas:** UPD-006-T02…T43 (T34 pospuesto)  
**Pruebas:** `SelfTest`, `30_docking_physical`, `31_compass_radar`,
`32_local_map`, `34_conventional_travel`, `18_map`, `17_warp`,
`29_first_ship`, `35_service_ship`

## Responsabilidad

Agrupa la física de la nodriza, el campo de contención, la orientación
(brújula y radar), los mapas local y de navegación, el viaje convencional/warp
y el comienzo de partida con compra de la primera nave.

## Clases principales

| Clase | Responsabilidad |
|---|---|
| `StationDef`, `Station` | Boca, punto de espera, corredor, guías y energía |
| `StationEnergy` | Capacidad, recarga y consumo continuo/por impacto |
| `ContainmentField` | Impactos, desvío, fragmentación y expulsión del radio seguro |
| `DockingSystem`, `Autopilot` | Acople manual y atraque automático por fases |
| `NavTarget`, `Sensors` | Objetivos conocidos y alcance de sensores |
| `LocalMapScreen` | Proyección 2D, zoom, pan, iconos, niebla y fijado |
| `MapScreen`, `TravelPlan`, `TravelService` | Mapa de navegación y viaje warp/convencional |
| `RadarScreen` | Radar circular con filtros y contactos reales |
| `ShipSpec`, `ShipOffer`, `ShipMarket` | Mercado determinista de naves |
| `ShipDealerScreen`, `EquipmentScreen`, `LoadoutPreview` | Compra, inspección, ranuras y previsualización |
| `ShipSlotDef` | Tipo, posición y compatibilidad de ranuras |
| `IntroSequence`, `GameSession`, `SaveCodec` | Inicio a pie, presupuesto, propiedad y migración v3 |
| `NpcActor`, `InteriorScene.mothership()` | Interior habitado de la nodriza |

## Flujo de estados

```text
MENU -> INTRO -> INTERIOR (nodriza, sin nave)
  -> SHIP_DEALER (compra + combustible + módulos)
  -> EQUIPMENT (ranuras y previsualización)
  -> HANGAR / BOARD_SHIP -> PLAYING
  -> MAP (navegación) / LOCAL_MAP / RADAR
  -> DOCKED (servicios) -> INTERIOR (nodriza)
```

## Reglas

- El corredor es la única excepción a la colisión del casco.
- Sin energía, el campo de contención se apaga y no bloquea rocas.
- La brújula básica no ocupa ranura; la avanzada exige `NAVIGATION`; el radar
  exige `RADAR`.
- El viaje convencional no exige warp; el warp es una ventaja.
- Los guardados v2 migran a `hasShip = true`; los nuevos guardan nave,
  condición, descubrimiento y energía.
- El tutorial y la campaña siguen pospuestos (`D-024`).

## Pruebas de referencia

- `java -cp "build/classes:jar/*" com.example.codegame.SelfTest`
- `tools/run-tests.sh 30_docking_physical 31_compass_radar 32_local_map`
- `tools/run-tests.sh 29_first_ship 33_equipment 34_conventional_travel 35_service_ship`
