# Sesiones de agentes

Cada sesión nueva se registra en un archivo independiente
`docs/sessions/S-XXX.md`. Así dos agentes pueden documentar su trabajo a la vez
sin insertar líneas en un único archivo grande.

## Qué contiene una sesión

1. Identificador (`S-XXX`), estado, fecha y agente.
2. Tarea o actualización realizada.
3. Resumen de los cambios.
4. Archivos modificados.
5. Pruebas ejecutadas y resultados.
6. Evidencias.
7. Cambios ajenos detectados.
8. Problemas encontrados.
9. Pendiente o siguiente paso.

## Cómo crear una sesión

```sh
python3 tools/continuity.py new-session \
  --agent "OpenCode / modelo" \
  --title "Título de la sesión" \
  --task T-XXX
```

El comando reserva el siguiente ID bajo bloqueo, crea el archivo con la
plantilla y registra la sesión en el tracker con `outcome: "EN_PROGRESO"` y
`document`. Así `STATUS.md` y el panel muestran sesiones activas y una sesión
interrumpida se puede detectar. Al terminar, envía un plan con
`session.update` (o `closeSession: true`) para cerrarla.

## Relación con el tracker y el log

- `docs/IMPLEMENTATION_TRACKER.json` sigue siendo la fuente canónica. La lista
  `sessions` contiene el resumen estructurado y el campo `document` apunta al
  archivo de la sesión.
- `docs/IMPLEMENTATION_LOG.md` conserva el historial narrativo anterior a S-093.
  No se reescribe para añadir sesiones nuevas.
- `docs/STATUS.md` muestra las sesiones activas y las últimas cerradas.
- El panel muestra las sesiones del tracker y enlaza su documento cuando existe.

## Reglas

- Una sesión no reescribe el archivo de otra.
- Una sesión no se marca completada sin pruebas y evidencias.
- Si se interrumpe, el archivo queda en `EN_PROGRESO` y la tarea conserva
  `assignedSessionId`; la siguiente sesión debe inspeccionarla antes de continuar.
- No se reutilizan IDs.
- No se eliminan sesiones antiguas.
