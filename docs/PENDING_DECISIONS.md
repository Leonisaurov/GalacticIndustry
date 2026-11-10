# DECISIONES — GalacticIndustry

**Última actualización:** 2026-11-09T23:25:21-06:00  
**Fuente canónica:** `docs/IMPLEMENTATION_TRACKER.json`  
**Detalle narrativo:** `docs/DECISIONS.md`

## Estado

**No hay decisiones pendientes.** `D-026` a `D-030` de `UPD-007`
(renovación gráfica del mundo, sesión S-101) fueron respondidas por el usuario
el 2026-11-08: mundo más realista manteniendo el estilo, solo glows (sin
bloom), sin naves nuevas pero con personalización visible, e interiores
incluidos; `D-029` quedó resuelta técnicamente al cerrarse UPD-006.
**La aprobación global de `UPD-007` sigue `PENDIENTE_APROBACION`**, por lo que
sus 11 tareas continúan `PENDIENTE` y no deben implementarse.

`UPD-008` (rediseño del dashboard), `UPD-009` (scroll interno) y `UPD-010`
(reconstrucción visual) están **COMPLETADAS** en el tracker. Las decisiones
`D-031`…`D-039` están aprobadas o aplicadas; no queda ninguna propuesta de
diseño pendiente de esas actualizaciones. `UPD-011` (indicadores derivados
canónicos y verificación comprobable del panel) fue **APROBADA** por el usuario
y no abre decisiones nuevas: las cinco dudas del planteamiento se cerraron con
evidencia del repositorio (`UPD-005` §6.3-6.4, `dashboard/README.md`,
`sync-dashboard.py` y `AGENTS.md`), no como preferencias de producto. Ver
[`updates/UPD-007.md`](updates/UPD-007.md) §11,
[`updates/UPD-010.md`](updates/UPD-010.md) y
[`updates/UPD-011.md`](updates/UPD-011.md) §10.

Las decisiones de diseño `D-018` a `D-023` de
`UPD-006` fueron respondidas y aprobadas por el usuario el 2026-11-08. `D-024`
(tutorial y campaña) sigue aplazada.

La actualización global `UPD-006` fue aprobada por el usuario el 2026-11-08.
La implementación se inició en la sesión S-100; el estado operativo de cada
tarea vive en el tracker y en `docs/STATUS.md`.

## Decisiones cerradas de UPD-006

| ID | Decisión | Estado | Respuesta aplicada |
|---|---|---|---|
| `D-017` | Contraste obligatorio | Aprobada | Toda propuesta se contrasta con el código real y se clasifica antes de convertirse en tareas. |
| `D-018` | Campo de contención | Aprobada | Exclusión de generación más campo de impactos contundentes, efectos visuales y físicos, naves autorizadas y consumo energético continuo y por impacto. |
| `D-019` | Entrada y acoplamiento | Aprobada | Punto de entrada, corredor y acoplamiento; estacionamiento manual sin módulo y piloto automático de estacionamiento independiente del de minería. |
| `D-020` | Presupuesto y mercado de naves | Aprobada | 1.200 CR; mercado de chatarra, usadas y nuevas; paquetes, inspecciones, compra personalizada, combustible por capacidad/eficiencia/masa y repostaje por unidad; motores modulares futuros. |
| `D-021` | Mapa, escala y viaje | Aprobada | Coordenadas derivadas de datos reales, universo ampliado con streaming, viaje convencional sin warp y warp opcional como ventaja. |
| `D-022` | Brújula y radar | Aprobada | Brújula básica hacia la nodriza más cercana; brújula avanzada y radar circular como módulos independientes, con alcance y conocimiento reales. |
| `D-023` | Interior de la nodriza | Aprobada | Escena dedicada amplia y reutilizable, personaje con sprite/animación y población NPC reducida con movimiento y objetivos básicos. |
| `D-024` | Tutorial y campaña | Aplazada | Se documentan dependencias; no se implementa tutorial ni campaña en `UPD-006`. |
| `D-025` | Motores modulares | Aprobada (documentación) | Se documentan potencia, consumo, eficiencia, masa y compatibilidad como puntos de extensión; no se implementan catálogos. |

## Decisiones históricas cerradas

| ID | Decisión | Estado | Respuesta aplicada |
|---|---|---|---|
| D-001 | Muerte y persistencia | Aprobada | Muerte permanente del personaje. Un personaje nuevo entra en el **mismo mundo persistente**. La nave anterior queda en el lugar de la muerte. La nodriza no es punto de reaparición automático. |
| D-002 | Tienda de pausa | Aprobada | Se elimina la tienda económica de la pausa. Compras, ventas, mejoras, reparaciones y servicios solo en estaciones físicas. |
| D-003 | Minería, misiles y tractor | Aprobada | Láser minero principal; misiles que fragmentan y dispersan mineral; rayo tractor que recupera fragmentos. |
| D-004 | Viaje interestelar | Aprobada | Viaje libre con propulsión warp, sin puertas obligatorias. Universo grande con carga/descarga dinámica. El warp consume combustible y energía y depende de equipamiento. |
| D-005 | Estación personal | Aprobada | La estación inicial es una plataforma vacía y acoplable. El jugador construye módulos progresivamente. |
| D-006 | Interiores | Aprobada a largo plazo | Se podrá caminar dentro de estaciones, estación propia y naves. La Etapa 10 empieza por la estación propia; naves y estaciones ajenas quedan para después. |
| D-007 | Agentes/offline | Técnica resuelta | 8 agentes iniciales, 24 máximo, tick 2/5/60 s, catch-up máximo de 12 h. |
| D-008 | Guardado | Técnica resuelta | JSON local, escritura atómica, backup y migración desde `run2`. Sin red. |
| D-009 | Audio | Aprobada | Música ambiental y efectos para motores, warp, láser, misiles, explosiones, impactos, recogida, alarmas, acople, comercio y módulos. |
| D-010 | Naves y personalización | Aprobada | Varios modelos con formas, características y capacidades distintas. Personalización por piezas, componentes, herramientas, propulsión, radares, navegación, minería y módulos. |
| D-011 | Galaxias | Aprobada | Galaxias reales empezando por la Vía Láctea, más otras reales y ficticias. Sin exactitud astronómica; prioridad a un universo jugable. |
| D-012 | Facciones, combate y online | Aplazada | No se implementa ahora IA hostil, combate avanzado, facciones completas, multijugador ni servidores. |
| D-013 | Actualizaciones y revisiones | Aprobada | El plan maestro queda como histórico. Los cambios posteriores se registran como `UPD-XXX`; cada actualización requiere aprobación explícita. |
| D-014 | Coordinación concurrente | Aprobada | Varias sesiones pueden trabajar a la vez con reserva de tarea, `expectUpdatedAt`, bloqueo `flock` breve y sesiones por archivo. |
| D-015 | Identidad visual | Aprobada | Interfaz de simulador espacial industrial: grafito y acero, ámbar de seguridad, cian de instrumentación, marcos de consola, botones con bisel e iconos geométricos. |
| D-016 | Versiones y dashboard | Aprobada | La implementación inicial es la versión visual `0.1`; cada actualización tiene `displayVersion` estable. Una única plantilla dinámica con selector y línea de tiempo comparte la selección. |

## Tareas que requieren decisión

Las tareas de `UPD-006`, `UPD-008`, `UPD-009` y `UPD-010` están completadas y
no dependen de decisiones `PENDIENTE`. Las 11 tareas de `UPD-007`
(`UPD-007-T01`…`T11`) siguen `PENDIENTE`: sus decisiones ya están respondidas,
pero no pueden iniciarse hasta la aprobación global de esa actualización.

Las 6 tareas de `UPD-011` (`UPD-011-T01`…`T06`) están aprobadas y no requieren
decisión del usuario; su avance operativo se registra en el tracker y en
`docs/STATUS.md`. `D-024` (tutorial y campaña) sigue aplazada por decisión
histórica, no bloquea ninguna tarea de estas actualizaciones.
