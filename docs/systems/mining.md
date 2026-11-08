# Sistema — Minería, misiles y rayo tractor

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `MiningLaser.java`, `EnergyCapacitor.java`, `Asteroid.java`, `AsteroidType.java`, `Missile.java`, `OreFragment.java`, `TractorBeam.java`, `GameWorld.java`, `Upgrades.java` |
| Documentación | `MASTER_PLAN.md` §6.1–6.3, §13.3; `arquitectura.md` §Asteroides y composición, §Minería láser y energía, §Extracción y agotamiento, §Fragmentos por misiles, §Rayo tractor; `pruebas.md` §Gate de la Etapa 4 |
| Decisiones | D-003 (láser, misiles y tractor) |
| Tareas | T-401…T-410 |
| Pruebas | `01_logic`, `12_mining`, `13_tractor`, `04_autopilot` |

## 1. Descripción

Cadena de extracción: el láser consume energía del capacitor y extrae mineral de
un asteroide; los misiles destruyen o fragmentan asteroides; los fragmentos
pueden recogerse a mano o con el rayo tractor. El mineral no entra en la bodega
hasta que se recoge.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Láser minero | Implementado | Potencia, penetración y consumo de capacitor; no consume combustible. |
| Tipos de asteroide | Implementado | C/S/M con reserva y composición. |
| Energía compartida | Implementado | `EnergyCapacitor` para láser y tractor. |
| Agotamiento | Implementado | La reserva baja y el asteroide puede desaparecer. |
| Misiles | Implementado | Proyectil con vida y daño; fragmentación. |
| Fragmentos | Implementado | `OreFragment` con vida limitada y dispersión. |
| Rayo tractor | Implementado | Alcance, velocidad y fragmentos simultáneos; el módulo HERRAMIENTAS suma +30 u de alcance. |
| Recogida | Implementado | `GameWorld` valida la bodega y recoge. |
| Mejoras de minería | Implementado | `Upgrades`. |

## 3. Ruta mínima de lectura

| Archivo | Qué leer |
|---|---|
| `MiningLaser.java` | constantes, `tick`, `canMine`, potencia y penetración |
| `EnergyCapacitor.java` | energía, `drain`, `update` |
| `Asteroid.java` / `AsteroidType.java` | reserva, tamaño y composición |
| `GameWorld.java` | extracción, impacto de misiles, dispersión y recogida (busca `minedThisFrame`, `updateMissiles`, `updateFragments`) |
| `Missile.java` / `OreFragment.java` | vida, daño y caducidad |
| `TractorBeam.java` | alcance, velocidad y consumo |
| `tools/tests/12_mining.sh` / `13_tractor.sh` | flujo en dispositivo |

No leas `SelfTest.java` completo: busca los bloques de minería, misiles y tractor.

## 4. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `MiningLaser` | Extracción por tick, potencia, penetración y consumo. |
| `EnergyCapacitor` | Energía disponible y recarga. |
| `Asteroid` / `AsteroidType` | Reserva, tamaño, composición y agotamiento. |
| `Missile` | Vuelo, impacto y daño. |
| `OreFragment` | Fragmento recolectable y tiempo de vida. |
| `TractorBeam` | Atracción de fragmentos y consumo. |
| `GameWorld` | Objetivo, extracción, impactos, dispersión y recogida. |

## 5. Flujo

1. El jugador selecciona un asteroide (requiere módulo de autopiloto para el
   selector; el láser se activa desde los controles).
2. `MiningLaser.tick()` consume capacitor y reduce la reserva del asteroide.
3. El mineral extraído puede aparecer como fragmento o entrar en la bodega según
   el modo de extracción.
4. Los misiles impactan y convierten reserva en fragmentos.
5. El tractor atrae fragmentos; la recogida valida la masa de `CargoHold`.
6. Los fragmentos no recogidos se pierden al agotar su vida.

## 6. Decisiones aplicables

- **D-003:** separar láser (extracción principal), misiles (destrucción y
  fragmentación) y rayo tractor (recogida). El láser no consume combustible;
  comparte capacitor con el tractor.

## 7. Tareas relacionadas

T-401…T-410: láser, asteroides, energía, misiles, fragmentos, tractor y
balance.

## 8. Pruebas existentes

- `01_logic`: extracción, agotamiento, composición y bodega.
- `12_mining`: flujo de minería en dispositivo.
- `13_tractor`: captura de fragmentos.
- `04_autopilot`: modo `MINE` y aproximación.

## 9. Problemas conocidos

- El láser no consume combustible; solo capacitor.
- El tractor y el láser comparten capacitor.
- Los fragmentos se pierden si no se recogen a tiempo.
- La composición y el balance están en `MASTER_PLAN.md` §6 y §13.3; no duplicar
  fórmulas aquí.

## 10. Dependencias

Nave y carga, energía, movimiento, autopiloto, HUD, economía y persistencia.
