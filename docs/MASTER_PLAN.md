# PLAN MAESTRO — GalacticIndustry

**Proyecto:** GalacticIndustry / *Asteroides*  
**Motor:** Java + LibGDX 1.14.2 · Android  
**Documento:** visión, arquitectura y plan de transformación  
**Estado:** auditoría y preparación completadas; implementación del juego **no iniciada**  
**Última actualización:** 2026-11-07  
**Fuente canónica de ejecución:** [`docs/IMPLEMENTATION_TRACKER.json`](IMPLEMENTATION_TRACKER.json)  
**Panel:** [`docs/dashboard/index.html`](dashboard/index.html)  
**Registro cronológico:** [`docs/IMPLEMENTATION_LOG.md`](IMPLEMENTATION_LOG.md)  
**Decisiones:** [`docs/DECISIONS.md`](DECISIONS.md)

> Este documento describe **qué** se quiere construir y **por qué**. El tracker describe **qué tarea concreta toca ahora**, con dependencias, criterios y evidencias. AGENTS.md describe **cómo** debe trabajar un agente. Ningún agente debe deducir el estado de implementación a partir de este documento: debe consultar el tracker y contrastarlo con el código.

---

## 0. Cómo usar este documento

1. Leer `AGENTS.md`.
2. Leer este plan maestro.
3. Abrir `docs/IMPLEMENTATION_TRACKER.json` y localizar la tarea `LISTO` o `EN_PROGRESO`.
4. Inspeccionar el código real antes de modificarlo.
5. Implementar **una sola tarea**.
6. Verificar criterios y pruebas.
7. Actualizar tracker, log, decisiones, documentación y panel.
8. Detenerse.

Las etapas de este plan son grandes. **La unidad de trabajo es la tarea del tracker, no la etapa.** Una sesión no debe implementar una etapa completa salvo instrucción explícita del usuario.

---

## 1. Resumen ejecutivo

### 1.1 Punto de partida

El proyecto es un arcade espacial 2D cenital, funcional y coherente, con:

- nave controlable con inercia;
- asteroides procedurales;
- misiles y destrucción de rocas;
- piloto automático a asteroides y bidones;
- monedas directas por destruir asteroides;
- tienda en menú/pausa;
- cinco mejoras globales;
- combustible, recarga y bidones;
- daño por siete partes con consecuencias funcionales;
- reparaciones;
- guardado de la partida en `SharedPreferences`;
- tests de lógica y de dispositivo.

### 1.2 Punto de llegada

Un juego de exploración, minería, supervivencia, comercio y gestión de recursos:

- piloto y nave como entidades separadas;
- oxígeno, salud, soporte vital y fugas;
- bodega, masa, capacidad y consumo;
- minería láser con tipos de asteroides y minerales;
- nave nodriza física y acoplable;
- mercado con stock y precios;
- pilotos autónomos que mueven la economía;
- galaxias, sistemas y sectores;
- estación espacial propia;
- interiores navegables, solo cuando lo anterior esté consolidado.

### 1.3 Estrategia

> **No reescribir. Introducir un modelo de sesión y de universo alrededor de la simulación existente, y sustituir el arcade por un ciclo económico por etapas, manteniendo el juego jugable después de cada etapa.**

### 1.4 Principios

1. Cada etapa deja una versión jugable.
2. La simulación se prueba sin Android ni LibGDX.
3. El render no contiene reglas de negocio.
4. Los datos son configurables y versionados.
5. La economía no depende solo del jugador.
6. La supervivencia crea decisiones, no castigos arbitrarios.
7. No hay movimiento a pie hasta que minería, comercio y estaciones estén consolidadas.
8. No se añaden dependencias sin justificación.
9. Una tarea por sesión.
10. La documentación refleja el código real, no deseos.

---

## 2. Auditoría del estado actual

### 2.1 Ficha técnica

| Elemento | Estado |
|---|---|
| Motor | LibGDX 1.14.2 |
| Build | `apkc build --inc`, sin Gradle |
| Android | `minSdk 26`, `targetSdk 36`, portrait, inmersivo |
| Código | Un paquete `com.example.codegame`; ~4.650 líneas propias |
| Física | Manual, círculos; sin Box2D |
| Render | `ShapeRenderer` + `SpriteBatch`; texturas procedurales |
| Texto | `assets/fonts/lsans-15.fnt`, ASCII |
| Persistencia | `SharedPreferences` (`codegame`): monedas, niveles, `run2` |
| Red | Sin permiso `INTERNET` |
| Audio | No existe |
| Tests | `SelfTest` + scripts `01..09` con adb |

### 2.2 Sistemas existentes

| Sistema | Estado | Evidencia |
|---|---|---|
| Movimiento de nave | Existente | `Ship.update`, joystick/barra |
| Mundo de asteroides | Existente | `GameWorld.generateChunk` |
| Destrucción con misiles | Existente | `Missile`, `destroyAsteroid` |
| Piloto automático | Existente | `Autopilot`: `FUEL`, `ASTEROID`, `STOP` |
| Monedas por asteroide | Existente | `destroyAsteroid` |
| Tienda | Existente | `MyGame.handleShop` |
| Mejoras | Parcial | 5 globales; faltan bodega, láser, soporte vital |
| Combustible | Existente sin masa | `Ship.fuel`, bidones, recarga por unidad (T-304) |
| Daño por partes | Existente | 7 partes con penalizaciones |
| Reparaciones | Existente | coste proporcional |
| Guardado | Parcial | texto plano, sin versión, solo `run2` |
| HUD | Parcial | casco, combustible, monedas, alertas |
| Recursos gráficos | Parcial | todo procedural; sin sprites |
| Tests | Parcial | lógica + dispositivo; acoplados a monedas |
| Telemetría | Existente | solo en modo debug |

### 2.3 Sistemas ausentes

- Piloto separado de la nave.
- Salud y oxígeno.
- Inventario personal y bodega.
- Masa y capacidad.
- Minería láser.
- Tipos de asteroides y minerales.
- Nave nodriza y acoplamiento.
- Mercado, stock y precios.
- Estaciones múltiples.
- Pilotos autónomos.
- Economía autónoma.
- Galaxias, sistemas y sectores.
- Estación personal.
- Interiores navegables.
- Audio.

### 2.4 Hallazgos que condicionan el plan

1. `Ship` mezcla piloto y nave.
2. `Upgrades` es global, no por nave.
3. La generación de asteroides no es completamente determinista: parte usa `MathUtils.random` fuera de la semilla del chunk.
4. El guardado no tiene versión ni composición de asteroides.
5. `MyGame` era un monolito de ~2.600 líneas; T-007 lo dividió en `MyGame` (flujo), `UiKit`, `PlayHud`, `ShopRenderer`, `OverlayRenderer` y `SpaceBackground`.
6. El dibujo no hace culling.
7. `Effects` asigna objetos por partícula.
8. La recarga de combustible se duplicaba globalmente; corregido en T-304 con 1 crédito/unidad y mínimo 5.
9. Los tests dependen de monedas y de la tienda en pausa.
10. La UI actual no cabe con todos los indicadores nuevos.
11. No hay red ni servicios, lo cual es correcto para la visión local.
12. `SelfTest` mostró un fallo esporádico en 5 ejecuciones; hay que fijar semillas.

### 2.5 Lo verificado y lo no verificado

- **Verificado:** estructura, fórmulas, estados, flujo de tienda, daño, guardado, generación, piloto, tests, configuración y build.
- **Verificado parcialmente:** `SelfTest` sobre clases compiladas; 4 de 5 ejecuciones `TODO OK`, una con un fallo no identificado.
- **No verificado:** tests de dispositivo `02–09`, FPS, memoria, batería, APK en teléfono, aspecto visual real, guardados largos.

---

## 3. Visión y ciclo de juego

### 3.1 Bucles

| Escala | Bucle |
|---|---|
| 30 s | Girar, acelerar, seleccionar roca, minar |
| 5–15 min | Salir, minar, gestionar bodega/O2/combustible, decidir volver |
| 30–90 min | Vender, reparar, mejorar, cambiar de campo/sistema |
| Varias horas | Desbloquear sistemas, comprar naves, construir estación |
| Largo plazo | Explorar galaxias, dominar rutas, economía propia |

### 3.2 Ciclo económico

```
Estación nodriza
  → reparar, repostar, comprar mejoras
  → despegar
  → minar
  → bodega llena / O2 bajo / fuga / combustible bajo
  → decidir volver
  → acoplar
  → descargar, vender, reparar, repostar
  → preparar otra expedición
```

### 3.3 Coherencia sistémica

La cadena clave es:

> **Mercado → Carga → Masa → Combustible → Movimiento → Riesgo → Supervivencia → Decisión de volver → Mercado.**

La minería llena la bodega; la bodega aumenta la masa; la masa aumenta el consumo; el consumo limita el viaje; el daño crea fugas; las fugas presionan el O2; el O2 obliga a decidir; la estación cierra el ciclo.

### 3.4 Anti-frustración

- Avisos antes de situaciones críticas.
- Margen de reacción.
- Rescate/remolque ante falta de combustible.
- La carga completa debe ser rentable.
- Las mejoras deben abrir decisiones, no solo inflar números.
- La economía necesita sumideros.
- No convertir el O2 en un temporizador de 3 minutos.

---

## 4. Entidades y sistemas

### 4.1 Piloto

| Campo | Función |
|---|---|
| `health`, `maxHealth` | Salud independiente del casco |
| `oxygen`, `o2Max` | Reserva de O2 |
| `status` | `OK`, `HYPOXIC`, `INCAPACITATED`, `DEAD` |
| `personalInventory` | Reservado; no se implementa al principio |
| `location` | Abordo, estación o interior |

El piloto no se mueve a pie en las primeras etapas. Su salud no baja por daño de casco directamente, salvo asfixia o eventos.

### 4.2 Nave

- Posición, velocidad, rumbo.
- Combustible y depósito.
- Bodega y masa.
- Partes y loadout.
- Clase de nave y nombre.
- Estado `wrecked`.

`Ship` puede seguir siendo la implementación de movimiento, pero deja de ser el contenedor del jugador.

### 4.3 Partes

Se amplía de 7 a 9, añadiendo al final:

| Índice | Parte | A 0 |
|---:|---|---|
| 0 | `HULL` | Nave destruida; fuga máxima |
| 1 | `ENGINE` | 10 % empuje/velocidad |
| 2 | `TANK` | 40 % capacidad y fuga de combustible |
| 3 | `CANNON` | No dispara |
| 4 | `STEERING` | 20 % giro |
| 5 | `WING_LEFT` | Ala perdida, deriva |
| 6 | `WING_RIGHT` | Ala perdida, deriva |
| 7 | `LIFE_SUPPORT` | Filtro al 20 % |
| 8 | `MINING_LASER` | Sin minería |

No se añade `CARGO` como parte dañable en la primera versión.

### 4.4 Inventarios

1. **Personal:** reservado, capacidad pequeña.
2. **Bodega de nave:** minerales, materiales, suministros; capacidad en UM.
3. **Almacén de estación:** depósito del jugador, separado del stock del mercado.

### 4.5 Soporte vital

- O2 máximo, consumo, filtro.
- Fugas por casco y brechas.
- La integridad de `LIFE_SUPPORT` reduce el filtro.
- No hay energía dedicada al soporte vital en la primera versión.

### 4.6 Minería

- Láser continuo con potencia, penetración y capacitor.
- Tipos C/S/M con dureza, reserva y composición.
- Extracción a bodega; agotamiento de roca.
- Misiles como herramienta secundaria, no como minería principal.
- El mineral sustituye a las monedas directas.

### 4.7 Estaciones

- Nave nodriza: centro de operaciones de la galaxia.
- Estación comercial, industrial, puesto avanzado.
- Estación del jugador: construida por módulos.
- Acoplamiento por proximidad y velocidad.
- Sin interiores en las primeras etapas.

---

## 5. Universo y galaxias

### 5.1 Jerarquía

| Nivel | Escala | Representación |
|---|---|---|
| Universo | Todo | Contenedor de galaxias |
| Galaxia | Pack de contenido | Mapa de sistemas |
| Sistema | Nodo | 1–3 sectores |
| Sector | ~12.000 × 12.000 u | Instancia 2D |
| Región | Subzona | Campo, estación, anomalía |
| Chunk | 800 × 800 u | Streaming procedural |

No se simula astronomía real. Las distancias son jugables.

### 5.2 Sectores finitos

El mundo infinito actual se convierte en sectores finitos cargables:

- estación principal cerca del centro;
- campos de asteroides por regiones;
- puerta/atajo opcional en el borde, no requisito de viaje;
- sin wraparound;
- al salir se guarda el estado y se descarga el detalle;
- al volver se reconstruye desde semilla + estado persistido;
- los recursos minados no reaparecen.

### 5.3 Nave nodriza

- Punto de origen de la aventura en cada galaxia.
- Servicios completos.
- Centro comercial y logístico.
- **No** es punto de reaparición automático.
- Puede ser una estación fija de gran escala.

### 5.4 Diferenciación de galaxias

| Dimensión | Ejemplo |
|---|---|
| Composición de asteroides | Más metales o más hielo |
| Mineral exclusivo | Material raro |
| Precios | Demanda distinta |
| Estaciones | Comerciales/industriales |
| Riesgo | Tormentas, piratas (futuro) |
| Tecnología | Naves/mejoras exclusivas |
| Identidad visual | Paleta y nebulosa |

Contenido inicial: la Vía Láctea como galaxia de arranque y al menos otra galaxia real o ficticia con contenido diferenciado. No se busca exactitud astronómica.

### 5.5 Viaje

| Modelo | Valoración |
|---|---|
| **Warp libre** | **Aprobado**: viaje desde la nave con coste de combustible/energía y equipamiento |
| Puertas de salto | Atajos opcionales, **no requisito** |
| Rutas/lanes | Futuro |
| Vuelo real 2D continuo a escala astronómica | No recomendado por escala y rendimiento |

El universo es muy grande y se carga dinámicamente: solo se mantiene el detalle de regiones cercanas; las lejanas se descargan o simplifican. El warp no elimina el coste: consume combustible/energía y depende del equipamiento de la nave. Las puertas pueden existir como infraestructura o atajo, pero no como condición para viajar.

### 5.6 Datos y determinismo

Cada galaxia, sistema, sector, chunk y asteroide deriva de una semilla estable. Se guarda el estado de recursos, no todo el detalle inactivo. Añadir una galaxia debe ser añadir datos, no tocar el núcleo.

---

## 6. Minería, minerales y carga

### 6.1 Tipos de asteroides

| Tipo | Dureza | Reserva | Composición orientativa | Frecuencia |
|---|---:|---:|---|---:|
| C carbonáceo/helado | 2 | 12–18 UM | Hielo 45 %, Silicato 35 %, Hierro 15 %, Cobre 5 % | 55 % |
| S silicáceo | 5 | 28–42 UM | Silicato 40 %, Hierro 30 %, Cobre 20 %, Titanio 8 %, Oro 2 % | 33 % |
| M metálico | 8 | 60–90 UM | Hierro 40 %, Cobre 25 %, Titanio 30 %, Oro 5 % | 12 % |

### 6.2 Minerales

| Mineral | Masa/UM | Precio base | Uso |
|---|---:|---:|---|
| Hielo | 0.80 | 1.5 | O2, combustible |
| Silicato | 1.00 | 2.0 | Construcción |
| Hierro | 1.20 | 3.0 | Reparación |
| Cobre | 1.50 | 5.0 | Electrónica |
| Titanio | 1.80 | 12.0 | Mejoras |
| Oro | 2.50 | 30.0 | Comercio de alto valor |

### 6.3 Láser

```text
resistencia = clamp(1 - max(0, dureza - penetracion) * 0.10, 0.15, 1.0)
tasa        = potenciaLaser * integridadLaser * resistencia   [UM/s]
tiempo      = masaRestante / tasa
energía     = -3.0/s mientras mina; +8.0/s tras 1 s sin minar
```

Valores iniciales:

- potencia 6 + 1.5·nivel;
- penetración 3 + 1·nivel;
- capacitor 100 + 25·nivel;
- C: 2–4 s; S: 6–9 s; M: 20–30 s con equipo inicial.

### 6.3.1 Misiles y rayo tractor

Los misiles se conservan como herramienta de destrucción/fragmentación. No entregan mineral directamente: rompen la roca y dispersan fragmentos que el jugador debe recuperar. El rayo tractor atrae esos fragmentos hacia la nave; tiene alcance, velocidad y capacidad mejorables. Láser, misiles y tractor son sistemas complementarios:

- **láser:** extracción controlada y eficiente;
- **misiles:** ruptura rápida con dispersión y posible pérdida;
- **rayo tractor:** recuperación de fragmentos.

No se fijan porcentajes definitivos de pérdida ni eficiencia hasta las pruebas de balance.

### 6.4 Carga y masa

```text
masaTotal   = masaBase + masaCarga
carga       = masaCarga / capacidad
aceleracion = thrust * engineFactor * (1 - 0.25*carga)
velocidad   = maxSpeed * (1 - 0.12*carga) * factorAlas
giro        = 220 * steer * (0.55 + 0.45*wing) * (1 - 0.10*carga)
consumo     = 10 * fuelUse * (1 + 0.35*carga)
```

- Masa base: 100 UM.
- Capacidad inicial: 100 UM.
- Carga completa: +35 % consumo, −25 % aceleración, −12 % velocidad, −10 % giro.
- Depósito base propuesto: 160; consumo base: 10.
- Ida y vuelta de 800 u con carga completa: ~112 de combustible, con margen.

---

## 7. Supervivencia

### 7.1 O2

| Parámetro | Valor |
|---|---:|
| O2 máximo | 100 + 20·nivel soporte |
| Consumo del piloto | 4/min |
| Filtro base | 1.5/min |
| Filtro efectivo | `1.5 * (0.20 + 0.80*LIFE/100) * (1 + 0.10*nivel)` |
| Fuga | `6 * (1 - casco/cascoMax) * (1 + 0.5*brecha)` |
| Asfixia | 5 s de gracia y luego 1 HP/s |

- Sin fugas: ~40 min de O2.
- Casco al 50 %: ~18 min.
- Brecha crítica: 5–8 min.
- La salud no baja mientras haya O2.
- La muerte por asfixia no es instantánea ni silenciosa.

### 7.2 Fugas

- Se generan por daño de casco.
- Una brecha añade fuga extra.
- Se reparan en estación o con kit de sellado.
- Deben ser visibles en HUD y en el mundo.
- No deben matar en 10 segundos sin aviso.

### 7.3 Muerte

Decisión **D-001 aprobada**: muerte permanente del personaje en el juego normal.

- El personaje muerto no reaparece ni se recupera.
- Se puede crear un personaje nuevo, que entra en el **mismo mundo persistente**, no en una copia reiniciada.
- La nave anterior permanece en las coordenadas exactas de la muerte, junto con los restos, objetos y recursos que sigan existiendo.
- El nuevo personaje puede viajar hasta esas coordenadas y encontrar/recuperar la nave.
- La nave nodriza **no** es un punto automático de reaparición.
- Debe existir un modo de desarrollo/pruebas para recuperarse o continuar sin perder el personaje; ese modo no forma parte del juego normal.
- La persistencia del mundo es un requisito, no un detalle opcional.

---

## 8. Estaciones

### 8.1 Nave nodriza

- Gran estación física en el sector inicial.
- Radio visual ~180–220; zona de acople ~260.
- Zona segura sin asteroides.
- Servicios completos.
- No es punto de reaparición.

### 8.2 Acoplamiento

1. Detectar proximidad y velocidad < 30.
2. Mostrar `ACOPLAR`.
3. Pulsar o tocar la estación.
4. Detener la nave y pasar a `DOCKED`.
5. Abrir pantalla de estación.
6. `DESPEGAR` devuelve al exterior con velocidad 0.
7. `Autopilot.STATION` puede llevar y frenar.

### 8.3 Servicios

| Servicio | Etapa |
|---|---|
| Descargar mineral | 5 |
| Vender a precio base | 5 |
| Comprar combustible | 5 |
| Reabastecer O2 | 5 |
| Reparar partes | 5 |
| Mejoras | 5 |
| Almacén | 5 |
| Mercado con stock | 6 |
| Refinería | 9 |
| Construcción | 9 |
| Misiones | 11 |

### 8.4 Flujo

```
MENU → NUEVA PARTIDA → estación nodriza → DESPEGAR → PLAYING
PLAYING → ACOPLAR → STATION → mercado/servicios/mejoras → DESPEGAR
PAUSA → CONTINUAR / OPCIONES / SALIR
```

La tienda de pausa deja de ser la economía principal. Decisión **D-002**.

### 8.5 Estación personal

- No es la nave nodriza.
- La primera versión es una **plataforma vacía** donde se puede acoplar la nave.
- No incluye almacén, reparación, refinería, mercado ni otros servicios de serie.
- El jugador construye y amplía módulos progresivamente: almacenes, talleres, refinerías, mercados, producción, hábitat y otros.
- La arquitectura debe ser modular y extensible.
- Sin interiores hasta la Etapa 10; la Etapa 10 empieza por la estación propia.
- Decisión **D-005 aprobada**.

---

## 9. Economía

### 9.1 Moneda

- Créditos enteros.
- Los minerales tienen precio y masa.
- Sin créditos negativos.
- Si no se puede pagar un rescate, se pierde carga o se aplica penalización.

### 9.2 Mercado

```text
factor      = clamp((targetStock / max(stock, 1)) ^ 0.5, 0.50, 2.00)
precioMedio = basePrice * factor
venta       = precioMedio * (1 - 0.08)
compra      = precioMedio * (1 + 0.08)
stock'      = stock + comprado - vendido
```

- Stock inicial 50–120 % del target.
- Límites de precio.
- Spread del 8 %.
- Reversión lenta al precio base.
- Producción/consumo por estación.
- El jugador y los NPC mueven stock.

### 9.3 Sumideros

- Combustible.
- O2.
- Reparaciones.
- Mejoras.
- Construcción.
- Pérdida de carga al morir.
- Tasas opcionales.

### 9.4 Transición desde monedas

| Fase | Fuente de créditos |
|---|---|
| Actual | Destruir asteroides |
| Etapa 4 | Fundición temporal de mineral a precio base |
| Etapa 5 | Venta en estación a precio base |
| Etapa 6 | Mercado con stock/precios |

---

## 10. Pilotos autónomos

### 10.1 Objetivo

Que los mercados cambien por actividades simuladas, no por azar cosmético. No se usa un LLM ni servicios externos.

### 10.2 Agente

- Rol, nave resumida, carga, combustible, estado, objetivo.
- 8 agentes iniciales; máximo 24.
- Estados: `DOCKED`, `TRAVEL`, `MINE`, `RETURN`, `SELL`, `REFUEL`, `IDLE`.
- Decisión por utilidad:

```text
utilidad = valorCarga*fill - combustible - tiempo*oportunidad - riesgo
```

### 10.3 Simulación

| Situación | Tick |
|---|---:|
| Cerca del jugador | 2 s |
| Sector activo, lejos | 5 s |
| Otro sector | 10 s |
| Offline | 60 s, máximo 12 h |

- Determinismo por semilla + contador.
- Sin pathfinding 2D complejo.
- Solo 2–4 naves cosméticas cerca de la estación.
- Persistencia de mercados y agentes.
- Sin hilos obligatorios ni servicio en segundo plano.

### 10.4 Impacto

Las ventas de mineral suben stock y bajan precio; las compras hacen lo contrario. Los cambios deben ser explicables y limitados.

---

## 11. Arquitectura técnica

### 11.1 Capas

```
Android / MainActivity
  → MyGame (router de estados)
    → pantallas: Sector, Station, Market, Map, Inventory, Interiors
      → GameSession
        → PlayerProfile, Pilot, ShipState, Cargo, UniverseState, EconomySim
          → datos puros: Commodity, Market, Agent, Mining, LifeSupport
```

### 11.2 Reglas

- Java puro para simulación.
- LibGDX solo en presentación/entrada.
- Sin ECS, sin bus de eventos, sin DI, sin dependencias nuevas.
- Mantener el paquete plano al principio; dividir solo si el tamaño lo exige.
- `GameWorld` se convierte en runtime de sector, no en el contenedor de todo.
- `MyGame` se reduce a router; extraer `UiKit` y pantallas.
- `Upgrades` se convierte en fachada de `ShipLoadout` con ranuras de módulos.
- `PlayerProfile` debe permitir identidad estable de personaje/jugador y varios personajes.
- No cerrar la puerta a facciones u online futuro: evitar suposiciones rígidas de un único jugador o de un servidor inexistente.
- El guardado debe poder migrar a un modelo con varios personajes y, eventualmente, servidor.
- `SelfTest` se amplía y se fija con semillas.

### 11.3 Guardado

- Archivo JSON local, versión 2.
- `Gdx.files.local("save/slot1.json")`.
- Escritura atómica con `.tmp` y backup `.bak`.
- Migración desde `run2`.
- No guardar cada frame.
- Guardar en pausa, acople, transacción, cambio de sector y cada 30 s.

### 11.4 Rendimiento

| Recurso | Límite |
|---|---:|
| Asteroides activos | 300 |
| Partículas | 520 |
| Agentes | 24 |
| Tick economía | 2–5 s |
| FPS objetivo | 60 gama media / 30 mínima |

- Culling por cámara.
- Sin asignaciones en render.
- Pool de partículas si hay GC.
- No simular economía por frame.
- No renderizar agentes lejanos.

### 11.5 UI

- Extraer `UiKit`.
- HUD por prioridades: casco/O2/combustible; carga/salud; créditos/energía; alertas.
- Pantallas contextuales, tabs, botones grandes.
- Fuente ASCII; textos sin acentos en UI.
- Mantener paleta y render procedural.
- No sustituir todos los gráficos.

### 11.6 Extensión futura: identidad, facciones y online

- Sin red, multijugador, PvP ni facciones en la implementación actual.
- `PlayerProfile` separa `playerId` (cuenta/jugador) de `id` (personaje).
- Los slots de `SaveService` permiten varios personajes sin mezclar archivos.
- `GameSession` es una instancia; no hay singleton global de jugador.
- La simulación local no tiene autoridad de servidor ni anti-cheat. El guardado
  es local y editable.
- Puntos de extensión previstos: ids de facción, transacciones autoritativas,
  registro de eventos y separación simulación/presentación.
- Cualquier modalidad online futura exigirá rediseñar autoridad, persistencia y
  validación; no se asume que el guardado local sea seguro.

---

## 12. Etapas y tareas

La granularidad ejecutable está en `IMPLEMENTATION_TRACKER.json`. Esta tabla es el mapa de alto nivel.

| Etapa | Nombre | Objetivo | Depende de | Tareas | Gate |
|---|---|---|---|---|---|
| S0 | Auditoría y preparación | Conocer el proyecto y crear continuidad | — | T-001..T-006 | **Completada** |
| S1 | Separación piloto/nave | GameSession, Pilot, ShipLoadout modular, save v2, revisión online | S0 | T-101..T-107 | **Completada**: paridad, migración y extensibilidad |
| S2 | Supervivencia | O2, filtro, fugas, salud, muerte permanente y nave legado | S1 | T-201..T-210 | **Completada**: expedición con O2/fuga y mundo persistente |
| S3 | Carga y masa | Bodega, minerales, masa, combustible | S1 | T-301..T-306 | **Completada**: viaje viable con carga |
| S4 | Minería y recuperación | Láser, misiles, fragmentos, rayo tractor | S2, S3 | T-401..T-410 | **Completada**: minar, fragmentar y recuperar |
| S5 | Nave nodriza | Estación física, acople, servicios | S4 | T-501..T-507 | **Completada**: ciclo completo en estación |
| S6 | Mercado | Stock, precios, compraventa | S5 | T-601..T-606 | **Completada**: precios reaccionan |
| S7 | NPC | Agentes, economía autónoma | S6 | T-701..T-707 | Precios cambian sin jugador |
| S8 | Universo | Warp libre, streaming, galaxias reales/ficticias | S5–S7 | T-801..T-808 | Viaje libre y streaming |
| S9 | Estación propia | Plataforma vacía, módulos construibles | S6, S8 | T-901..T-908 | Construir y ampliar |
| S10 | Interiores | Movimiento a pie; estación propia y base para naves | S9 | T-1001..T-1007 | Entrar, caminar, salir |
| S11 | Expansión | Naves modulares, audio completo, balance | S8–S10 | T-1101..T-1109 | Regresión final |

### 12.1 Reglas de las etapas

- No empezar una etapa si sus dependencias no están completadas.
- No implementar tareas de una etapa posterior “de paso”.
- Una tarea solo se marca `COMPLETADO` tras verificar criterios y pruebas.
- Si una tarea requiere una decisión pendiente, se marca `REQUIERE_DECISION` y se detiene.
- Si una tarea se bloquea, se registra el motivo exacto y qué falta.
- Si se añade una tarea, se le asigna un ID nuevo; no se renumeran las existentes.
- Si se cambia una dependencia, se documenta en el log y se actualiza el tracker.

### 12.2 Tareas de decisión

No hay tareas `REQUIERE_DECISION`: las decisiones D-001 a D-006 y D-009 a D-011 están aprobadas. D-012 está aplazada.

- `T-1103` (facciones/combate/online): `POSPUESTO`.
- `T-1007` (interiores de naves y estaciones no propias): `POSPUESTO`.
- `T-107` (revisión de extensibilidad online): `PENDIENTE`, no cuenta para el progreso de implementación.

El agente no debe implementar T-1103 ni T-1007 hasta que el usuario lo pida.

---

## 13. Fórmulas y parámetros iniciales

> Valores de arranque, no definitivos. Ajustar con telemetría.

### 13.1 Nave

| Parámetro | Valor |
|---|---:|
| `RADIUS` | 16 |
| `TURN_SPEED` | 220 °/s |
| `thrust` | 260 + 60·motor |
| `maxSpeed` | 220 + 40·motor |
| `maxFuel` | 160 + 30·depósito |
| `fuelUse` | 1 − 0.08·gasolina |
| `FUEL_PER_SEC_THRUST` | 10 |
| `FUEL_PER_SHOT` | 2 |
| `masaBase` | 100 |
| `capacidad` | 100 + 25·bodega |
| `hull` | 100 + 25·blindaje |

### 13.2 Supervivencia

| Parámetro | Valor |
|---|---:|
| `saludMax` | 100 |
| `o2Max` | 100 + 20·soporte |
| `consumoO2` | 4/min |
| `filtroBase` | 1.5/min |
| `fugaBase` | 6/min al 0 % de casco |
| `danoAsfixia` | 1 HP/s tras 5 s |

### 13.3 Minería

| Parámetro | Valor |
|---|---:|
| `potencia` | 6 + 1.5·láser |
| `penetración` | 3 + 1·láser |
| `capacitor` | 100 + 25·láser |
| `consumo` | 3/s |
| `recarga` | 8/s tras 1 s |

### 13.4 Mercado

```text
factor = clamp((target/stock)^0.5, 0.50, 2.00)
venta  = base * factor * 0.92
compra = base * factor * 1.08
```

### 13.5 Agentes

- 8 inicial; 24 máximo.
- Tick 2/5/60 s.
- Offline 12 h.
- Utilidad = valor − combustible − tiempo − riesgo.

### 13.6 Estación

| Servicio | Precio |
|---|---:|
| Combustible | 1 crédito/unidad |
| O2 | 0,5 créditos/unidad |
| Reparación | 0,70 créditos/punto |
| Acople | gratis |
| Rescate | 100 créditos + 25 % de carga o pérdida |
| Núcleo de estación | 500 créditos + 60 hierro + 30 silicato |
| Módulo almacén | 300 créditos + 40 hierro |

---

## 14. Riesgos y limitaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Explosión de alcance | Alto | Una tarea por sesión; etapas cerradas |
| Monolito `MyGame` | Medio | Extraer `UiKit`/pantallas en Etapa 5 |
| Guardado corrupto | Alto | SaveService v2, backup, migración |
| Nondeterminismo | Medio | RNG explícito y semillas |
| Rendimiento Android | Alto | LOD, culling, ticks, límites |
| Economía inflacionaria | Alto | Spread, stock, límites, sumideros |
| Softlock combustible/O2 | Alto | Rescate, avisos, margen |
| UI saturada | Medio | Prioridades, tabs, pantallas |
| Tests acoplados a monedas | Medio | Actualizar por etapa |
| Interiores desvían el proyecto | Alto | Etapa 10 solo tras 9; D-006 |
| Pérdida de identidad visual | Medio | Conservar render procedural y paleta |

Limitaciones:

- 2D cenital, sin gravedad ni órbitas.
- Economía local, determinista, sin mercado global real.
- NPC con máquinas de estados, no IA generativa.
- Sin multijugador, nube ni servicios en esta etapa; la arquitectura debe permitir evolucionar a online sin rehacer el núcleo.
- Estación personal sin interiores hasta Etapa 10.
- Combate, facciones y PvP aplazados; la simulación económica se centra en minería, transporte y comercio.

---

## 15. Decisiones

El registro completo está en [`docs/DECISIONS.md`](DECISIONS.md) y en el tracker. Resumen:

| ID | Decisión | Estado | Recomendación |
|---|---|---|---|
| D-001 | Muerte y persistencia | **Aprobada** | Muerte permanente, mundo persistente, nave legado, modo test |
| D-002 | Tienda de pausa | **Aprobada** | Eliminar; economía solo en estaciones |
| D-003 | Láser, misiles y tractor | **Aprobada** | Tres sistemas complementarios; sin porcentajes definitivos |
| D-004 | Viaje interestelar | **Aprobada** | Warp libre, streaming, sin puertas obligatorias |
| D-005 | Estación personal | **Aprobada** | Plataforma vacía y módulos construibles |
| D-006 | Interiores | **Aprobada a largo plazo** | Estaciones, estación propia y naves; después |
| D-007 | Agentes/offline | Técnica resuelta | 8/24, 12 h |
| D-008 | Guardado | Técnica resuelta | JSON local + backup |
| D-009 | Audio | **Aprobada** | Música, ambientes y SFX completos en refinamiento |
| D-010 | Naves y módulos | **Aprobada** | Modelos distintos, piezas y módulos; autopiloto instalable |
| D-011 | Galaxias | **Aprobada** | Vía Láctea, otras reales y ficticias |
| D-012 | Facciones/combate/online | **Aplazada** | No ahora; arquitectura abierta a futuro online |

---

## 16. Protocolo de cambios en el plan

Cuando se apruebe una característica nueva:

1. Añadirla a este plan maestro.
2. Determinar dependencias.
3. Desglosarla en tareas.
4. Asignar IDs nuevos.
5. Definir criterios de aceptación y pruebas.
6. Actualizar `IMPLEMENTATION_TRACKER.json` y los conteos `taskCount` /
   `implementationTaskCount` de la etapa.
7. Actualizar `IMPLEMENTATION_LOG.md`.
8. Actualizar `DECISIONS.md` si implica una decisión.
9. Sincronizar el panel HTML.
10. Registrar el cambio.

Reglas:

- No eliminar tareas o etapas silenciosamente.
- No cambiar IDs existentes.
- No convertir una propuesta no aprobada en requisito definitivo.
- Si una nueva característica cambia el orden, documentar el motivo y actualizar dependencias.
- Si una tarea se divide, crear tareas nuevas y marcar la original como `POSPUESTO` o `COMPLETADO` según corresponda, sin reutilizar IDs.

---

## 17. Definición de terminado

Una tarea está terminada cuando:

- el código compila;
- las pruebas de la tarea pasan;
- los criterios de aceptación se han comprobado;
- no hay regresiones conocidas;
- los archivos modificados están registrados;
- las decisiones técnicas están documentadas;
- el tracker está actualizado;
- el log tiene una entrada de sesión;
- el panel HTML está sincronizado;
- el agente se ha detenido.

Una etapa está terminada cuando todas sus tareas `countsForProgress` están `COMPLETADO` o `POSPUESTO` con justificación, y su gate se ha verificado.

---

## 18. Relación con la infraestructura de continuidad

| Archivo | Responsabilidad |
|---|---|
| `AGENTS.md` | Punto de entrada y protocolo |
| `docs/MASTER_PLAN.md` | Visión, arquitectura y diseño |
| `docs/IMPLEMENTATION_TRACKER.json` | Estado canónico de etapas/tareas |
| `docs/IMPLEMENTATION_LOG.md` | Historial cronológico |
| `docs/DECISIONS.md` | Decisiones y propuestas |
| `docs/PENDING_DECISIONS.md` | Preguntas pendientes para conversar y plantilla de respuestas |
| `docs/dashboard/index.html` | Panel visual con datos integrados |
| `docs/dashboard/dashboard.css` | Estilos del panel |
| `docs/dashboard/dashboard.js` | Lógica del panel |
| `docs/PROJECT_DASHBOARD.html` | Redirección de compatibilidad al panel nuevo |
| `tools/sync-dashboard.py` | Validación y sincronización JSON → HTML |

El panel no inventa estados: representa el tracker. Si el HTML y el JSON no coinciden, el JSON manda y hay que ejecutar la sincronización.
