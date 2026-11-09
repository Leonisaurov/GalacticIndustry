# Sistema — Nave, componentes y carga

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `Ship.java`, `ShipClassDef.java`, `ShipModuleDef.java`, `ShipLoadout.java`, `Upgrades.java`, `CargoHold.java`, `Commodity.java`, `Cargo.java`, `StationStorage.java` |
| Documentación | `MASTER_PLAN.md` §4.2–4.4, §6.4; `arquitectura.md` §Clases de nave (T-1101), §Módulos de nave (T-1109), §Carga y bodega |
| Decisiones | D-010 (naves y personalización) |
| Tareas | T-103, T-301…T-306, T-1101, T-1109, UPD-004-T01 |
| Pruebas | `01_logic`, `11_cargo`, `12_mining`, `23_ship_modules` |

## 1. Descripción

La nave es la unidad jugable: casco, sistemas reparables, combustible, bodega,
clase, módulos y ranuras. La carga se mide en masa (UM) y afecta al movimiento.
Las mejoras se compran en estación y se aplican a la partida en curso.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Partes y casco | Implementado | `HULL`, motor, depósito, cañón, timón, alas, láser y `LIFE_SUPPORT`. |
| Clases de nave | Implementado | `ShipClassDef` define forma y características; T-1101. |
| Módulos y ranuras | Implementado | `ShipModuleDef`, `ShipLoadout`; se compran e instalan en la pestaña `MODULOS` de la estación. |
| Mejoras | Implementado | `Upgrades` gestiona niveles, costes, blindaje, depósito, bodega y tienda. |
| Bodega | Implementado | Capacidad por masa (`CargoHold`), no por unidades. |
| Almacén de estación | Implementado | `StationStorage` para descarga, venta y estación personal. |
| Persistencia de nave | Implementado | Save v2 y formato legado. |
| Personalización visual amplia | Parcial | Hay clases, colores y módulos; no hay editor completo de apariencia. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Ship` | Posición, rumbo, velocidad, partes, combustible, disparo, minado y dibujo. |
| `ShipClassDef` | Datos de clase: tamaño, forma y rasgos. |
| `ShipModuleDef` | Catálogo de módulos y efecto real (autopiloto, etc.). |
| `ShipLoadout` | Ranuras instaladas, niveles y consultas como `hasAutopilot()`. |
| `Upgrades` | Niveles comprados, monedas, fórmulas de coste y mejoras. |
| `CargoHold` | Masa máxima, entradas y validación de capacidad. |
| `Cargo` / `Commodity` | Entrada de carga y definición de mineral. |
| `StationStorage` | Almacén persistente por estación. |

## 4. Flujo

1. `GameSession` crea la nave del sector con `Upgrades` y `ShipLoadout`.
2. La extracción y el comercio añaden `Cargo` a `CargoHold`.
3. La masa de la bodega entra en `MassModel` y modula aceleración/consumo.
4. Las reparaciones, mejoras y módulos se aplican en `StationScreen`/`Upgrades`.
5. El guardado serializa partes, combustible, carga y loadout.

## 5. Decisiones aplicables

- **D-010:** varias naves con modelos y capacidades distintas; personalización
  por piezas, componentes, propulsión, radares, minería y módulos. El piloto
  automático puede ser un módulo instalable.

## 6. Tareas relacionadas

T-103 (loadout), T-301…T-306 (carga y masa), T-1101 (clases), T-1109 (módulos).

## 7. Pruebas existentes

`01_logic` (SelfTest de nave, carga y módulos), `11_cargo`, `12_mining`.

## 8. Problemas conocidos

- El módulo `autopilot` se exige para seleccionar blancos (`MyGame.pickTarget`),
  no para volar manualmente.
- Hay 4 ranuras para 5 módulos; el jugador debe elegir. Retirar un módulo no
  reembolsa créditos.
- La capacidad se mide en masa; cambiar a unidades exigiría revisar comercio,
  minería y balance.
- No hay editor visual completo de nave.

## 9. Dependencias

Movimiento y combustible, daños y reparaciones, minería, comercio, estaciones,
persistencia e interfaz.


## 10. Propuesta UPD-006 (no implementada)

`UPD-006` propone, pendiente de aprobación global:

- Mercado de naves de chatarra, usadas y nuevas, con condición, desgaste,
  componentes, depósito, eficiencia, masa, carga, módulos y precio reales.
- Inspección de naves usadas y paquetes de naves nuevas.
- Compra personalizada: nave vieja reparada, combustible y módulos.
- Separación de capacidad de depósito, eficiencia de propulsión, masa base y masa
  de carga; repostaje por unidad suministrada.
- Ranuras visuales con tipo, posición y compatibilidad.
- Separación de `AUTOPILOT` (minería) y `DOCKING_AUTOPILOT` (estacionamiento).

Decisiones: `D-019`, `D-020`, `D-022`. No se ha modificado el código de nave.
