# Sistema — Piloto, oxígeno y supervivencia

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json) · [Plan maestro](../MASTER_PLAN.md)

| Campo | Valor |
|---|---|
| Estado | **Implementado** (con dos limitaciones documentadas abajo) |
| Código | `Pilot.java`, `LifeSupport.java`, `GameSession.java`, `GameWorld.java`, `Ship.java`, `MyGame.java`, `PlayHud.java`, `StationScreen.java`, `Upgrades.java` |
| Documentación | `MASTER_PLAN.md` §4.1, §4.5, §7, §13.2; `arquitectura.md` §Clases, §Daño y reparaciones, §Secuencia de muerte; `pruebas.md` §Balance de supervivencia |
| Decisiones | D-001 (muerte permanente), D-002 (servicios en estación) |
| Tareas | T-102, T-201, T-202, T-203, T-204, T-205, T-206, T-207, T-208, T-209, T-210 |
| Pruebas | `01_logic` (SelfTest), `10_survival`, `05_death`, `08_visual_death` |

## 1. Descripción

El piloto es una entidad separada de la nave. Tiene salud y oxígeno propios; el
casco y los sistemas de la nave no comparten esos valores. El soporte vital
convierte la integridad del casco y la eficiencia del filtro en consumo, fuga y
asfixia. La muerte del piloto termina la partida sin explosión de la nave; la
muerte por casco destruido sí provoca la secuencia de explosión.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Salud y O2 del piloto | Implementado | 100/100 iniciales, persistidos en guardado v2 y formato legado. |
| Consumo de O2 | Implementado | 4 O2/min; filtro base 1,5 O2/min; pérdida neta 2,5 O2/min sin daño. |
| Filtro por integridad de `LIFE_SUPPORT` | Implementado | `partFactor = 0,20 + 0,80 × integridad`; el filtro almacenado (0–100) lo modula. |
| Fugas por casco | Implementado | Proporcionales al casco perdido; la brecha multiplica ×1,5. |
| Brecha crítica | Implementado | Se abre con impacto ≥30 y casco ≤70 %, o con casco ≤20 %. |
| Asfixia | Implementado | 5 s de gracia y después 1 HP/s; a 0 HP el piloto queda `INCAPACITATED`. |
| Muerte por piloto | Implementado | `MyGame.triggerPilotDeath()` cierra la partida sin explosión y deja nave legado. |
| HUD y alertas | Implementado | Barras O2/HP, avisos al 25 %/10 %, texto `FUGA`/`BRECHA`. |
| Reparación de brecha en juego | **No conectada** | `LifeSupport.repairBreach()` solo se usa en `SelfTest`; la estación no la llama y «REPARAR TODO» no limpia `breach`. |
| Recarga de O2 en estación | **Parcial** | `StationScreen` asigna `pilot.oxygen = o2Max` directamente y no usa `Pilot.refillOxygen()`. En el instante de la compra no repone la gracia ni cambia `HYPOXIC` a `OK`, pero el siguiente `updateAsphyxia()` con O2 > 0 los restaura. No revive a un piloto `INCAPACITATED` o `DEAD`. |
| Desgaste del filtro | No implementado | El campo `filter` no baja con el uso; la regeneración del filtro es una tasa de O2, no un depósito que se agote. |
| Bonus por nivel de soporte | No usado | `FILTER_LEVEL_BONUS` existe, pero `LifeSupport.update()` llama a `netOxygenPerMinute(ship, 0)`. |

## 3. Ruta mínima de lectura

Para responder sin leer los archivos completos, localiza estos símbolos y rangos
aproximados (si el código se movió, busca el método):

| Archivo | Qué leer | Líneas orientativas |
|---|---|---|
| `Pilot.java` | constantes, `reset`, `o2AlertLevel`, `updateAsphyxia`, `refillOxygen` | 20-120 |
| `LifeSupport.java` | constantes, `filterEffectiveness`, `netOxygenPerMinute`, `updateLeak`, `onHullDamage`, `update` | 12-120 |
| `GameSession.java` | `update(dt, c)` | 219-223 |
| `GameWorld.java` | `damageShip`, `emitLeak`, `updateLeak` | 690-733 |
| `MyGame.java` | `triggerPilotDeath`, `finishRun`, comprobación `!pilot.alive()` | 745-790, 1550-1660 |
| `PlayHud.java` | avisos de O2, `drawSurvivalText`, barras | 150-170, 388-466 |
| `StationScreen.java` | servicio de O2 | 168-190 |
| `tools/tests/10_survival.sh` | flujo y telemetría del HUD | completo (28 líneas) |
| `tools/tests/05_death.sh` | muerte por piloto y nave legado | completo (73 líneas) |

No hace falta leer `SelfTest.java` completo: busca los bloques `T-201`…`T-207` y
`T-102` si necesitas las aserciones exactas.

## 4. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `Pilot` | Salud, O2, estados, gracia, alertas, asfixia, serialización y `refillOxygen()`. |
| `LifeSupport` | Filtro, fuga, brecha, `filterEffectiveness()`, `netOxygenPerMinute()`, `update()`, `estimatedSeconds()`. |
| `GameSession` | Compone piloto y soporte vital; `update(dt, c)` llama a `sector.update()` y después a `lifeSupport.update()`. |
| `GameWorld` | Impactos (`damageShip`), `onHullDamage()`, `updateLeak()` y partículas de gas (`emitLeak`). |
| `Ship` | Guarda la integridad de `HULL` y `LIFE_SUPPORT`; no guarda salud ni O2 del piloto. |
| `MyGame` | Detecta `!pilot.alive()`, ejecuta `triggerPilotDeath()` y guarda el mundo/nave legado. |
| `PlayHud` | Dibuja O2, HP, `FUGA`/`BRECHA` y avisos. |
| `StationScreen` | Servicio de recarga de O2 y reparación de `LIFE_SUPPORT` como parte de la nave. |

## 5. Flujo y fórmulas verificadas

1. `GameSession.update(dt, c)` actualiza el sector y luego el soporte vital.
2. `GameWorld.update()` recalcula la fuga con `updateLeak(ship)` y emite gas si `leak > 0,01`.
3. Un impacto de asteroide llama a `ship.damage(HULL, base)` y a `lifeSupport.onHullDamage(ship, base)`.
4. `LifeSupport.update()` calcula `(netOxygenPerMinute + leak) / 60` y resta O2; después llama a `Pilot.updateAsphyxia(dt)`.
5. Con O2 a 0: 5 s de gracia y 1 HP/s. La salud a 0 pone `INCAPACITATED`; `Pilot.alive()` pasa a `false` y `MyGame` cierra la partida.
6. El casco a 0 se comprueba aparte (`world.isOver()`): termina la partida con explosión.

Fórmulas clave:

- `netOxygenPerMinute = 4 − 1,5 × filtroEfectivo` (con nivel de soporte 0).
- `filtroEfectivo = (0,20 + 0,80 × integridadLIFE_SUPPORT) × (filter / 100)`.
- `leak = 6 × (1 − casco/maxHull) × (1 + 0,5 si breach)` unidades/min.
- A 50 % de casco: 3 O2/min de fuga; a 20 %: 4,8 O2/min; con brecha, ×1,5.
- Sin daño: 2,5 O2/min → 75 % de O2 tras 10 minutos.

## 6. Decisiones aplicables

- **D-001:** muerte permanente; el personaje no reaparece. Un personaje nuevo
  entra en el mismo mundo y puede recuperar la nave legado. El modo de
  recuperación es solo de desarrollo.
- **D-002:** la recarga de O2 y las reparaciones son servicios de estación, no
  de la pausa.

## 7. Tareas relacionadas

T-102 (piloto separado), T-201 (LIFE_SUPPORT y persistencia), T-202 (consumo y
filtro), T-203 (fugas y brecha), T-204 (asfixia y estados), T-205 (HUD), T-206
(recarga y reparación), T-207 (balance), T-208 (muerte permanente), T-209 (nuevo
personaje y nave legado), T-210 (recuperación de desarrollo).

## 8. Pruebas existentes

- `01_logic` / `SelfTest`: valores iniciales, independencia casco/salud,
  persistencia, tasas a 60 s, filtro 100/50/0 %, fugas a 100/50/20 %, asfixia y
  recarga.
- `10_survival`: HUD con O2 al 24 %, salud 70 %, casco 50 %, brecha y aviso.
- `05_death`: casco a 0, piloto a 0, nuevo personaje, nave legado y recuperación.
- `08_visual_death`: secuencia visual de muerte.

## 9. Problemas conocidos

- Los comentarios de `Pilot`, `LifeSupport` y `GameSession` todavía dicen que el
  consumo, las fugas y la asfixia "se conectan en T-202/T-203/T-204". El código
  ya las implementa; son comentarios históricos obsoletos. No se modificaron
  para no tocar Java en esta actualización.
- El comentario de `LifeSupport.leak` dice "unidades de O2 por segundo", pero el
  cálculo y `update()` lo tratan como unidades por minuto. Prevalece el código.
- `StationScreen` recarga O2 sin pasar por `Pilot.refillOxygen()`: en el instante
  de la compra no repone la gracia ni saca de `HYPOXIC`, aunque el siguiente
  tick con O2 > 0 lo corrige. No revive estados terminales.
- No hay ninguna ruta de juego que llame a `LifeSupport.repairBreach()`; una
  brecha abierta solo desaparece al cargar una partida sin ella o al reiniciar.
  «REPARAR TODO» repara `LIFE_SUPPORT` pero no limpia `breach`, así que la fuga
  ×1,5 persiste tras reparar la nave.
- `FILTER_LEVEL_BONUS` no tiene efecto porque siempre se pasa nivel 0.
- La telemetría y el HUD usan la fuga recalculada; si se cambia el orden de
  `GameSession.update()`, hay que revisar `01_logic` y `10_survival`.

## 10. Dependencias

- **Entrada:** `GameWorld` (impactos), `Ship` (casco y `LIFE_SUPPORT`),
  `Upgrades` (coste de recarga), `StationScreen` (servicio).
- **Salida:** `MyGame` (muerte y guardado), `PlayHud` (HUD), `SaveCodec` /
  `SaveService` (persistencia), `GameWorld` (gas y nave legado).
- **Sistemas relacionados:** daños y reparaciones, estaciones, persistencia,
  interfaz/HUD.
