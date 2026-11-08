# Panel de continuidad

Panel visual autocontenido de GalacticIndustry. Se abre con doble clic en
`index.html` y funciona con `file://` sin servidor, CDN ni red.

## Archivos

| Archivo | Responsabilidad |
|---|---|
| `index.html` | Estructura del panel y bloque `tracker-data` con los datos embebidos para `file://` |
| `dashboard.css` | Tema oscuro/claro, tarjetas, tablas, badges y responsive |
| `dashboard.js` | Render, filtros, agrupación, orden, enlaces directos, temas y refresco por HTTP |

`docs/PROJECT_DASHBOARD.html` se conserva como redirección de compatibilidad
para enlaces antiguos.

## Datos

`index.html` es la única parte que contiene el tracker integrado, porque
`file://` no permite leer `IMPLEMENTATION_TRACKER.json` con `fetch()`. El bloque
entre `<!-- TRACKER_DATA_START -->` y `<!-- TRACKER_DATA_END -->` se regenera
con:

```sh
python3 tools/sync-dashboard.py
python3 tools/sync-dashboard.py --check
```

No edites ese bloque a mano. Si el panel se sirve por HTTP, el botón
**Actualizar** relee `../IMPLEMENTATION_TRACKER.json` sin recargar la página.
