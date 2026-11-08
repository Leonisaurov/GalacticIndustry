# Sistema — Contratos y misiones

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado (básico)** |
| Código | `Mission.java`, `MissionService.java`, `Station.java`, `MyGame.java` |
| Documentación | `MASTER_PLAN.md` §8.3; `arquitectura.md` §Contratos de estación (T-1102) |
| Decisiones | Ninguna específica; D-012 aplaza facciones y narrativa. |
| Tareas | T-1102 |
| Pruebas | `01_logic`, `13_station`, `14_market` |

## 1. Descripción

Contratos de estación para entregar mineral antes de un plazo. Las ofertas son
deterministas por estación y se siguen desde el HUD. No hay facciones,
reputación ni cadenas narrativas.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Contrato | Implementado | `Mission` con mineral, cantidad, plazo, recompensa y estado. |
| Ofertas | Implementado | `MissionService` determinista por estación. |
| Aceptación | Implementado | `MyGame` y `StationScreen`. |
| Seguimiento | Implementado | HUD y estado de la misión. |
| Entrega | Implementado | Valida mineral y plazo. |
| Facciones/reputación | Aplazado | D-012; no implementado. |
| Cadenas narrativas | No implementado | Fuera del alcance actual. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Mission` | Datos, estado y recompensa del contrato. |
| `MissionService` | Generación determinista, aceptación, entrega y plazo. |
| `Station` | Servicio de misiones por estación. |
| `MyGame` | Aceptación, seguimiento y finalización. |

## 4. Flujo

1. La estación ofrece contratos deterministas.
2. El jugador acepta uno; queda activo con plazo.
3. Al entregar el mineral requerido antes del plazo, cobra la recompensa.
4. Si expira, el contrato falla.

## 5. Decisiones aplicables

- **D-012:** facciones, combate y online quedan aplazados; no deben presentarse
  como parte del sistema de misiones actual.

## 6. Tareas relacionadas

T-1102 (contratos de estación). T-1103 (facciones/combate) está pospuesta.

## 7. Pruebas existentes

`01_logic` (generación y entrega), `13_station` (servicio), `14_market`
(interacción económica).

## 8. Problemas conocidos

- Solo existe el contrato básico de entrega; no hay narrativa, reputación ni
  facciones.
- El plazo y la recompensa deben mantenerse deterministas para las pruebas.

## 9. Dependencias

Estaciones, economía, carga, HUD y persistencia.
