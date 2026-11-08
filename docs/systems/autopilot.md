# Sistema — Piloto automático

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `Autopilot.java`, `MyGame.java`, `ShipModuleDef.java`, `ShipLoadout.java` |
| Documentación | `MASTER_PLAN.md` §4.6, §12; `arquitectura.md` §Piloto automático; `pruebas.md` §Extras de arranque |
| Decisiones | D-010 (módulos instalables) |
| Tareas | T-404, T-502, T-1109, UPD-004-T01 |
| Pruebas | `04_autopilot`, `12_mining`, `13_station`, `23_ship_modules` |

## 1. Descripción

Vuelo automático hacia combustible, asteroides, estación o modo minero. El piloto
usa el mismo modelo de movimiento y combustible que el jugador. Requiere el
módulo `AUTOPILOT` instalado para seleccionar blancos.

## 2. Estado real

| Modo | Estado | Detalle |
|---|---|---|
| `FUEL` | Implementado | Rumbo a bidón y captura. |
| `ASTEROID` | Implementado | Rumbo a asteroide. |
| `MINE` | Implementado | Aproximación y extracción. |
| `STATION` | Implementado | Aproximación y acoplamiento. |
| `STOP` | Implementado | Frenada al perder el objetivo. |
| Cancelación | Implementado | El jugador recupera el control. |
| Requisito de módulo | Implementado | `ShipLoadout.hasAutopilot()`. |
| Adquisición del módulo | Implementada | Pestaña `MODULOS` de la estación: compra por 150 CR, primera ranura libre, retirada sin reembolso. |

## 3. Ruta mínima de lectura

| Archivo | Qué leer |
|---|---|
| `Autopilot.java` | modos, `target`, `steer`, `cancel`, frenada |
| `MyGame.java` | `pickTarget`, entrada táctil y transición de control |
| `ShipModuleDef.java` | definición del módulo `AUTOPILOT` |
| `ShipLoadout.java` | `hasAutopilot`, instalación y retirada |
| `tools/tests/04_autopilot.sh` | captura, frenada y cancelación |

No necesitas leer todo `MyGame.java`; busca `pickTarget` y `pilot`.

## 4. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Autopilot` | Modo, objetivo, rumbo, empuje y frenada. |
| `MyGame` | `pickTarget()`, entrada táctil y transición de control. |
| `ShipModuleDef` | Define el módulo `AUTOPILOT`. |
| `ShipLoadout` | Instala, consulta y retira el módulo. |

## 5. Flujo

1. `MyGame.pickTarget()` elige estación, asteroide o bidón si el módulo está.
2. `Autopilot` calcula rumbo y empuje; usa `Ship` y `MassModel`.
3. Al llegar o perder el objetivo, pasa a `STOP` y limpia el acelerador.
4. El jugador puede cancelar y recuperar el control.

## 6. Decisiones aplicables

- **D-010:** el piloto automático puede ser un módulo instalable; no es innato.
  `01_logic` comprueba que no se puede usar sin instalarlo.

## 7. Tareas relacionadas

T-404 (piloto automático), T-502 (aproximación a estación), T-1109 (módulos).

## 8. Pruebas existentes

`04_autopilot` (captura de bidón, asteroide, frenada y cancelación),
`12_mining` (modo `MINE`), `13_station` (aproximación y acople).

## 9. Problemas conocidos

- `MyGame.pickTarget` exige el módulo `AUTOPILOT` para elegir estación,
  asteroide o bidón; sin él, el toque en espacio libre dispara. El módulo se
  compra en la pestaña `MODULOS` de la estación (`23_ship_modules`).
- El piloto debe frenar (`STOP`) al perder el objetivo y limpiar el acelerador;
  si se cambia, revisar `04_autopilot`.
- Los comentarios de `Pilot` sobre "frenada" son históricos; el piloto
  automático real está en `Autopilot`.

## 10. Dependencias

Movimiento y combustible, nave y módulos, minería, estaciones, HUD.
