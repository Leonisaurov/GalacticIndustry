# REGISTRO DE DECISIONES — GalacticIndustry

**Última actualización:** 2026-11-08T15:35:00-06:00  
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
| D-017 | Contraste obligatorio con el código real | **APROBADA** | Toda propuesta se contrasta con la implementación real antes de convertirse en tareas. Se clasifica como implementada, parcial, ausente, incompleta visualmente, inconsistente, planificada, pospuesta o por verificar, con referencias a archivos y métodos reales. |
| D-018 | Campo de contención de la nodriza | **APROBADA** | Exclusión de generación más campo de contención con impactos contundentes, efectos visuales y físicos, naves autorizadas que lo atraviesan y consumo energético continuo y por impacto. La estación conserva sus colisiones. |
| D-019 | Entrada y acoplamiento de la nodriza | **APROBADA** | Punto de entrada, corredor y acoplamiento. Estacionamiento manual sin módulo, exigente pero aprendible, y módulo opcional de piloto automático de estacionamiento, independiente del piloto automático de minería. |
| D-020 | Presupuesto inicial, mercado de naves y economía de equipamiento | **APROBADA** | 1.200 CR. Mercado de chatarra, usadas y nuevas; paquetes de naves nuevas; inspecciones; compra personalizada; combustible por capacidad, eficiencia, masa y repostaje por unidad; motores modulares como evolución futura; configuración inicial viable y anti-bloqueo. |
| D-021 | Mapa de navegación, escala del universo y viaje convencional | **APROBADA** | Coordenadas derivadas de los datos reales. Ampliar dimensiones y distancias manteniendo streaming. Viaje convencional sin warp entre sistemas y galaxias, con tiempos mayores; warp opcional como ventaja. El mapa muestra ambas posibilidades y sus requisitos. |
| D-022 | Brújula y radar | **APROBADA** | Brújula básica de serie hacia la nodriza más cercana; brújula avanzada como módulo independiente; radar circular como módulo independiente, con categorías, filtros, alcance y conocimiento real, sin revelar todo el universo. |
| D-023 | Interior de la nave nodriza | **APROBADA** | Escena interior dedicada y amplia, con arquitectura reutilizable o documentada para homogeneizar con estaciones personales. Personaje con sprite, animación, colisiones e interacción. Población NPC reducida con movimiento y objetivos básicos. |
| D-024 | Tutorial interactivo y campaña narrativa | **APLAZADA** | Se documentan pasos y dependencias, pero no se implementa el tutorial ni una campaña en esta actualización. |
| D-026 | Estilo gráfico del mundo (UPD-007) | **APROBADA** | Mundo más realista y definido conservando el estilo industrial; corregir sectores apagados; todo pulido y orgánico. La UI de D-015 no se reabre. |
| D-027 | Bloom y post-proceso (UPD-007) | **APROBADA** | Solo glows mejorados: mejores texturas y capas aditivas, sin bloom, sin shaders ni FBO. |
| D-028 | Naves y personalización (UPD-007) | **APROBADA** | No agregar contenido: mejorar las naves existentes, diferenciar las que comparten estilo y hacer que la personalización se vea realmente en el casco. |
| D-029 | Secuenciación con UPD-006 | **TÉCNICA RESUELTA** | UPD-006 quedó COMPLETADA el 2026-11-08; no hay archivos en conflicto. Orden interno: efectos antes que sus consumidores. |
| D-030 | Interiores en UPD-007 | **APROBADA** | Incluirlos: pasada gráfica a salas, mobiliario, luz, avatar y NPCs, por cómo quedaron tras UPD-006. |

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

## D-017 — Contraste obligatorio con el código real

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T14:35:00-06:00  
**Tareas afectadas:** ninguna del plan base  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta

Ninguna propuesta se convierte en tarea o cambio de código sin contrastarla
antes con la implementación real. Una descripción del usuario no confirma que
algo no exista: puede estar implementado, parcialmente implementado o funcionar
de otra forma.

Para cada funcionalidad se localizan archivos, clases y métodos; se investiga el
comportamiento real; se consultan fichas y decisiones; se describe el estado
actual; se compara con el objetivo; se identifica la brecha; y se diseña solo lo
necesario. Cada característica se clasifica como `IMPLEMENTADA`, `PARCIAL`,
`AUSENTE`, `INCOMPLETA VISUALMENTE`, `INCONSISTENTE`, `PLANIFICADA`, `POSPUESTA`
o `POR VERIFICAR`, con referencias reales.

### Contexto

El usuario puede describir un mapa como “incompleto” aunque exista una lista de
sectores, o pedir una brújula aunque exista un módulo RADAR con otro efecto. Sin
auditoría, el agente podría reconstruir lo que ya funciona o crear sistemas
paralelos.

### Opciones consideradas

- Interpretar literalmente la descripción y crear tareas desde cero.
- Auditar el código y limitarse a lo que falte.
- Auditar, clasificar y registrar decisiones antes de planificar.

### Recomendación inicial (histórica; prevalece la respuesta)

La tercera: auditar, clasificar y registrar la brecha antes de crear tareas.

### Impacto

Afecta a `AGENTS.md`, `docs/updates/README.md`, todas las propuestas y
revisiones futuras, la documentación de sistemas y las decisiones. No afecta al
código del juego ni al balance.

---

## D-018 — Campo de contención de la nodriza

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T15:35:00-06:00  
**Tareas afectadas:** `UPD-006-T04`, `UPD-006-T05`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta aprobada

Se combina la exclusión de generación con un campo de contención de impactos
contundentes:

- Los asteroides no se generan espontáneamente dentro de la zona protegida.
- Los asteroides que llegan desde fuera impactan contra el campo.
- El impacto debe ser perceptible y contundente, con destellos, ondas de choque,
  fragmentación o desviación física de los objetos; no un simple desplazamiento
  suave.
- Las naves autorizadas pueden atravesar el campo.
- La estructura física de la estación conserva sus propias colisiones.

El campo no es gratuito. Su mantenimiento consume energía de forma continua y
cada impacto puede generar un consumo adicional. La protección requiere
infraestructura y suministro energético constante. Esto se contempla como parte
de la futura economía y administración de estaciones: mantener protegida una
instalación grande tiene costes. En esta fase no se implementa una simulación
económica compleja; solo se definen funcionamiento, consumo y relación con los
recursos de la estación.

### Contexto

La zona segura actual solo evita que se generen asteroides dentro de
`Station.inSafeZone`; las rocas que ya se mueven pueden atravesarla. El usuario
quiere una protección física coherente, no una pared invisible.

### Opciones consideradas

- Exclusión de generación más campo de contención con impactos contundentes.
- Repulsión suave sin impacto.
- Destrucción o neutralización de los objetos al entrar.
- Pared invisible que impide todo movimiento.

### Impacto

Afecta a `ContainmentField`, `Station`, `StationDef`, `GameWorld`, `Effects`,
`Asteroid`, fragmentos, generación, energía de estación y futuros agentes con
posición. Afecta también a `docs/systems/stations.md` y a la futura economía de
estaciones. No afecta al mercado ni a los servicios de estación en esta fase.

---

## D-019 — Entrada y acoplamiento de la nodriza

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T15:35:00-06:00  
**Tareas afectadas:** `UPD-006-T02`, `UPD-006-T06`, `UPD-006-T07`, `UPD-006-T35`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta aprobada

Se adopta un punto de entrada, un corredor de aproximación y un acoplamiento con
dos modalidades:

1. **Estacionamiento manual.** No requiere ningún módulo. El jugador localiza la
   entrada, se aproxima, sigue el corredor, controla velocidad, orienta la nave,
   alinea la trayectoria y ejecuta el acoplamiento. La maniobra debe ser
   relativamente exigente pero aprendible mediante práctica.
2. **Piloto automático de estacionamiento.** Módulo opcional y especializado que
   realiza aproximación, alineación y acoplamiento. Es independiente del piloto
   automático de minería, que automatiza selección, aproximación y extracción de
   asteroides. El jugador puede adquirirlos por separado. El estacionamiento
   manual sigue siendo funcional para quien no invierta en automatización.

El módulo `AUTOPILOT` existente se mantiene por compatibilidad, pero el plan
separa sus responsabilidades: minería/blancos y estacionamiento. La migración de
guardados y la semántica de `MyGame.pickTarget()` se define en la tarea
`UPD-006-T35`.

### Contexto

El acople actual se permite en cualquier punto dentro de `dockRadius` y el piloto
automático apunta al centro geométrico. La bahía dibujada no tiene física.

### Opciones consideradas

- Punto de atraque, corredor y acoplamiento manual más módulo opcional.
- Mantener el radio actual y añadir solo orientación.
- Rayo tractor que atrae la nave a la boca.
- Un único piloto automático que haga minería y estacionamiento.

### Impacto

Afecta a `StationDef`, `Station`, `DockingSystem`, `Autopilot`, `ShipModuleDef`,
`ShipLoadout`, `MyGame`, `StationScreen` y las pruebas de navegación, estación y
módulos.

---

## D-020 — Presupuesto inicial, mercado de naves y economía de equipamiento

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T15:35:00-06:00  
**Tareas afectadas:** `UPD-006-T27`, `UPD-006-T28`, `UPD-006-T29`, `UPD-006-T31`, `UPD-006-T36`, `UPD-006-T37`, `UPD-006-T38`, `UPD-006-T39`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta aprobada

El presupuesto inicial es de **1.200 CR**. El jugador comienza dentro de la nave
nodriza sin ser propietario de una nave. Debe comprar su primera nave y
distribuir el presupuesto entre vehículo, combustible y equipamiento.

#### Mercado de naves

Tres categorías:

- **Chatarra:** naves muy económicas, normalmente deterioradas.
- **Usadas:** naves con distintas condiciones, precios y equipamiento.
- **Nuevas:** naves con características conocidas y precios más definidos.

El mercado de ocasión genera variabilidad, pero las características reales de
cada nave existen en sus datos; no se convierte en azar injusto. Factores:
integridad del casco, estado de componentes, desgaste, capacidad del depósito,
eficiencia de combustible, masa, capacidad de carga, módulos instalados y
precio. Puede haber oportunidades excepcionales o malas inversiones. Se estudian
inspecciones para conocer mejor el estado antes de comprar.

#### Paquetes de naves nuevas

Configuraciones preparadas por comerciantes, cómodas y listas para funcionar,
con precio superior al de comprar componentes por separado y equipamiento
limitado o determinado. El jugador puede comprar un paquete o construir su
propia configuración.

#### Compra personalizada

El jugador puede comprar una nave vieja y distribuir el presupuesto entre
reparaciones, combustible, piloto automático, piloto automático de
estacionamiento, herramientas de minería y otros módulos. Una nave deteriorada
puede ser una buena compra si se invierte bien; la nave nueva no es
automáticamente superior en todas las situaciones.

#### Combustible

Se distinguen:

- capacidad del depósito (máximo almacenado);
- eficiencia de propulsión (combustible necesario para un desplazamiento);
- masa base de la nave;
- masa adicional de la carga;
- consumo efectivo según masa y motores.

El peso del inventario sigue influyendo en el consumo. Una nave puede tener
depósito pequeño y motores eficientes, o depósito grande y motores poco
eficientes.

#### Repostaje

El coste depende de las unidades realmente suministradas. Una nave con depósito
pequeño paga menos para llenarse; si ya tiene combustible, solo paga lo que
falta. No hay tarifa fija por pulsar el botón. El código actual ya calcula
`Upgrades.refillCost(missingFuel)` por unidad faltante, con mínimo 5; el plan
debe conservar ese comportamiento y exponerlo con claridad.

#### Motores modulares futuros

Queda contemplado un sistema futuro de motores con potencias, consumos,
eficiencias y compatibilidades distintas. No se implementan catálogos, modelos
técnicos ni compatibilidades complejas ahora. Las fórmulas iniciales de
combustible, consumo, masa y eficiencia deben diseñarse para poder evolucionar.

#### Equilibrio inicial

Con 1.200 CR debe existir al menos una configuración viable. El jugador puede
elegir entre una nave económica personalizada y una nave nueva mediante paquete,
sin quedar imposibilitado para su primera expedición. Los precios de casco son
iniciales de prueba y se contrastan con la economía actual; no se fijan
definitivamente sin balance.

### Contexto

El juego actual entrega una nave completa y gratuita. No hay precio de casco,
propiedad de nave ni fondo de arranque. La muerte es permanente y las monedas se
conservan por cuenta, así que un nuevo personaje sin nave podría quedar atrapado.

### Opciones consideradas

- Mercado de chatarra/usadas/nuevas con 1.200 CR y anti-bloqueo.
- Solo tres cascos nuevos con precio fijo.
- Entrega de una nave de servicio gratuita cuando no haya saldo.

### Impacto

Afecta a `Upgrades`, `GameSession`, `ShipMarket`, `ShipOffer`, `ShipDealerScreen`,
`EquipmentScreen`, `Ship`, `MassModel`, `SaveCodec`, `RescueService` y al balance
inicial. No cambia los precios existentes de combustible, módulos o mejoras sin
una decisión adicional.

---

## D-021 — Mapa de navegación, escala del universo y viaje convencional

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T15:35:00-06:00  
**Tareas afectadas:** `UPD-006-T15`, `UPD-006-T16`, `UPD-006-T17`, `UPD-006-T18`, `UPD-006-T42`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta aprobada

Se usan coordenadas derivadas de los datos reales del universo. El mapa debe
representar galaxias, sistemas, regiones, estaciones, nave nodriza, otros puntos
de interés, distancias y posibilidades de desplazamiento. Se revisan `mapX` y
`mapY` antes de decidir su uso definitivo; si su significado y contenido son
adecuados, se reutilizan.

Las galaxias y las distancias entre regiones deben ampliarse considerablemente
para que explorar tenga sentido, manteniendo la carga y descarga dinámica de
sectores. No se inventan coordenadas astronómicas ni contenido que no exista en
el universo del proyecto.

#### Viaje convencional y warp

Debe ser posible viajar entre sistemas e incluso galaxias sin propulsión warp.

- **Viaje convencional:** usa la propulsión normal, permite grandes distancias y
  puede requerir tiempos mucho mayores.
- **Viaje warp:** extremadamente rápido o prácticamente instantáneo en
  comparación, requiere equipamiento especializado y tiene costes de energía y
  combustible según su diseño.

El warp es una ventaja tecnológica, no una barrera obligatoria. El mapa
representa ambas posibilidades y muestra sus requisitos.

### Contexto

Solo `SectorDef` tiene `mapX`/`mapY`; `GalaxyDef` y `SystemDef` no tienen
posición. El mapa actual es una lista y el warp exige motor de nivel ≥1.

### Opciones consideradas

- Coordenadas derivadas de datos reales, escala ampliada y viaje convencional.
- Posiciones manuales nuevas.
- Mantener solo la lista de sectores.
- Warp obligatorio para salir del sistema.

### Impacto

Afecta a `SectorDef`, `SystemDef`, `GalaxyDef`, `UniverseState`, `MapScreen`,
`TravelService`, `GameSession`, `SaveCodec` y las pruebas de universo, warp y
mapa. No crea contenido astronómico nuevo.

---

## D-022 — Brújula y radar

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T15:35:00-06:00  
**Tareas afectadas:** `UPD-006-T08`, `UPD-006-T09`, `UPD-006-T10`, `UPD-006-T43`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta aprobada

Se mantienen tres instrumentos separados e independientes:

1. **Brújula básica de serie.** Indica la dirección de la **nave nodriza más
   cercana** al jugador, no solo la de la galaxia de origen. Si el jugador está
   en otra galaxia y hay una nodriza más cercana, señala esa. Representación:
   barra horizontal compacta con un punto móvil que indica la dirección relativa
   respecto a la orientación de la nave. Punto centrado = delante; derecha =
   derecha; izquierda = izquierda. Siempre disponible durante el vuelo.
2. **Brújula avanzada.** Módulo independiente y adquirible. Amplía la orientación
   hacia objetivos y destinos conocidos. No se confunde con el radar.
3. **Radar.** Módulo independiente con representación circular clásica. Dentro
   de su alcance y conocimiento muestra asteroides, fragmentos minerales, otras
   naves, pilotos artificiales, naves nodriza, estaciones, objetivos y
   delimitaciones de regiones/galaxias cuando la escala lo permita. Tiene
   categorías visuales y filtros, no satura la pantalla y no revela todo el
   universo: respeta alcance, sensores y estado de conocimiento. La brújula
   básica funciona sin radar.

### Contexto

No existe brújula. El módulo `RADAR` actual solo amplía el margen de selección
táctil en `MyGame.pickTarget()`. La brújula avanzada y el radar deben ser
instrumentos distintos, no duplicados.

### Opciones consideradas

- Brújula básica + brújula avanzada + radar separados.
- RADAR como brújula avanzada.
- Solo brújula básica.

### Impacto

Afecta a `PlayHud`, `HudView`, `MyGame`, `ShipModuleDef`, `ShipLoadout`,
`Upgrades`, `NavTarget`, `RadarScreen`, `StationScreen` y las pruebas de HUD,
módulos y navegación. El radar necesita posiciones reales de naves y agentes;
`PilotAgent` aún no las tiene y se documenta como dependencia.

---

## D-023 — Interior de la nave nodriza

**Estado:** `APROBADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T15:35:00-06:00  
**Tareas afectadas:** `UPD-006-T19`, `UPD-006-T20`, `UPD-006-T21`, `UPD-006-T22`, `UPD-006-T27`, `UPD-006-T30`, `UPD-006-T41`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta aprobada

Se combina una escena interior dedicada para la nave nodriza con una
arquitectura que aproveche y pueda ampliar el sistema de interiores de las
estaciones personales.

#### Escena interior dedicada

La nodriza es un espacio amplio y físicamente representado con zona de
adquisición y exposición de naves, área de acoplamiento y hangares, comercios,
instalaciones técnicas, pasillos, zonas de tránsito, lugares de interacción y
espacios habitables. No es una simple pantalla de servicios. El jugador se
desplaza físicamente y se acerca a las instalaciones para interactuar. Los menús
pueden existir como interfaces, pero se abren como consecuencia de interactuar
con elementos representados en el mundo.

#### Arquitectura reutilizable

La preferencia es extender el sistema de interiores de las estaciones personales
para soportar una instalación más amplia y compleja. Si no es técnicamente
conveniente, se puede crear una estructura específica para la nodriza, dejando
documentadas las posibilidades de homogeneización futura. Las estaciones
personales construidas por jugadores deben poder compartir movimiento,
representación, colisiones e interacción.

#### Personaje

El jugador está representado por un personaje reconocible, con sprites,
dirección de movimiento, animaciones básicas de caminar y reposo, colisiones e
interacción. No se acepta un círculo o punto como representación final.

#### Actividad y población interior

Desde las primeras versiones debe existir actividad visible: algunos NPC
caminando por pasillos, visitando instalaciones, personal en comercios y zonas
técnicas, NPC desplazándose entre lugares y actividad alrededor de hangares. No
se requieren cientos de personajes ni una simulación social compleja. La
población inicial es reducida, con movimiento y objetivos básicos; los NPC no
son estáticos. En etapas futuras podrán tener rutinas, trabajos, relaciones,
actividades comerciales e integración con la economía y los pilotos autónomos.
La prioridad es un entorno habitado visualmente convincente sin sobrecargar
Android.

### Contexto

Solo existe el interior de la estación personal. La nodriza tiene servicios pero
no una escena caminable. El nuevo inicio exige que el jugador comience a pie
dentro de la nodriza.

### Opciones consideradas

- Escena dedicada con arquitectura reutilizable.
- Reutilizar la escena de la estación personal sin ampliarla.
- Resolver la compra y el equipamiento solo con menús.
- Interior sin NPC ni actividad.

### Impacto

Afecta a `InteriorScene`, `Interactable`, `MyGame`,
`StationManagementScreen`, `ShipDealerScreen`, `Avatar`, `InteriorRenderer`,
`NpcActor` y las pruebas de interiores, rendimiento y accesibilidad.

---

## D-024 — Tutorial interactivo y campaña narrativa

**Estado:** `APLAZADA`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T14:35:00-06:00  
**Tareas afectadas:** `UPD-006-T34`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta

El tutorial interactivo y la campaña narrativa quedan aplazados. Esta
actualización solo documenta el contexto mínimo, la motivación y las
dependencias del tutorial futuro. No se implementan ventanas de tutorial,
campaña, facciones, historias conectadas ni sucesos dinámicos.

### Contexto

El usuario quiere que el inicio tenga personalidad, pero no considera apropiado
desarrollar ahora un tutorial completo ni una historia extensa.

### Opciones consideradas

- Implementar el tutorial ahora.
- Posponerlo y documentar sus dependencias.
- No hacerlo nunca.

### Recomendación inicial (histórica; prevalece la respuesta)

Posponerlo y documentar los pasos, dependencias y principios de diseño.

### Impacto

Afecta a `docs/updates/UPD-006.md`, `docs/PENDING_DECISIONS.md` y a futuras
actualizaciones narrativas. No afecta al código del juego en esta actualización.

---

## D-025 — Evolución de motores modulares

**Estado:** `APROBADA (documentación)`  
**Decidido por:** Usuario  
**Fecha:** 2026-11-08T18:10:00-06:00  
**Tareas afectadas:** `UPD-006-T40`  
**Actualizaciones afectadas:** `UPD-006`

### Respuesta

Se documenta la evolución futura de motores modulares sin implementar catálogos
ni compatibilidades en esta actualización. Las fórmulas actuales de empuje,
consumo, eficiencia y masa deben permitir añadir motores como datos más
adelante.

### Modelo futuro

| Concepto | Significado | Punto de extensión actual |
|---|---|---|
| Potencia | Empuje máximo del motor | `Upgrades.thrust()` y `ShipClassDef.thrustMultiplier` |
| Consumo | Combustible por segundo a plena potencia | `Ship.FUEL_PER_SEC_THRUST`, `Upgrades.fuelUse()`, `Ship.specFuelFactor` |
| Eficiencia | Combustible por unidad de desplazamiento | `Upgrades.fuelUse()` y `MassModel.fuelFactor()` |
| Propulsión | Velocidad máxima y respuesta | `Upgrades.maxSpeed()`, `Ship.update()` |
| Masa | Peso base y de carga | `Ship.specMassFactor`, `MassModel` y `CargoHold.usedMass()` |
| Compatibilidad | Ranura y tipo de montaje | `ShipSlotDef`, `ShipModuleDef.mountType` |

### Límites

- No se añaden catálogos de motores, modelos técnicos ni piezas.
- No se cambia el balance actual de niveles de Motor y Gasolina.
- Un motor futuro debe ser un dato con potencia, consumo, eficiencia, masa y
  tipo de montaje; no debe exigir reescribir `Ship.update()` ni la UI de
  equipamiento.
- La compatibilidad futura reutiliza `ShipSlotDef` y el modo legado de
  `ShipLoadout`.

### Impacto

Afecta a `docs/updates/UPD-006.md`, `docs/DECISIONS.md`,
`docs/PENDING_DECISIONS.md`, `Ship`, `Upgrades`, `MassModel` y
`ShipSlotDef` como puntos de extensión documentados. No cambia el
comportamiento actual.

---

## D-026 a D-030 — Renovación gráfica del mundo (UPD-007)

**Estado:** `D-026`, `D-027`, `D-028`, `D-030` APROBADAS; `D-029` TÉCNICA RESUELTA
**Decidido por:** Usuario (respuestas en conversación del 2026-11-08)
**Actualizaciones afectadas:** `UPD-007`
**Detalle completo:** [`updates/UPD-007.md`](updates/UPD-007.md) §11

Propuesta de `S-101` (Qoder) para pulir el apartado gráfico del mundo,
contrastada con el render real. `UPD-003` renovó la interfaz pero excluyó el
mundo 2D; `UPD-007` cubre ese hueco sin cambiar mecánicas ni agregar contenido.

### Respuestas del usuario

- **D-026 (estilo):** mundo más realista y definido, conservando el estilo.
  Algunos sectores se ven muy apagados; el juego crece en mecánicas pero no en
  diseño. Pulir todo y dejarlo orgánico.
- **D-027 (bloom):** solo glows mejorados. Sin bloom, sin shaders ni FBO.
- **D-028 (naves):** no agregar más naves (sería contenido). Mejorar las
  existentes; si dos comparten estilo, diferenciarlas. La personalización
  debe verse realmente y distinguirse en el casco.
- **D-029 (orden):** resuelta por los hechos: UPD-006 (S-100) quedó COMPLETADA
  el 2026-11-08 durante la preparación; no hay archivos en conflicto.
- **D-030 (interiores):** incluirlos; dar una pasada gráfica a todo lo
  relacionado por cómo quedaron tras UPD-006.

`UPD-007` queda en `PENDIENTE_APROBACION`; ninguna de sus 11 tareas puede
iniciarse hasta la aprobación explícita del usuario.

---

## UPD-008 — Rediseño del dashboard (S-102 → S-103)

| ID | Pregunta | Estado | Respuesta / recomendación |
|---|---|---|---|
| `D-031` | ¿Quién implementa el rediseño del dashboard y cómo se evita interferir con `UPD-007`? | Aprobada (usuario) | `UPD-008` (solo `docs/dashboard/*`) lo implementa `S-102`; `UPD-007` (solo `src/`) sigue en `S-101`. Sin solapamiento; `flock` protege `sync-dashboard.py`. |
| `D-032` | ¿Fuente local empaquetada o stack del sistema para el dashboard? | Aprobada (usuario) | Stack del sistema (`ui-sans`/`ui-monospace`): cumple la restricción no-CDN y no añade activos. Sin fuente empaquetada. |
| `D-033` | ¿Qué dirección visual debe tener el dashboard? | Aprobada (usuario) | Ambientada y personalizada, no industrial: carta estelar/cuaderno de navegación, orientación clara de "dónde estoy", información organizada y navegable sin saturar. |
| `D-034` | ¿Cómo se ejecutan las seis tareas de `UPD-008`? | Aprobada (usuario) | Como un rediseño cohesionado (`T01`–`T06`), limitado a `docs/dashboard/*`, en lugar de seis entregas visuales inconexas. |
| `D-035` | ¿Qué sesión continúa la implementación tras el agotamiento de `S-102`? | Técnica resuelta | La implementación continúa en `S-103` (CodeBuddy), conservando el alcance, la dirección visual y las decisiones de `S-102`. |

`UPD-008` quedó `COMPLETADA` el 2026-11-08: `T01`–`T06` implementadas,
pruebas de render y continuidad en verde y revisión visual en 375/768/1024/1440
px con ambos temas. Detalle en [`updates/UPD-008.md`](updates/UPD-008.md).

---

## UPD-009 — Scroll interno en listas largas (S-104)

| ID | Pregunta | Estado | Respuesta / recomendación |
|---|---|---|---|
| `D-036` | ¿Cómo evitar que las listas largas dominen la página? | Aprobada (usuario) | Altura máxima y scroll interno con fundidos, barra temática y foco de teclado en decisiones, problemas, bloqueadas, aprobaciones, tareas e historiales; el resto de la página se mantiene. |

`UPD-009` quedó `COMPLETADA` el 2026-11-08: la página con `UPD-008` seleccionada
pasó de ~15 362 px a ~5 818 px y con la versión base de ~22 686 px a ~6 974 px
(medido a 1440x900). Detalle en [`updates/UPD-009.md`](updates/UPD-009.md).

---

## UPD-010 — Reconstrucción visual del panel (S-105)

| ID | Pregunta | Estado | Respuesta / recomendación |
|---|---|---|---|
| `D-037` | ¿Se sustituye la carta estelar de `D-033` o se rehace su ejecución? | Aprobada (usuario) | Se conserva la carta estelar y se reconstruye la ejecución visual (retícula, tipografía, superficies, densidad); se descarta la dirección industrial. |
| `D-038` | ¿Qué tipografía usa el panel? | Aprobada (usuario) | Sans del sistema para UI y prosa, mono para cifras, IDs, versiones y fechas; sin serif. |
| `D-039` | ¿Cómo se reparte la información duplicada? | Aprobada (usuario) | Una sola fuente de verdad por cifra: cabecera comprimida, readout rail, resumen global denso y distribución única en el bloque global. |

`UPD-010` quedó `COMPLETADA` el 2026-11-08 (`S-105`): 6/6 tareas, cabecera de
242 px a 1440, sin desbordamiento en 1440/1024/768/390 y pruebas en verde.
Detalle en [`updates/UPD-010.md`](updates/UPD-010.md).

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
