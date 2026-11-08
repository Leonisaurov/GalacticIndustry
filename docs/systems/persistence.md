# Sistema — Persistencia y guardado

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** |
| Código | `SaveService.java`, `SaveCodec.java`, `GameSession.java`, `PlayerProfile.java`, `SectorRuntime.java` |
| Documentación | `MASTER_PLAN.md` §11.3; `arquitectura.md` §Persistencia y descarga de sectores |
| Decisiones | D-008 (formato y ubicación del guardado) |
| Tareas | T-104, T-505, T-605, T-803, T-905 |
| Pruebas | `01_logic`, `13_station`, `14_market`, `16_universe` |

## 1. Descripción

Guardado local JSON versionado (v2) con perfil, piloto, nave y sector. Incluye
backup, migración desde el formato heredado `run2` y snapshots de sectores. No
usa nube ni red.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Guardado v2 | Implementado | `SaveCodec`: `profile`, `pilot`, `ship`, `sector`. |
| Escritura atómica | Implementado | Temporal + backup + sustitución. |
| Backup | Implementado | Conserva la versión anterior. |
| Migración | Implementado | Desde `run2` y guardados sin piloto/soporte. |
| Piloto y soporte vital | Implementado | Salud, O2, filtro, fuga y brecha. |
| Snapshots de sector | Implementado | `SectorRuntime`. |
| Nube | No implementado | Solo almacenamiento local. |
| Guardado de detalle de sectores no visitados | Parcial | Snapshot resumido. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `SaveService` | Archivo local, escritura atómica, backup y migración. |
| `SaveCodec` | Serialización/deserialización JSON v2. |
| `GameSession` | Composición de perfil, piloto, nave y sector; `saveRun()`/`loadRun()`. |
| `PlayerProfile` | Identidad y progreso. |
| `SectorRuntime` | Snapshot del sector. |

## 4. Flujo

1. `GameSession.saveRun()` compone el estado y llama a `SaveService`.
2. `SaveService` escribe un temporal, conserva backup y sustituye.
3. Al cargar, `SaveService` valida la versión y migra si hace falta.
4. `SaveCodec` restaura perfil, piloto, nave y sector.
5. Los guardados antiguos sin piloto/soporte usan valores iniciales.

## 5. Decisiones aplicables

- **D-008:** JSON local con backup; sin red.

## 6. Tareas relacionadas

T-104 (guardado v2), T-505 (almacén), T-605 (mercado), T-803 (sectores), T-905
(estación personal).

## 7. Pruebas existentes

`01_logic` (serialización, migración y compatibilidad), `13_station`,
`14_market`, `16_universe`.

## 8. Problemas conocidos

- El guardado es local; no hay sincronización entre dispositivos.
- La migración desde `run2` se conserva por compatibilidad; no eliminarla sin
  una migración equivalente.
- Los sectores no visitados solo conservan un snapshot resumido.
- Cambiar el esquema exige actualizar `SaveCodec`, `SelfTest` y la documentación.

## 9. Dependencias

Perfil, piloto/supervivencia, nave, universo, estaciones y estación personal.
