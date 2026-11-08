# Sistema — Movimiento, propulsión y combustible

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `Ship.java`, `MassModel.java`, `FuelPickup.java`, `Controls.java`, `Autopilot.java` |
| Documentación | `MASTER_PLAN.md` §4.2, §13.1; `arquitectura.md` §Masa y movimiento, §Piloto automático |
| Decisiones | Ninguna específica; D-001 y D-002 afectan a la muerte y los servicios. |
| Tareas | T-303, T-304, T-404, T-502 |
| Pruebas | `01_logic`, `02_navigation`, `03_controls`, `04_autopilot` |

## 1. Descripción

Movimiento 2D con giro, inercia y consumo de combustible. La masa de la carga
modifica la aceleración y el consumo. Acelerar y disparar gastan combustible;
los bidones lo recuperan. El piloto automático usa el mismo modelo.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Giro e inercia | Implementado | `Ship` aplica dirección, velocidad y rozamiento. |
| Consumo al acelerar | Implementado | El empuje consume combustible según masa y motor. |
| Consumo al disparar | Implementado | `Ship.canFire()` exige combustible y cañón. |
| Efecto de la carga | Implementado | `MassModel` modula aceleración y consumo. |
| Bidones | Implementado | `FuelPickup` y recogida por proximidad. |
| Repostaje en estación | Implementado | Servicio de combustible con coste. |
| Rescate por falta de combustible | Implementado | `RescueService` con coste/pérdida de carga. |
| Vuelo manual | Implementado | Joystick, teclado y controles táctiles. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Ship` | Estado cinemático, empuje, giro, consumo y límites. |
| `MassModel` | Masa efectiva y penalización por carga. |
| `FuelPickup` | Bidón recogible y cantidad. |
| `Controls` | Entrada de un fotograma (empuje, giro, disparo, etc.). |
| `Autopilot` | Rumbo, frenada y modos de vuelo automático. |

## 4. Flujo

1. `Controls` recoge la entrada del jugador o del piloto automático.
2. `Ship.update()` aplica giro, empuje, inercia y consumo.
3. La masa de `CargoHold` entra en `MassModel`.
4. La recogida de `FuelPickup` y el repostaje de estación rellenan el depósito.
5. El guardado conserva posición, rumbo, velocidad y combustible.

## 5. Decisiones aplicables

No hay una decisión de producto exclusiva. La economía de combustible es una
regla crítica: acelerar y disparar consumen; cualquier cambio debe actualizar
`SelfTest` y la documentación.

## 6. Tareas relacionadas

T-303 (movimiento y masa), T-304 (combustible), T-404 (piloto automático),
T-502 (acoplamiento y aproximación).

## 7. Pruebas existentes

`01_logic` (consumo y masa), `02_navigation` (rumbo y llegada), `03_controls`
(entrada), `04_autopilot` (frenada y captura).

## 8. Problemas conocidos

- El piloto automático debe limpiar el acelerador y frenar (`STOP`) al perder el
  objetivo; si se cambia, revisar `04_autopilot`.
- El consumo exacto depende de mejoras y masa; no duplicar fórmulas en documentos.

## 9. Dependencias

Nave y carga, daños (motor/depósito), piloto automático, estaciones y rescate.
