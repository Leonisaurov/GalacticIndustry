# Sistema — Facciones, combate y online (aplazado)

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Aplazado** (D-012). No hay código implementado. |
| Código previsto | `Faction.java`, `Reputation.java`, `CombatSystem.java` (no existen todavía) |
| Documentación | `MASTER_PLAN.md` §11.6; `arquitectura.md` §Extensión futura: identidad, facciones y online; `DECISIONS.md` D-012 |
| Decisiones | D-012 |
| Tareas | T-107, T-1103 (pospuesta) |
| Pruebas | No hay pruebas porque no hay implementación. |

## 1. Descripción

Línea futura de facciones, reputación, combate hostil y posible modo online. La
arquitectura no debe cerrar esa puerta, pero no forma parte del alcance actual.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Facciones | Aplazado | Sin clases ni datos. |
| Reputación | Aplazado | Sin clases ni datos. |
| Combate hostil | Aplazado | Solo hay misiles contra asteroides. |
| NPC hostiles | Aplazado | Los agentes actuales son económicos. |
| Multijugador/online | Aplazado | Sin servidores ni red. |
| Arquitectura preparada | Parcial | El universo y los agentes permiten ampliar, pero no hay contrato implementado. |

## 3. Componentes previstos

| Componente | Responsabilidad prevista |
|---|---|
| `Faction` | Identidad, relaciones y territorio. |
| `Reputation` | Reputación del jugador por facción. |
| `CombatSystem` | Reglas de combate hostil. |

No deben presentarse como clases existentes.

## 4. Flujo previsto

Sin definir. Cualquier propuesta debe pasar por una actualización `UPD-XXX` y
una decisión explícita del usuario.

## 5. Decisiones aplicables

- **D-012:** mantener la prioridad en minería, exploración, supervivencia,
  economía y naves/estaciones. No implementar ahora IA hostil, combate avanzado,
  facciones completas, multijugador ni servidores. La arquitectura no debe cerrar
  la posibilidad futura.

## 6. Tareas relacionadas

T-107 (decisión), T-1103 (pospuesta).

## 7. Pruebas existentes

Ninguna. No hay implementación que probar.

## 8. Problemas conocidos

- Riesgo de que la documentación antigua lo presente como implementado. No es
  así: es una línea aplazada.
- No crear clases vacías ni stubs sin una tarea aprobada.

## 9. Dependencias

Ninguna actual. Dependería de economía, universo, agentes y una decisión de
producto nueva.
