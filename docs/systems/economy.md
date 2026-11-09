# Sistema — Comercio y economía

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `Market.java`, `PriceModel.java`, `TradeService.java`, `MarketScreen.java`, `EconomySim.java`, `StationStorage.java`, `Upgrades.java` |
| Documentación | `MASTER_PLAN.md` §9, §13.4; `arquitectura.md` §Mercado y precios, §Comercio, §Simulación económica, §Venta en estación; `pruebas.md` §Gate de la Etapa 6 |
| Decisiones | D-002 (economía en estación), D-005 (estación personal con mercado) |
| Tareas | T-601…T-606 |
| Pruebas | `01_logic`, `14_market` |

## 1. Descripción

Mercado local por estación con stock, precio objetivo, spread y compraventa
validada. `EconomySim` produce y deriva precios con el tiempo. El jugador puede
vender mineral y comprar suministros; la estación personal puede ofrecer mercado
si construye el módulo correspondiente.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Mercado local | Implementado | `Market` con stock y precios por estación. |
| Modelo de precios | Implementado | `PriceModel`: fórmula y spread. |
| Compraventa | Implementado | `TradeService` valida monedas, stock y bodega. |
| Pantalla de mercado | Implementado | `MarketScreen` con confirmación. |
| Simulación económica | Implementado | `EconomySim` con producción y deriva. |
| Almacén y venta | Implementado | `StationStorage` y venta en estación. |
| Mercado de estación personal | Implementado | Requiere el módulo de mercado (T-908). |
| Comercio entre jugadores | No implementado | Sin online ni servidores. |

## 3. Ruta mínima de lectura

| Archivo | Qué leer |
|---|---|
| `Market.java` | stock, objetivo y precios |
| `PriceModel.java` | fórmula, spread y actualización |
| `TradeService.java` | compraventa y validación |
| `MarketScreen.java` | selección, confirmación y mensajes |
| `EconomySim.java` | tick, producción y deriva |
| `StationStorage.java` | almacén y venta |
| `tools/tests/14_market.sh` | flujo en dispositivo |

No leas `SelfTest.java` completo: busca los bloques de mercado y economía.

## 4. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Market` | Stock, objetivo y precios de una estación. |
| `PriceModel` | Fórmula de precio, spread y actualización. |
| `TradeService` | Compra/venta, validación y actualización de stock. |
| `MarketScreen` | Interfaz, selección y confirmación. |
| `EconomySim` | Tick económico, producción y deriva. |
| `StationStorage` | Almacén y venta. |
| `Upgrades` | Monedas y niveles de mejora. |

## 5. Flujo

1. `EconomySim` actualiza stock y precios con el reloj.
2. `MarketScreen` muestra ofertas y precios.
3. `TradeService` valida y ejecuta la operación.
4. El resultado se guarda con `saveRun()`.
5. La estación personal puede habilitar mercado al construir su módulo.

## 6. Decisiones aplicables

- **D-002:** la economía principal está en estaciones físicas; la pausa no es
  una tienda alternativa.
- **D-005:** la estación personal nace vacía; sus servicios dependen de módulos.

## 7. Tareas relacionadas

T-601 (mercado), T-602 (precios), T-603 (comercio), T-604 (interfaz), T-605
(almacén/venta), T-606 (simulación).

## 8. Pruebas existentes

`01_logic` (fórmulas, compraventa y almacén), `14_market` (flujo en dispositivo).

## 9. Problemas conocidos

- El mercado es local; no hay comercio entre estaciones ni entre jugadores.
- Los precios dependen de `EconomySim`; no duplicar fórmulas en documentos.
- La interfaz de mercado espera confirmación; las pruebas deben cubrir
  `CONFIRMAR`/`CANCELAR`.

## 10. Dependencias

Nave y carga, estaciones, estación personal, contratos, persistencia y HUD.


## 11. Propuesta UPD-006 (no implementada)

`UPD-006` propone, pendiente de aprobación global:

- Mercado de naves de chatarra, usadas y nuevas, con ofertas e inspecciones.
- Paquetes de naves nuevas y compra personalizada.
- Presupuesto inicial de 1.200 CR y regla anti-bloqueo.
- Combustible por capacidad, eficiencia, masa y repostaje por unidad; el código
  actual ya calcula `refillCost(missingFuel)` por unidad.
- Energía de estación para el campo de contención como recurso futuro.

Decisiones: `D-018`, `D-020`. No se ha modificado el código de economía.
