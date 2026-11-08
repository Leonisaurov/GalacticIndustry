# Sistema — Accesibilidad y localización

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Parcial** |
| Código | `MyGame.java`, `UiKit.java`, `docs/convenciones.md` |
| Documentación | `arquitectura.md` §Accesibilidad y localización (T-1106); `convenciones.md` §Accesibilidad |
| Decisiones | Ninguna específica |
| Tareas | T-1106 |
| Pruebas | `20_accessibility`, `22_ui_polish` |

## 1. Descripción

Reglas de legibilidad, botones táctiles, navegación Atrás y textos ajustados. La
interfaz está en español y usa una fuente ASCII. La localización completa a otros
idiomas queda pendiente.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Textos ajustados | Implementado | `UiKit` adapta el texto a los botones. |
| Botones táctiles | Implementado | Áreas amplias y estados deshabilitados explicados. |
| Navegación Atrás | Implementado | Pausa/reanuda y vuelve de pantallas. |
| Fuente ASCII | Implementado | Evita glifos inexistentes. |
| Contraste/temas | Parcial | Panel con temas; UI de juego con paleta fija. |
| Localización multiidioma | No implementado | Solo español. |
| Lectores de pantalla | No implementado | Sin capa de accesibilidad nativa específica. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `MyGame` | Manejo de Atrás, avisos y navegación. |
| `UiKit` | Botones, tamaños y ajuste de texto. |
| `convenciones.md` | Reglas de estilo y accesibilidad. |

## 4. Flujo

1. Las pantallas usan `UiKit` para dibujar botones y texto.
2. Atrás pausa/reanuda o vuelve a la pantalla anterior.
3. Las acciones deshabilitadas muestran el motivo.
4. La fuente ASCII limita acentos y símbolos.

## 5. Decisiones aplicables

No hay una decisión de producto específica. La accesibilidad se trata como
requisito de calidad, no como sistema jugable independiente.

## 6. Tareas relacionadas

T-1106 (accesibilidad y localización), T-1110 (pulido de interacción y HUD).

## 7. Pruebas existentes

`20_accessibility` (navegación, tamaños y textos), `22_ui_polish` (sin solapes).

## 8. Problemas conocidos

- La fuente `lsans-15` es ASCII; no introducir acentos sin comprobar el glifo.
- No hay traducción a otros idiomas.
- No hay soporte específico para lectores de pantalla.
- El contraste y los temas se aplican al panel, no a toda la UI de juego.

## 9. Dependencias

Interfaz/HUD, render, convenciones y pruebas visuales.
