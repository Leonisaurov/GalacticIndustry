# Sistema — Estaciones, acoplamiento y rescate

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `Station.java`, `StationDef.java`, `DockingSystem.java`, `StationScreen.java`, `RescueService.java`, `StationTraffic.java` |
| Documentación | `MASTER_PLAN.md` §4.7, §8; `arquitectura.md` §Estación nodriza, §Acoplamiento, §Servicios de estación, §Rescate por combustible |
| Decisiones | D-001 (mundo persistente), D-002 (servicios en estación) |
| Tareas | T-501…T-507, UPD-004-T01 |
| Pruebas | `13_station`, `02_navigation`, `03_controls`, `23_ship_modules` |

## 1. Descripción

Estaciones físicas con radio, zona segura, servicios y almacén. El jugador se
acopla si está cerca y a baja velocidad. Los servicios incluyen combustible,
oxígeno, reparaciones, mejoras, mercado y almacén. El rescate por combustible
evita quedar varado.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Estación nodriza | Implementado | `Station` con posición, almacén y misiones. |
| Definiciones | Implementado | `StationDef` con servicios booleanos. |
| Acoplamiento | Implementado | Proximidad, velocidad y estado `DOCKED`. |
| Servicios | Implementado | Reparar, repostar, O2, mejoras, módulos de nave, mercado, almacén. |
| Rescate | Implementado | `RescueService` con coste o pérdida de carga. |
| Tráfico | Implementado | `StationTraffic` cosmético. |
| Zona segura | Implementado | No se generan asteroides dentro. |
| Tienda de pausa | Retirada | T-504; la economía vive en la estación. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Station` | Instancia, posición, almacén, misiones y zona segura. |
| `StationDef` | Datos estáticos y servicios disponibles. |
| `DockingSystem` | Comprueba distancia/velocidad y cambia a `DOCKED`. |
| `StationScreen` | Interfaz de servicios y acciones. |
| `RescueService` | Solicitud, coste, pérdida de carga y temporizador. |
| `StationTraffic` | Naves cosméticas de ambiente. |

## 4. Flujo

1. El jugador se aproxima dentro de `dockRadius` y a baja velocidad.
2. `DockingSystem` permite acoplarse; `StationScreen` muestra servicios.
3. Las acciones descuentan monedas y llaman a `saveRun()`.
4. Si falta combustible, `RescueService.canRequest()` ofrece rescate.
5. Al despegar, el estado vuelve a `PLAYING`.

## 5. Decisiones aplicables

- **D-001:** la estación no es punto de reaparición automático.
- **D-002:** compras, ventas, mejoras, reparaciones y servicios solo en
  estaciones físicas.

## 6. Tareas relacionadas

T-501 (estación), T-502 (acoplamiento), T-503 (servicios), T-504 (retirada de la
tienda de pausa), T-505 (almacén), T-506 (rescate), T-507 (tráfico).

## 7. Pruebas existentes

`13_station` (servicios, compraventa, O2, reparación y rescate),
`23_ship_modules` (compra, instalación, retirada y persistencia de módulos),
`02_navigation` (aproximación), `03_controls` (entrada).

## 8. Problemas conocidos

- La recarga de O2 no repone la gracia ni el estado `HYPOXIC`; ver
  [`survival.md`](survival.md).
- El rescate puede perder carga; el jugador debe poder pagar o aceptar la pérdida.
- No hay estaciones de otros jugadores ni online.

## 9. Dependencias

Nave, economía, contratos, supervivencia, persistencia, HUD y universo.


## 10. Propuesta UPD-006 (no implementada)

`UPD-006` propone, pendiente de aprobación global:

- Entrada identificable, punto de atraque y corredor de aproximación.
- Estacionamiento manual sin módulo, exigente pero aprendible.
- Módulo independiente `DOCKING_AUTOPILOT` para aproximación, alineación y acople.
- Colisión física con el casco y respuesta por impacto.
- Campo de contención con impactos contundentes, efectos visuales, naves
  autorizadas y consumo energético continuo y por impacto.
- Interior dedicado de la nodriza con hangar, concesionario, talleres, comercios,
  pasillos y NPC básicos.

Decisiones: `D-018` (campo), `D-019` (acoplamiento), `D-023` (interior). No se ha
modificado el código de estaciones.
