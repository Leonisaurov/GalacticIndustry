# Sistema — Interfaz, HUD y render

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `MyGame.java`, `UiKit.java`, `PlayHud.java`, `OverlayRenderer.java`, `SpaceBackground.java`, `HudView.java`, `Visuals.java`, `Effects.java` |
| Documentación | `arquitectura.md` §Estructura de la interfaz, §Render y aspecto visual; `convenciones.md`; `MASTER_PLAN.md` §11.5 |
| Decisiones | D-002 (pausa sin tienda), D-015 (identidad visual industrial) |
| Tareas | T-007, T-504, T-1110, UPD-003-T01 |
| Pruebas | `06_visual_menu`, `07_visual_play`, `08_visual_death`, `13_station`, `14_market`, `17_player_station`, `18_interiors`, `18_map`, `22_ui_polish` |

## 1. Descripción

Capa de presentación y orquestación: estados de juego, menú, pausa, HUD, fin de
partida, fondos, partículas y utilidades de UI. `MyGame` sigue siendo el
orquestador central.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Estados y entrada | Implementado | Menú, juego, pausa, mapa, mercado, estación, interior, muerte. |
| HUD de partida | Implementado | Monedas, oleada, puntos, casco, combustible, carga, O2/HP. |
| Menú y pausa | Implementado | Sin solapes; pausa sin tienda económica. |
| Overlay de muerte | Implementado | Diálogo de fin y reintento. |
| Fondos y partículas | Implementado | `SpaceBackground`, `Effects`, `Visuals`. |
| UI táctil | Implementado | `UiKit` con botones y texto ajustado. |
| Identidad visual | Implementado | Consola industrial: grafito, acero, ámbar y cian; marcos, LEDs e iconos geométricos. |
| Fuente ASCII | Implementado | `assets/fonts/`; no usar `new BitmapFont()`. |
| Accesibilidad completa | Parcial | Ver [`accessibility.md`](accessibility.md). |

## 2.1. Sistema visual

La interfaz sigue la decisión **D-015**:

- **Superficies:** `Visuals.PANEL`, `CARD`, `GLASS`, `STEEL`; profundidad con
  bisel superior, sombra inferior y bordes de acero.
- **Acentos:** `ACCENT` (ámbar de seguridad) para acciones y navegación,
  `INFO`/`O2` (cian) para instrumentación, `GOOD`/`WARN`/`DANGER` para estado.
- **Texto:** `TEXT`, `TEXT_DIM` y `TEXT_OFF`; todo en mayúsculas para etiquetas
  técnicas y sin acentos si el glifo ASCII no existe.
- **Marcos:** `UiKit.frame(...)` dibuja cabecera, barra de acento, esquinas y
  remaches; `panel`, `divider`, `grid` y `led` completan el lenguaje.
- **Iconos:** mineral, carga, combustible, O2, casco, energía, estación, warp,
  alerta y engranaje; se dibujan con primitivas y no añaden atlas.
- **Composición:** cabecera + lista + pie en estación, mercado, mapa y gestión;
  menú/pausa/fin comparten panel centrado con acciones separadas.

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `MyGame` | Estados, entrada, sesión, guardado y telemetría. |
| `UiKit` | Paneles, marcos de consola, botones, iconos, LEDs, rejilla y texto. |
| `Visuals` | Paleta industrial, texturas suaves y utilidades de dibujo. |
| `PlayHud` | HUD de partida (incluye O2/HP). |
| `OverlayRenderer` | Menú, pausa, fin y fundido. |
| `SpaceBackground` | Fondo espacial. |
| `HudView` | Estado efímero del HUD. |
| `Visuals` / `Effects` | Paleta, texturas y partículas. |

## 4. Flujo

1. `MyGame.render()` dibuja el mundo y el HUD según el estado.
2. `PlayHud` lee la sesión y dibuja barras y avisos.
3. `OverlayRenderer` dibuja menús y diálogos.
4. `UiKit` centraliza botones, paneles y texto.
5. Las partículas se actualizan en `GameWorld`/`Effects`.

## 5. Decisiones aplicables

- **D-002:** la pausa no es una tienda; las acciones económicas viven en la
  estación.
- **D-015:** identidad de simulador espacial industrial; paleta grafito/acero/
  ámbar/cian y componentes de consola reutilizables.

## 6. Tareas relacionadas

T-007 (panel/UI base), T-504 (retirada de la tienda de pausa), T-1110 (pulido de
interacción, pausa y HUD adaptable) y UPD-003-T01 (identidad visual).

## 7. Pruebas existentes

`06_visual_menu`, `07_visual_play`, `08_visual_death`, `13_station`, `14_market`,
`17_player_station`, `18_interiors`, `18_map` y `22_ui_polish`.

## 8. Problemas conocidos

- `MyGame` sigue siendo el orquestador central; no crear objetos `Color` ni
  asignaciones dentro de `render()`.
- Tras una pasada aditiva: `Visuals.normal(batch)` **y** `beginShapes()`.
- La fuente es ASCII; evitar acentos si el glifo no existe.
- `ShapeRenderer.rect(x, y, originX, originY, width, height, scaleX, scaleY,
  degrees)` usa un **origen relativo al rectángulo**; pasar coordenadas
  absolutas dibuja el icono desplazado. El engranaje de `UiKit` lo documenta.
- Las etiquetas dibujadas a mano deben centrarse con el centro real del botón
  (`textCenteredMiddle`), no con una Y fija.

## 9. Dependencias

Todos los sistemas visibles, entrada, audio, accesibilidad y render.
