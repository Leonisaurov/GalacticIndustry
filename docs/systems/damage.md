# Sistema — Daños y reparaciones

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `Ship.java`, `GameWorld.java`, `StationScreen.java`, `Upgrades.java`, `LifeSupport.java` |
| Documentación | `MASTER_PLAN.md` §4.3, §7.3; `arquitectura.md` §Daño y reparaciones, §Secuencia de muerte |
| Decisiones | D-001 (muerte permanente), D-002 (reparaciones en estación) |
| Tareas | T-205, T-206, T-504 |
| Pruebas | `09_wing_damage`, `05_death`, `13_station`, `01_logic` |

## 1. Descripción

Cada impacto de asteroide daña el casco y, además, un sistema o ala al azar. Las
partes destruidas aplican penalizaciones: motor, depósito, cañón, timón, alas,
láser y soporte vital. El casco a 0 destruye la nave y termina la partida.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Casco | Implementado | `HULL`; a 0, `GameWorld.isOver()` y explosión. |
| Motor | Implementado | A 0 reduce el empuje. |
| Depósito | Implementado | A 0 reduce capacidad y puede provocar fuga. |
| Cañón | Implementado | A 0 impide disparar. |
| Timón | Implementado | A 0 reduce el giro. |
| Alas | Implementado | A 0 afecta al dibujo y a la estabilidad; hay prueba visual. |
| Láser minero | Implementado | A 0 reduce o anula la extracción. |
| `LIFE_SUPPORT` | Implementado | A 0 baja el filtro al mínimo (20 %); la brecha se gestiona aparte. |
| Reparación | Implementado | `StationScreen` repara partes con monedas. |
| Nave legado | Implementado | Al morir se conserva la nave para un personaje nuevo. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Ship` | Almacena `part[]`, aplica `damage()`, `repair()` y calcula factores. |
| `GameWorld` | `collideShipAsteroids()`, `damageShip()`, `markLegacyWreck()`. |
| `LifeSupport` | Reacciona al daño de casco: fuga y brecha. |
| `StationScreen` | Coste y ejecución de reparaciones. |
| `Upgrades` | Blindaje y mejora de casco. |
| `MyGame` | `triggerDeath()`, `triggerPilotDeath()`, `finishRun()`. |

## 4. Flujo

1. `GameWorld.collideShipAsteroids()` detecta el impacto.
2. `damageShip()` daña `HULL` y un sistema aleatorio; avisa con partículas.
3. `LifeSupport.onHullDamage()` actualiza fuga y posible brecha.
4. Las partes dañadas cambian los factores de motor, giro, depósito, cañón o láser.
5. El casco a 0 ejecuta `triggerDeath()`; el piloto a 0 ejecuta `triggerPilotDeath()`.
6. `finishRun()` guarda el mundo, marca la nave legado y cierra la partida.

## 5. Decisiones aplicables

- **D-001:** muerte permanente; el mundo persiste y la nave queda en el lugar de
  la muerte. No hay reaparición automática en la nave nodriza.
- **D-002:** las reparaciones son servicios de estación.

## 6. Tareas relacionadas

T-205 (HUD de daños), T-206 (reparación y O2), T-504 (retirada de la tienda de
pausa), T-208/T-209 (muerte y legado).

## 7. Pruebas existentes

`09_wing_damage` (ala destruida), `05_death` (casco y piloto), `13_station`
(reparaciones), `01_logic` (factores y persistencia).

## 8. Problemas conocidos

- No hay vidas: el casco a 0 termina la partida.
- La reparación de `LIFE_SUPPORT` no cierra una brecha abierta; ver
  [`survival.md`](survival.md).
- El daño de sistemas es aleatorio; las pruebas no deben depender de una parte
  concreta salvo que usen el modo de prueba adecuado.

## 9. Dependencias

Nave, supervivencia, estaciones, persistencia, HUD y muerte.
