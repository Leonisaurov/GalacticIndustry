# Sistema — Interiores y avatar

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Parcial** (estación personal; naves y estaciones ajenas aplazadas) |
| Código | `InteriorScene.java`, `Room.java`, `Door.java`, `Interactable.java`, `Avatar.java`, `InteriorInput.java`, `InteriorRenderer.java` |
| Documentación | `MASTER_PLAN.md` §8.5; `arquitectura.md` §Marco de interiores, §Avatar y movimiento interior, §Render de interiores, §Interacción por proximidad, §Entrada, salida y guardado del interior |
| Decisiones | D-006 (interiores de estaciones y naves) |
| Tareas | T-1001…T-1007 |
| Pruebas | `01_logic`, `18_interiors` |

## 1. Descripción

El jugador puede entrar en la estación personal y moverse a pie por salas con
puertas e interactuables. El render es cenital. La entrada y salida conservan la
posición del avatar.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Marco de salas | Implementado | `InteriorScene`, `Room`, `Door`. |
| Avatar | Implementado | Movimiento, colisión y sala actual. |
| Render | Implementado | `InteriorRenderer` cenital. |
| Entrada | Implementado | `InteriorInput`. |
| Interacción | Implementado | `Interactable` por proximidad. |
| Guardado de posición | Implementado | `GameSession.saveAvatar()` / `restoreAvatar()`. |
| Interiores de naves | Aplazado | T-1007. |
| Interiores de estaciones ajenas | Aplazado | T-1007. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `InteriorScene` | Salas, puertas e interactuables. |
| `Room` / `Door` / `Interactable` | Geometría y puntos de interacción. |
| `Avatar` | Personaje, movimiento, colisión y sala. |
| `InteriorInput` | Entrada de movimiento/interacción. |
| `InteriorRenderer` | Dibujo cenital. |

## 4. Flujo

1. El jugador entra en el interior desde la estación personal.
2. `Avatar` se mueve con `InteriorInput` y colisiona con salas/puertas.
3. `InteriorRenderer` dibuja la sala y los interactuables.
4. Al salir, `GameSession` guarda sala y posición.
5. Al volver, se restaura la posición.

## 5. Decisiones aplicables

- **D-006:** a largo plazo habrá interiores de estaciones y naves. En el alcance
  actual solo está implementado el de la estación personal; T-1007 está
  pospuesta.

## 6. Tareas relacionadas

T-1001 (marco), T-1002 (avatar), T-1003 (render), T-1004 (interacción), T-1005
(entrada/salida/guardado), T-1006 (verificación), T-1007 (naves/estaciones
ajenas, pospuesta).

## 7. Pruebas existentes

`01_logic` (posición y colisión), `18_interiors` (flujo en dispositivo).

## 8. Problemas conocidos

- T-1007 pospuesta: no hay interiores de naves ni de estaciones no propias.
- El render es cenital y sencillo; no hay modelo 3D.
- El joystick y la acción contextual deben seguir visibles en interiores
  (T-1110).

## 9. Dependencias

Estación personal, estaciones, HUD/entrada, persistencia y render.
