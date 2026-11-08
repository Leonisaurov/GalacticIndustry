# Sistema — Estación personal y módulos

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `PlayerStation.java`, `ModuleDef.java`, `StationModule.java`, `ConstructionService.java`, `StationManagementScreen.java`, `StationStorage.java`, `Market.java` |
| Documentación | `MASTER_PLAN.md` §8.5; `arquitectura.md` §Estación personal (datos), §Sistema modular, §Almacén y taller, §Refinería y mercado, §Pantalla de gestión |
| Decisiones | D-005 (estación personal vacía y modular) |
| Tareas | T-901…T-908 |
| Pruebas | `01_logic`, `17_player_station`, `14_market` |

## 1. Descripción

La estación personal empieza como una plataforma vacía. El jugador construye
módulos que habilitan servicios: almacén, taller, refinería y mercado. Sin
módulos no hay servicios.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Plataforma vacía | Implementado | `PlayerStation`. |
| Catálogo de módulos | Implementado | `ModuleDef` con coste y requisitos. |
| Módulo y nivel | Implementado | `StationModule`. |
| Construcción | Implementado | `ConstructionService` valida coste y colocación. |
| Gestión | Implementado | `StationManagementScreen`. |
| Almacén y taller | Implementado | Servicios derivados del módulo. |
| Refinería y mercado | Implementado | Servicios derivados. |
| Persistencia | Implementado | Guardado v2. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `PlayerStation` | Plataforma, módulos y servicios derivados. |
| `ModuleDef` | Catálogo construible. |
| `StationModule` | Instancia y nivel. |
| `ConstructionService` | Coste, validación y colocación. |
| `StationManagementScreen` | Interfaz de construcción y gestión. |
| `StationStorage` / `Market` | Servicios habilitados por módulos. |

## 4. Flujo

1. El jugador abre la gestión de su estación.
2. `ConstructionService` valida monedas, espacio y requisitos.
3. Al construir, `PlayerStation` habilita el servicio.
4. El almacén, taller, refinería o mercado aparecen en la estación.
5. El estado se guarda con la partida.

## 5. Decisiones aplicables

- **D-005:** la estación nace vacía; los servicios se construyen. No se asume
  que el jugador tenga almacén, reparación o mercado desde el inicio.

## 6. Tareas relacionadas

T-901 (datos), T-902 (módulos), T-903 (construcción), T-904 (gestión), T-905
(almacén/taller), T-906 (refinería), T-907 (servicios), T-908 (mercado).

## 7. Pruebas existentes

`01_logic` (costes, módulos y servicios), `17_player_station` (construcción y
gestión), `14_market` (mercado habilitado).

## 8. Problemas conocidos

- Sin módulos no hay almacén, reparación, refinería ni mercado.
- La colocación y los requisitos deben validarse; no crear módulos gratis.
- No hay interiores de naves ni estaciones no propias (T-1007 pospuesta).

## 9. Dependencias

Economía, estaciones, interiores, persistencia y HUD.
