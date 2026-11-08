# Sistema — Universo, galaxias y viaje warp

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `UniverseState.java`, `GalaxyDef.java`, `SystemDef.java`, `SectorDef.java`, `SectorRuntime.java`, `SectorStreamer.java`, `TravelService.java`, `JumpGate.java`, `MapScreen.java` |
| Documentación | `MASTER_PLAN.md` §5, §11.3; `arquitectura.md` §Universo y galaxias (datos), §Sector finito (runtime), §Streaming de sectores y regiones, §Viaje warp y TravelService, §Mapa de sectores |
| Decisiones | D-004 (warp libre y streaming), D-008 (guardado), D-011 (galaxias reales y ficticias) |
| Tareas | T-801…T-808 |
| Pruebas | `01_logic`, `16_universe`, `17_warp`, `18_map` |

## 1. Descripción

Universo jerárquico (universo → galaxia → sistema → sector) con sectores finitos
y carga dinámica. El viaje warp tiene coste y puede usar puertas de salto
opcionales. El mapa permite seleccionar destinos conocidos.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Jerarquía | Implementado | `UniverseState` y definiciones. |
| Sectores finitos | Implementado | `SectorDef` y `SectorRuntime`. |
| Streaming | Implementado | `SectorStreamer` carga/descarga sectores. |
| Viaje warp | Implementado | `TravelService` con coste. |
| Puertas de salto | Implementado (opcional) | `JumpGate`. |
| Mapa | Implementado | `MapScreen` con destinos y descubrimiento. |
| Galaxias reales/ficticias | Parcial | Datos y diferenciación; contenido limitado al inicial. |
| Persistencia de sectores no visitados | Parcial | Snapshots resumidos, no detalle completo. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `UniverseState` | Jerarquía, descubrimiento y snapshots. |
| `GalaxyDef` / `SystemDef` / `SectorDef` | Datos estáticos. |
| `SectorRuntime` | Snapshot del sector activo. |
| `SectorStreamer` | Carga y descarga de sectores. |
| `TravelService` | Coste, destino y transición warp. |
| `JumpGate` | Puerta opcional. |
| `MapScreen` | Mapa, selección y viaje. |

## 4. Flujo

1. `GameSession` crea el universo y el sector inicial.
2. `SectorStreamer` carga el sector activo y descarga los lejanos.
3. `MapScreen` muestra destinos descubiertos.
4. `TravelService` valida combustible/coste y cambia de sector.
5. El sector anterior se guarda como snapshot.

## 5. Decisiones aplicables

- **D-004:** viaje warp libre, sin puertas obligatorias; sectores finitos y carga
  dinámica.
- **D-008:** guardado local JSON con backup.
- **D-011:** galaxias reales y ficticias; sin exactitud astronómica.

## 6. Tareas relacionadas

T-801…T-808: universo, sectores, streaming, warp, mapa y contenido.

## 7. Pruebas existentes

`01_logic` (jerarquía y snapshots), `16_universe` (streaming), `17_warp`
(viaje), `18_map` (mapa).

## 8. Problemas conocidos

- Los sectores no visitados solo conservan un snapshot resumido; no se simula su
  detalle completo.
- El contenido de galaxias es limitado; la arquitectura permite ampliarlo.
- El viaje warp consume recursos; no duplicar costes en documentos.

## 9. Dependencias

Nave y combustible, estaciones, persistencia, mapa/HUD y economía.
