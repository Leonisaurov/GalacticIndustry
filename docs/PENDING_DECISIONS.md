# DECISIONES CERRADAS — GalacticIndustry

**Última actualización:** 2026-11-08T03:16:00-06:00  
**Estado:** no hay decisiones pendientes.  
**Fuente canónica:** `docs/IMPLEMENTATION_TRACKER.json`  
**Detalle narrativo:** `docs/DECISIONS.md`

Este archivo se usó para conversar las decisiones con otro agente. Las respuestas ya fueron aplicadas al tracker y al plan maestro. Se conserva como resumen histórico.

## Resumen de respuestas

| ID | Decisión | Estado | Respuesta aplicada |
|---|---|---|---|
| D-001 | Muerte y persistencia | Aprobada | Muerte permanente del personaje en el juego normal. Un personaje nuevo entra en el **mismo mundo persistente**. La nave anterior queda en las coordenadas de la muerte, con los restos/recursos que sigan existiendo. La nave nodriza no es punto de reaparición. Debe existir un modo de desarrollo para recuperarse en pruebas. |
| D-002 | Tienda de pausa | Aprobada | Se elimina la tienda económica de la pausa. Compras, ventas, mejoras, reparaciones y servicios solo en estaciones físicas. |
| D-003 | Minería, misiles y tractor | Aprobada | Tres sistemas: láser minero principal; misiles que fragmentan y dispersan mineral; rayo tractor que recupera fragmentos, con mejoras de alcance, velocidad y capacidad. Sin porcentajes definitivos hasta balance. |
| D-004 | Viaje interestelar | Aprobada | Viaje libre con propulsión warp, sin puertas obligatorias. Universo grande con carga/descarga dinámica de sectores y regiones. El warp consume combustible/energía y depende de equipamiento. |
| D-005 | Estación personal | Aprobada | La estación inicial es una plataforma vacía donde se acopla la nave. No trae almacén, reparación, refinería ni mercado. El jugador construye módulos progresivamente. |
| D-006 | Interiores | Aprobada a largo plazo | Se podrá caminar dentro de estaciones, estación propia y naves. No se implementa en las primeras etapas. La Etapa 10 empieza por la estación propia; naves y estaciones no propias quedan como extensión posterior. |
| D-007 | Agentes/offline | Técnica resuelta | 8 agentes iniciales, 24 máximo, tick 2/5/60 s, catch-up máximo de 12 h. |
| D-008 | Guardado | Técnica resuelta | JSON local, escritura atómica, backup y migración desde `run2`. Sin red. |
| D-009 | Audio | Aprobada | Experiencia audiovisual completa: música ambiental, ambientes de exploración, música para distintas situaciones y efectos para motores, warp, láseres, misiles, explosiones, impactos, recuperación, alarmas, acople, maquinaria, comercio y módulos. |
| D-010 | Naves y personalización | Aprobada | Varios modelos de nave con formas, características y capacidades distintas. Personalización por piezas, componentes, herramientas, propulsión, radares, navegación, minería y módulos. El piloto automático puede ser un módulo instalable. |
| D-011 | Galaxias | Aprobada | Galaxias reales empezando por la Vía Láctea, más otras galaxias reales y ficticias cuando convenga. Sin exactitud astronómica; prioridad a un universo jugable. |
| D-012 | Facciones, combate y online | Aplazada | No se implementa ahora IA hostil, combate avanzado, facciones completas, multijugador ni servidores. La arquitectura no debe cerrar la posibilidad futura de facciones, PvP, saqueo, cooperación y online. La economía actual se centra en minería, transporte y comercio. |

## Tareas de decisión

Ya no hay tareas en estado `REQUIERE_DECISION`.

Las tareas que dependían de estas decisiones fueron reactivadas o pospuestas:

- `T-208`, `T-504`, `T-406`, `T-804`, `T-903`, `T-1001`…`T-1006`, `T-1104`: reactivadas como `PENDIENTE` cuando correspondía.
- `T-1103` (facciones/combate/online): `POSPUESTO`.
- `T-1007` (interiores de naves y estaciones no propias): `POSPUESTO`.
- `T-107` (revisión de extensibilidad online): `PENDIENTE`, no cuenta para el progreso de implementación.

Las Etapas 1, 2, 3, 4 y 5 están completadas. La siguiente tarea de implementación es **T-603 — Crear MarketScreen y flujo de compraventa** (Etapa 6).
