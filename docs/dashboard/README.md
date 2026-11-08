# Panel de continuidad

Panel visual autocontenido de GalacticIndustry. Se abre con doble clic en
`index.html` y funciona con `file://` sin servidor, CDN ni red.

## Archivos

| Archivo | Responsabilidad |
|---|---|
| `index.html` | Estructura del panel y bloque `tracker-data` con los datos embebidos para `file://` |
| `dashboard.css` | Tema oscuro/claro, tarjetas, tablas, badges y responsive |
| `dashboard.js` | Render, filtros, agrupación, orden, enlaces directos, actualizaciones, propuestas, expedientes, temas y refresco por HTTP |

`docs/PROJECT_DASHBOARD.html` se conserva como redirección de compatibilidad
para enlaces antiguos.

## Documentación relacionada

- [`../INDEX.md`](../INDEX.md) — mapa de sistemas, código, diseño, decisiones,
  tareas y pruebas.
- [`../STATUS.md`](../STATUS.md) — resumen compacto generado desde el tracker.
- [`../IMPLEMENTATION_TRACKER.json`](../IMPLEMENTATION_TRACKER.json) — fuente
  canónica del estado.

El panel enlaza a `INDEX.md` y `STATUS.md` desde la cabecera y el pie. En el
detalle de cada tarea muestra `contextFiles`, `contextDocs` y `artifacts`
cuando existen; `files` se conserva por compatibilidad.

## Datos

`index.html` es la única parte que contiene el tracker integrado, porque
`file://` no permite leer `IMPLEMENTATION_TRACKER.json` con `fetch()`. El bloque
entre `<!-- TRACKER_DATA_START -->` y `<!-- TRACKER_DATA_END -->` se regenera
con:

```sh
python3 tools/sync-dashboard.py          # panel + docs/STATUS.md
python3 tools/sync-dashboard.py --check  # valida HTML y STATUS.md
```

No edites ese bloque a mano. Si el panel se sirve por HTTP, el botón
**Actualizar** relee `../IMPLEMENTATION_TRACKER.json` sin recargar la página.

## Actualizaciones y revisiones

La sección **Actualizaciones** muestra:

- el plan original como tarjeta histórica contraída;
- las actualizaciones complementarias en orden cronológico;
- la actualización actual (`meta.currentUpdateId`) expandida;
- las anteriores contraídas, expandibles manualmente.

La tarjeta de cada actualización muestra estado, tipo, prioridad, progreso,
sistemas y funcionalidades afectadas, referencias históricas, aprobación,
criterios, tareas `UPD-XXX-TNN`, observaciones y registro de implementación.

Dos acciones abren ventanas modales, no formularios permanentes:

- **Nueva propuesta**: redacta una propuesta, la guarda como borrador local y
  permite descargar `.md`, `.txt` y copiar un bloque JSON para el tracker.
- **Preparar expediente de revisión**: selecciona etapas, tareas, tareas de
  actualización, sistemas, funcionalidades y actualizaciones; descarga un
  expediente `.md` o `.txt` con la sección final `OBSERVACIONES DEL USUARIO`.

Los documentos se generan con `Blob` y `URL.createObjectURL`. No se escribe en
el proyecto desde el navegador: con `file://` eso no es posible. Los borradores
de `localStorage` son temporales y no son la fuente canónica. La persistencia
real es el tracker y los archivos de `docs/updates/`, actualizados por un agente.

Flujo completo: [`docs/updates/README.md`](../updates/README.md).
