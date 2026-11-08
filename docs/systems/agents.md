# Sistema — Pilotos autónomos

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Parcial** (simulación económica abstracta) |
| Código | `PilotAgent.java`, `AgentRoster.java`, `AgentBrain.java`, `EconomySim.java`, `SimClock.java` |
| Documentación | `MASTER_PLAN.md` §10, §13.5; `arquitectura.md` §Pilotos autónomos (datos), §Tráfico cosmético; `pruebas.md` §Gate de la Etapa 7 |
| Decisiones | D-007 (cantidad de agentes y simulación offline) |
| Tareas | T-701…T-707 |
| Pruebas | `01_logic`, `15_agents` |

## 1. Descripción

Simulación de pilotos NPC que producen, transportan y comercian de forma
abstracta. No tienen posición 2D ni se renderizan; el tráfico visual es
cosmético (`StationTraffic`). El objetivo es dar vida económica al universo sin
IA pesada ni servicios externos.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Identidad y rol | Implementado | `PilotAgent` con nombre, rol, carga y estado. |
| Plantilla | Implementado | `AgentRoster` con 8 iniciales y máximo 24. |
| Cerebro | Implementado | `AgentBrain` con estados y utilidad. |
| Simulación | Implementado | `EconomySim` con tick, LOD y transacciones. |
| Reloj | Implementado | `SimClock`. |
| Posición 2D | No implementado | Los agentes no vuelan en el sector. |
| Render | No implementado | No se dibujan como naves. |
| Pathfinding | No implementado | La simulación es por tiempo/estado. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `PilotAgent` | Datos del agente y su carga. |
| `AgentRoster` | Plantilla, creación y límite. |
| `AgentBrain` | Estado y utilidad de decisión. |
| `EconomySim` | Tick económico, LOD y transacciones. |
| `SimClock` | Tiempo de simulación. |

## 4. Flujo

1. `AgentRoster` crea agentes al iniciar el universo.
2. `EconomySim` avanza el reloj y ejecuta decisiones de `AgentBrain`.
3. Las transacciones afectan a stock y precios.
4. El LOD reduce la frecuencia de simulación de agentes lejanos.

## 5. Decisiones aplicables

- **D-007:** cantidad inicial de agentes y simulación offline; ajustable por
  telemetría.

## 6. Tareas relacionadas

T-701…T-707: datos, roster, cerebro, simulación, LOD, integración y balance.

## 7. Pruebas existentes

`01_logic` (roster, decisiones y economía), `15_agents` (flujo en dispositivo).

## 8. Problemas conocidos

- No hay posición 2D ni render; no deben describirse como naves visibles.
- La simulación es abstracta y por tiempo, no pathfinding.
- Cambiar el número de agentes afecta a `EconomySim` y a `15_agents`.

## 9. Dependencias

Economía, universo, reloj, estaciones y persistencia.
