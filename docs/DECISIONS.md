# REGISTRO DE DECISIONES — GalacticIndustry

**Última actualización:** 2026-11-08T15:00:00-06:00  
**Fuente canónica de estado:** `docs/IMPLEMENTATION_TRACKER.json`  
**Plan maestro:** `docs/MASTER_PLAN.md`  
**Preguntas para conversar con otro agente:** `docs/PENDING_DECISIONS.md`

Este archivo explica el **contexto** y la **respuesta** de cada decisión. El estado operativo vive en el tracker. Si hay discrepancia, el tracker manda.

## Estados

| Estado | Significado |
|---|---|
| `PENDIENTE` | El usuario todavía no ha decidido. |
| `APROBADA` | El usuario aprobó una opción; puede implementarse cuando toque. |
| `RECHAZADA` | El usuario rechazó la propuesta. |
| `APLAZADA` | Se decidió conscientemente no implementarla todavía. |
| `TÉCNICA RESUELTA` | Decisión de arquitectura de bajo nivel, revisable por el usuario. |

## Resumen

| ID | Decisión | Estado | Respuesta |
|---|---|---|---|
| D-001 | Muerte permanente, mundo persistente y nave legado | **APROBADA** | Muerte permanente del personaje en el juego normal. No hay reaparición del mismo personaje. Se puede crear un personaje nuevo, que entra en el mism... |
| D-002 | Retirada de la tienda económica de la pausa | **APROBADA** | Eliminar la tienda económica del menú de pausa. Compras, ventas, mejoras, reparaciones y servicios económicos solo en estaciones espaciales físicas... |
| D-003 | Láser, misiles y rayo tractor | **APROBADA** | Separar tres sistemas: (1) láser minero como extracción principal, controlada y eficiente; (2) misiles como destrucción/fragmentación rápida, con m... |
| D-004 | Viaje warp libre y streaming del universo | **APROBADA** | Viaje libre mediante propulsión warp, sin puertas de salto obligatorias. Universo/mapa muy grande con carga dinámica de sectores/regiones: cargar l... |
| D-005 | Estación personal vacía y modular | **APROBADA** | La primera estación personal empieza completamente vacía: una plataforma sencilla, básicamente un cuadrado donde el jugador puede colocar y acoplar... |
| D-006 | Interiores de estaciones y naves | **APROBADA** | A largo plazo el personaje podrá caminar dentro de estaciones espaciales, estaciones construidas por el jugador e interiores de naves, abandonando ... |
| D-007 | Cantidad de agentes y simulación offline | **TÉCNICA RESUELTA** | Se adopta la recomendación como valor inicial ajustable por telemetría. |
| D-008 | Formato y ubicación del guardado | **TÉCNICA RESUELTA** | Se adopta JSON local con backup; no se usa red. |
| D-009 | Audio, música y ambientación | **APROBADA** | Experiencia audiovisual completa: música ambiental espacial, ambientes tranquilos de exploración y música para distintas situaciones; efectos para ... |
| D-010 | Naves múltiples, módulos y personalización | **APROBADA** | Varias naves con modelos, formas, características y capacidades distintas, no simples variaciones visuales. Personalización mediante colores/aparie... |
| D-011 | Galaxias reales y ficticias | **APROBADA** | Usar galaxias reales, empezando por la Vía Láctea y añadiendo otras galaxias conocidas; también se pueden incorporar galaxias ficticias cuando conv... |
| D-012 | Facciones, combate y online futuro | **APLAZADA** | Mantener la prioridad en minería, exploración, supervivencia, economía y desarrollo de naves/estaciones. No implementar ahora IA hostil ni un siste... |
| D-013 | Actualizaciones y revisiones | **APROBADA** | El plan maestro queda como histórico. Los cambios posteriores se registran como actualizaciones `UPD-XXX` en `docs/updates/` y en la clave `updates` del tracker. Cada actualización requiere aprobación explícita antes de implementarse. Las tareas `UPD-XXX-TNN` no alteran el progreso base. |
| D-014 | Coordinación concurrente y escrituras seguras | **APROBADA** | Varias sesiones pueden trabajar a la vez sin coordinador permanente. Cada una reserva su tarea (`assignedSessionId`), actualiza el tracker con un plan validado y `expectUpdatedAt`, y usa un bloqueo `flock` breve con escritura atómica. Las sesiones nuevas viven en `docs/sessions/S-XXX.md`. Los commits no son automáticos. |
| D-015 | Identidad visual de simulador espacial industrial | **APROBADA** | Interfaz de simulador espacial industrial: grafito y acero, ámbar de seguridad, cian de instrumentación, marcos de consola, botones con bisel e iconos geométricos. Sin cambios de mecánicas ni recursos nuevos. |
| D-016 | Versiones de presentación y dashboard dinámico | **APROBADA** | La implementación inicial es la versión visual `0.1`; cada actualización `UPD-XXX` tiene una etiqueta `displayVersion` estable. Una única plantilla dinámica con selector y línea de tiempo comparte la selección; el contenido, el progreso, las etapas, las tareas y el historial corresponden solo a la versión seleccionada. La numeración no cambia IDs internos. |

---

## D-001 — Muerte permanente, mundo persistente y nave legado

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-208, T-209, T-210, T-506

### Respuesta

Muerte permanente del personaje en el juego normal. No hay reaparición del mismo personaje. Se puede crear un personaje nuevo, que entra en el mismo mundo persistente, no en una copia reiniciada. La nave del personaje anterior permanece en las coordenadas exactas de la muerte, junto con los objetos/restos/recursos que sigan existiendo según el estado real del mundo; el nuevo personaje puede viajar hasta allí y encontrarla. La nave nodriza no es punto automático de reaparición. Durante desarrollo y pruebas debe existir un modo especial de recuperación/continuación para no perder el personaje, separado del comportamiento normal.

### Contexto

La nave nodriza no debe ser un punto de reaparición automático. Hay que decidir qué ocurre al destruirse la nave o morir el piloto.

### Opciones consideradas

- Arcade actual: se pierde la partida y se conservan monedas/mejoras.
- Cápsula de escape + rescate a la estación más cercana, con pérdida de carga/nave.
- Seguro: pagar prima y recuperar parte de la nave/carga.
- Permadeath de perfil.

### Recomendación inicial (histórica; prevalece la respuesta)

Cápsula de escape + rescate a la estación más cercana; el piloto sobrevive, se pierde la carga y la nave queda destruida o recuperable con coste. Las mejoras de cuenta se conservan en la primera versión.

### Impacto

Afecta a la Etapa 2 (asfixia) y a la Etapa 5 (rescate). No bloquea la Etapa 1.

### Implementación actual (T-208)

- La partida termina por casco 0 o por piloto a 0 HP/asfixiado.
- Al morir se guarda `legacy.json` con el mundo y la nave; el botón
  `NUEVO PERSONAJE` carga ese mismo mundo y deja la nave anterior como resto.
- No hay reaparición automática en la nave nodriza.
- El modo de recuperación de desarrollo se separa en T-210 y no afecta al juego
  normal.
- **Rescate de combustible (T-506):** no revive al piloto. Cuesta 100 créditos
  (o todo el saldo) y 25 % de la carga (50 % si no se pudo pagar), tarda 3 s y
  deja la nave junto a la estación con 20 de combustible.

---

## D-002 — Retirada de la tienda económica de la pausa

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-504

### Respuesta

Eliminar la tienda económica del menú de pausa. Compras, ventas, mejoras, reparaciones y servicios económicos solo en estaciones espaciales físicas. La pausa no es una tienda alternativa.

### Contexto

La visión exige que la economía principal esté en la estación, pero la tienda de pausa actual es cómoda y los tests dependen de ella.

### Opciones consideradas

- Mantener la tienda de pausa.
- Retirarla y dejar solo la estación.
- Dejar servicios de emergencia limitados y caros.

### Recomendación inicial (histórica; prevalece la respuesta)

Dejar servicios de emergencia limitados durante la transición y retirar la economía principal de la pausa cuando la estación esté validada.

### Impacto

Afecta a la Etapa 5 y a los tests 02/03.

### Implementación actual (T-504)

- La pausa solo ofrece `CONTINUAR` y `MENU`; no hay tienda.
- El menú principal solo ofrece jugar/continuar.
- Compras, ventas, combustible, O2, reparaciones y mejoras se hacen en
  `StationScreen`, tras acoplar en la nodriza.
- La antigua fundición `FUNDIR` también se retiró; vender en la estación paga
  precio base.
- Los tests usan `station_service_test` para las acciones económicas.

---

## D-003 — Láser, misiles y rayo tractor

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-406, T-409, T-410

### Respuesta

Separar tres sistemas: (1) láser minero como extracción principal, controlada y eficiente; (2) misiles como destrucción/fragmentación rápida, con mineral disperso que puede perderse; (3) rayo tractor para atraer fragmentos, con mejoras de alcance, velocidad y capacidad. No fijar porcentajes definitivos de eficiencia ni pérdida; se determinarán por pruebas y balance.

### Contexto

Los misiles son la mecánica actual de destrucción; la minería láser debe ser la principal.

### Opciones consideradas

- Eliminar los misiles del bucle de minería.
- Mantenerlos como herramienta secundaria con rendimiento reducido.
- Convertirlos en arma de combate futura.

### Recomendación inicial (histórica; prevalece la respuesta)

Mantenerlos como herramienta secundaria: rompen rocas y recuperan ~35 % del mineral. No dan monedas.

### Impacto

Afecta a la Etapa 4 y al autopiloto.

### Implementación actual (T-406)

- Los misiles destruyen o fragmentan la roca y dispersan parte de su mineral.
- La dispersión usa `FRAGMENT_SHARE = 0.60` y `FRAGMENT_LOSS = 0.25` como
  valores iniciales de balance; el resto queda en escombros.
- Los fragmentos no entran en la bodega solos: se recogen al acercarse. El rayo
  tractor llega en T-410 y la persistencia de fragmentos en T-409.
- No se entregan monedas.

---

## D-004 — Viaje warp libre y streaming del universo

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-804, T-805, T-806, T-808

### Respuesta

Viaje libre mediante propulsión warp, sin puertas de salto obligatorias. Universo/mapa muy grande con carga dinámica de sectores/regiones: cargar lo cercano, descargar o simplificar lo lejano, sin mantener todo el universo cargado. Preparar escalas entre sistemas y galaxias. El warp se integra con combustible, energía y equipamiento; viaje libre no significa combustible ilimitado. Las puertas pueden existir como atajos, no como requisito.

### Contexto

La visión pide varias galaxias, pero no una simulación astronómica ni vuelo real a escala.

### Opciones consideradas

- Puertas de salto entre nodos.
- Warp libre con coste por distancia.
- Rutas/lanes con encuentros.
- Vuelo real en 2D.

### Recomendación inicial (histórica; prevalece la respuesta)

Puertas de salto para sistemas y una puerta mayor para galaxias; sin vuelo real entre sistemas.

### Impacto

Afecta a la Etapa 8 y al mapa.

### Implementación actual (T-804)

- `TravelService` viaja sin puertas; el coste depende de distancia, motor y una
  puerta opcional (`JumpGate`, factor 0,5).
- Requiere motor warp de nivel ≥1, combustible y energía.
- El destino se carga desde el streaming y la nave conserva carga, daños,
  combustible restante y energía.
- El sector de origen queda como snapshot y se puede volver a él.

---

## D-005 — Estación personal vacía y modular

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-901, T-902, T-903, T-904, T-905, T-906, T-907, T-908

### Respuesta

La primera estación personal empieza completamente vacía: una plataforma sencilla, básicamente un cuadrado donde el jugador puede colocar y acoplar su nave. No incluye almacén, reparación, mercado ni otros servicios de serie. El jugador construye y amplía módulos progresivamente (almacenes, talleres, refinerías, mercados, producción y otros). La arquitectura debe ser modular y extensible.

### Contexto

La estación personal puede crecer mucho; hay que acotar la primera versión.

### Opciones consideradas

- Solo almacén y reparación.
- Almacén, reparación, refinería y mercado.
- Añadir hábitat, defensa y astillero desde el inicio.

### Recomendación inicial (histórica; prevalece la respuesta)

Primera versión con almacén, reparación, refinería y mercado; hábitat como preparación de interiores.

### Impacto

Afecta a la Etapa 9.

---

## D-006 — Interiores de estaciones y naves

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-1001, T-1002, T-1003, T-1004, T-1005, T-1006, T-1007

### Respuesta

A largo plazo el personaje podrá caminar dentro de estaciones espaciales, estaciones construidas por el jugador e interiores de naves, abandonando los controles de pilotaje para interactuar. No implementar en las primeras etapas, pero la arquitectura debe preverlo. La Etapa 10 empieza por la estación propia; naves y estaciones no propias quedan como extensión posterior.

### Contexto

Los interiores son costosos y no deben adelantarse.

### Opciones consideradas

- No implementarlos.
- Solo estación propia.
- Estación propia y algunas naves.
- Todas las estaciones y naves.

### Recomendación inicial (histórica; prevalece la respuesta)

Solo estación propia en la Etapa 10; el resto, futuro.

### Impacto

Afecta a la Etapa 10; puede posponerse sin bloquear el resto.

---

## D-007 — Cantidad de agentes y simulación offline

**Estado:** `TÉCNICA RESUELTA`  
**Decidido por:** Arquitectura del plan maestro  
**Fecha:** 2026-11-07  
**Tareas afectadas:** T-705, T-707

### Respuesta

Se adopta la recomendación como valor inicial ajustable por telemetría.

### Contexto

La economía autónoma debe ser ligera en Android.

### Opciones consideradas

- 8 agentes, tick 2-5 s, offline 12 h.
- 16-24 agentes, más detalle.
- Cientos de agentes agregados.

### Recomendación inicial (histórica; prevalece la respuesta)

8 iniciales, 24 máximo, tick 2-5 s, offline 12 h en pasos de 60 s.

### Impacto

Afecta a la Etapa 7 y al rendimiento.

---

## D-008 — Formato y ubicación del guardado

**Estado:** `TÉCNICA RESUELTA`  
**Decidido por:** Arquitectura del plan maestro  
**Fecha:** 2026-11-07  
**Tareas afectadas:** T-104, T-605, T-803

### Respuesta

Se adopta JSON local con backup; no se usa red.

### Contexto

El guardado actual es texto en SharedPreferences y no tiene versión.

### Opciones consideradas

- Seguir con SharedPreferences.
- Archivo JSON local con backup.
- Base de datos local.

### Recomendación inicial (histórica; prevalece la respuesta)

Archivo JSON local en almacenamiento privado, escritura atómica y backup; perfil pequeño puede seguir en Preferences.

### Impacto

Afecta a la Etapa 1 y a todas las siguientes.

---

## D-009 — Audio, música y ambientación

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-1104, T-1108

### Respuesta

Experiencia audiovisual completa: música ambiental espacial, ambientes tranquilos de exploración y música para distintas situaciones; efectos para motores, warp, láseres, misiles, explosiones, impactos, recuperación de minerales, alarmas, acoplamiento, maquinaria de estaciones, comercio y módulos. Se desarrolla en refinamiento sin bloquear las mecánicas fundamentales.

### Contexto

El juego no tiene audio; no es requisito de la visión inicial, pero ayuda a la identidad.

### Opciones consideradas

- Sin audio.
- Set mínimo (motor, láser, impactos, alarmas, acople).
- Banda sonora y voces.

### Recomendación inicial (histórica; prevalece la respuesta)

Set mínimo local en Etapa 11, con opción de silencio.

### Impacto

Afecta a la Etapa 11 y al tamaño del APK.

### Implementación actual (T-1104)

- `AudioService` con WAV locales en `assets/audio/`.
- Efectos para motor, láser, misil, explosión, impacto, recogida, alarma,
  acople, comercio, módulo y warp.
- Silencio global persistido y botón en la pausa.
- La música/ambientes se implementan en T-1108 con dos piezas locales
  (exploración y tensión) y mezcla suave; el silencio de música es independiente.

---

## D-010 — Naves múltiples, módulos y personalización

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-103, T-1101, T-1109

### Respuesta

Varias naves con modelos, formas, características y capacidades distintas, no simples variaciones visuales. Personalización mediante colores/apariencia, piezas, componentes intercambiables, herramientas, propulsión, radares, navegación, minería y módulos especializados con funciones reales. El piloto automático puede ser un módulo instalable, no una capacidad innata de todas las naves. Preparar arquitectura de ranuras/módulos y especializaciones (minería, exploración, transporte, etc.).

### Contexto

La visión menciona adquirir nuevas naves, pero no es necesario en las primeras etapas.

### Opciones consideradas

- Una sola nave para siempre.
- Varias naves compradas y seleccionables.
- Flota con gestión avanzada.

### Recomendación inicial (histórica; prevalece la respuesta)

Arquitectura preparada para varias naves; implementación real en Etapa 8/11.

### Impacto

Afecta a ShipLoadout y a la estación.

---

## D-011 — Galaxias reales y ficticias

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-806

### Respuesta

Usar galaxias reales, empezando por la Vía Láctea y añadiendo otras galaxias conocidas; también se pueden incorporar galaxias ficticias cuando convenga. No se reproduce exactitud astronómica: prioridad a un universo jugable, interesante y amplio.

### Contexto

La visión propone una galaxia inicial inspirada en la Vía Láctea; el nombre y el contenido son creativos.

### Opciones consideradas

- Vía Láctea real.
- Galaxia ficticia inspirada.
- Nombre genérico provisional.

### Recomendación inicial (histórica; prevalece la respuesta)

Galaxia ficticia inspirada en la Vía Láctea; dejar los nombres definitivos para Etapa 8.

### Impacto

Afecta a datos y textos, no a la arquitectura.

---

## D-012 — Facciones, combate y online futuro

**Estado:** `APLAZADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-07T22:30:00-06:00  
**Tareas afectadas:** T-107, T-1103

### Respuesta

Mantener la prioridad en minería, exploración, supervivencia, economía y desarrollo de naves/estaciones. No implementar ahora IA hostil ni un sistema completo de facciones. A futuro: atacar/saquear naves, piratas, NPC hostiles y pacíficos, facciones de pilotos/jugadores, expediciones conjuntas, relaciones y cooperación. La arquitectura no debe cerrar la posibilidad de una modalidad online, pero no se implementa multijugador, servidores ni combate avanzado en esta etapa. La simulación económica actual se centra en minería, transporte y comercio.

### Contexto

La visión no exige combate, pero un universo activo puede beneficiarse de facciones y peligro.

### Opciones consideradas

- Sin facciones ni combate.
- Facciones económicas sin combate.
- Facciones + piratas + combate.

### Recomendación inicial (histórica; prevalece la respuesta)

Aplazar; priorizar minería, comercio, estaciones y economía. Si se añade, empezar por facciones económicas.

### Impacto

Afecta a Etapa 11 y al alcance.

---

## D-013 — Sistema de actualizaciones y revisiones

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T12:00:00-06:00  
**Tareas afectadas:** ninguna del plan base  
**Actualizaciones afectadas:** UPD-001

### Respuesta

Mantener `docs/MASTER_PLAN.md` como plan histórico y crear actualizaciones
complementarias versionadas (`UPD-XXX`) para correcciones, mejoras, cambios de
diseño, nuevas características, reequilibrios y refactorizaciones posteriores.
El tracker sigue siendo la fuente canónica del estado; las tareas de
actualización se guardan dentro de `updates[].tasks` con IDs `UPD-XXX-TNN` y no
alteran el progreso del plan base.

Una actualización debe pasar por propuesta, revisión y aprobación explícita antes
de implementarse. El panel ofrece un expediente de revisión descargable y un
formulario de propuesta; ninguno escribe directamente en el proyecto. El
navegador solo genera descargas y borradores temporales.

### Contexto

El plan base está terminado. El usuario quiere seguir evolucionando el juego sin
reabrir etapas ni reescribir tareas completadas, y necesita un flujo repetible
para revisar lo implementado, criticarlo y convertirlo en un plan nuevo.

### Opciones consideradas

- Reabrir el plan maestro y añadir tareas nuevas a las etapas existentes.
- Crear un segundo tracker independiente para las actualizaciones.
- Ampliar el tracker con una clave `updates` y mantener el plan base intacto.

### Recomendación inicial (histórica; prevalece la respuesta)

Ampliar el tracker con `updates` y `reviewCatalog`, mantener una sola fuente de
verdad y documentar el flujo en `AGENTS.md` y `docs/updates/`.

### Impacto

Afecta al tracker, al validador, al panel y a la documentación de continuidad. No
afecta al código del juego ni al comportamiento jugable. No se modifican IDs ni
estados históricos.

---

## D-014 — Coordinación concurrente y escrituras seguras

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T11:20:00-06:00  
**Tareas afectadas:** ninguna del plan base  
**Actualizaciones afectadas:** UPD-002

### Respuesta

Varias sesiones pueden trabajar a la vez en tareas distintas sin un agente
coordinador permanente. Cada sesión:

- reserva su tarea con `tools/continuity.py claim` y queda registrada en
  `assignedSessionId`;
- actualiza el tracker con un plan JSON validado, usando `expectUpdatedAt` para
  detectar conflictos;
- usa un bloqueo `flock` breve sobre `docs/.continuity.lock`, liberado por el
  kernel al terminar el proceso;
- escribe el tracker y los derivados de forma atómica (temporal + `fsync` +
  `os.replace`);
- documenta su sesión en `docs/sessions/S-XXX.md`;
- sincroniza el panel con `tools/sync-dashboard.py`;
- no hace commits automáticos ni revierte cambios ajenos.

El bloqueo no cubre la implementación completa, solo la lectura-modificación-
escritura de archivos compartidos. Los cambios de tareas distintas se conservan;
los de la misma tarea producen un conflicto explícito.

### Contexto

La infraestructura anterior permitía una sola tarea `EN_PROGRESO` y no tenía
protección verificable para escrituras simultáneas. El usuario pidió que varios
agentes pudieran trabajar en paralelo sin perder cambios ni depender de un
coordinador.

### Opciones consideradas

- Un agente dedicado a mantener el tracker, el índice y el panel.
- Bloquear todo el proyecto durante la implementación.
- Bloqueo breve por operación, escritura atómica y responsabilidad distribuida.

### Recomendación inicial (histórica; prevalece la respuesta)

La tercera opción: herramientas ligeras en `tools/`, sin servidores ni bases de
datos, con sesiones por archivo y validación antes de escribir.

### Impacto

Afecta a `AGENTS.md`, `tools/continuity.py`, `tools/continuity_io.py`,
`tools/sync-dashboard.py`, el tracker, `docs/INDEX.md`, las fichas de sistema,
`docs/STATUS.md`, `docs/CONCURRENCY.md` y `docs/sessions/`. No afecta al código
del juego ni al balance.

---

## D-015 — Identidad visual de simulador espacial industrial

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T12:40:00-06:00  
**Tareas afectadas:** ninguna del plan base  
**Actualizaciones afectadas:** UPD-003

### Respuesta

La interfaz adopta una identidad de **simulador espacial industrial**, no de
arcade:

- superficies de grafito azulado y acero, con profundidad mediante biseles,
  sombras y marcos de consola;
- ámbar de seguridad como acento principal y cian de instrumentación para datos
  técnicos;
- texto hueso, gris y apagado para una jerarquía de tres niveles;
- botones rectangulares con bisel, sombra y pestaña de acento, sin brillos neón;
- iconos geométricos reutilizables (mineral, carga, combustible, O2, casco,
  energía, estación, warp, alerta, engranaje);
- cabecera, lista y pie consistentes en menú, HUD, estación, mercado, mapa,
  gestión e interiores.

Las mecánicas, el balance, el guardado y la telemetría no cambian. Los recursos
existentes se conservan; la identidad se construye con primitivas de dibujo y la
fuente ASCII actual.

### Contexto

Las pantallas funcionaban, pero mezclaban azules saturados, botones redondeados
tipo arcade, etiquetas desalineadas y bloques de acción demasiado juntos. El
juego evoluciona hacia minería, exploración, supervivencia, comercio y gestión de
estaciones, y necesitaba una presentación coherente con ese oficio.

### Opciones consideradas

- Corregir solo los solapes y mantener la paleta anterior.
- Sustituir la interfaz por imágenes y un atlas nuevo.
- Crear un sistema visual compartido con primitivas y tokens de color.

### Recomendación inicial (histórica; prevalece la respuesta)

La tercera opción: un sistema ligero en `Visuals` y `UiKit`, sin dependencias ni
recursos nuevos, aplicado a todas las pantallas.

### Impacto

Afecta a `Visuals.java`, `UiKit.java`, `OverlayRenderer.java`, `PlayHud.java`,
`StationScreen.java`, `MarketScreen.java`, `MapScreen.java`,
`StationManagementScreen.java`, `MyGame.java`, las capturas de diseño, la ficha
`docs/systems/ui.md` y la documentación de pruebas. No afecta a mecánicas ni
datos de partida.

---


## D-016 — Versiones de presentación y dashboard dinámico

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T15:00:00-06:00  
**Tareas afectadas:** ninguna del plan base  
**Actualizaciones afectadas:** UPD-005

### Respuesta

El panel de continuidad representa el desarrollo como versiones visuales. La
implementación inicial es la versión `0.1`; cada actualización `UPD-XXX` recibe
una etiqueta `displayVersion` estable (`0.2`, `0.3`, …). La numeración es solo
de presentación: los IDs internos de actualizaciones, tareas y sesiones no
cambian.

El panel mantiene una sola página `docs/dashboard/index.html` y una única
plantilla dinámica. Un selector horizontal y una línea de tiempo comparten la
selección. Solo se renderiza el contenido de la versión seleccionada: cabecera,
progreso, etapas o grupos, tareas, historial y pendientes. Las etapas `S0`–`S11`
pertenecen únicamente a la versión `0.1`; las actualizaciones sin `groups`
muestran una lista plana. La información global del proyecto vive en una sección
independiente.

### Contexto

El panel anterior presentaba el plan maestro como bloque principal y apilaba
todas las actualizaciones. La página crecía con cada versión y no permitía
responder rápido qué versión estaba seleccionada, qué objetivo tenía, cuánto
progreso llevaba y qué quedaba pendiente. El tracker ya contenía los datos
necesarios.

### Opciones consideradas

- Mantener una página continua con todas las actualizaciones apiladas.
- Crear un HTML independiente por actualización.
- Usar una única plantilla dinámica alimentada por el tracker, con selector y
  línea de tiempo.

### Recomendación inicial (histórica; prevalece la respuesta)

La tercera opción: una sola página, una sola plantilla y metadatos mínimos de
presentación. Sin frameworks, servidores ni páginas por actualización.

### Impacto

Afecta a `docs/dashboard/index.html`, `dashboard.css`, `dashboard.js`,
`tools/sync-dashboard.py`, `tools/tests/`, `docs/dashboard/README.md`,
`docs/updates/README.md`, `docs/systems/continuity.md`, `docs/INDEX.md` y a los
metadatos de presentación del tracker (`displayVersion`, `objective`,
`meta.initialVersion`, `groups` opcional). No cambia IDs, estados históricos ni
código del videojuego.

---

## Decisiones técnicas durante una tarea

Si un agente necesita resolver una decisión técnica no listada:

1. Elegir la opción más simple compatible con el plan.
2. Documentarla en el log de la sesión.
3. Añadirla a este archivo si tiene impacto duradero.
4. No presentarla como decisión de producto.
5. Si afecta al usuario, crear una decisión pendiente y detenerse.

## Reglas de cambio

- No cambiar IDs de decisiones.
- No convertir una propuesta no aprobada en requisito.
- Si una decisión se divide, crear IDs nuevos.
- Si se reabre una decisión, marcarla `PENDIENTE` y actualizar el tracker.
