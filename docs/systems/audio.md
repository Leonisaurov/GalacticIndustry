# Sistema — Audio y música

[← Índice](../INDEX.md) · [Estado](../STATUS.md) · [Tracker](../IMPLEMENTATION_TRACKER.json)

| Campo | Valor |
|---|---|
| Estado | **Implementado** (audio local) |
| Código | `AudioService.java`, `assets/audio/`, `MyGame.java` |
| Documentación | `MASTER_PLAN.md` §11.5; `arquitectura.md` §Audio y efectos (T-1104), §Música y ambientes dinámicos (T-1108) |
| Decisiones | D-009 (audio, música y ambientación) |
| Tareas | T-1104, T-1108 |
| Pruebas | `19_audio`, `21_music` |

## 1. Descripción

Efectos, bucle de motor, música y ambientes dinámicos con recursos locales. No
hay streaming ni audio online. El jugador puede silenciar sonido y música.

## 2. Estado real

| Parte | Estado | Detalle |
|---|---|---|
| Efectos | Implementado | Disparo, impacto, explosión, recogida, etc. |
| Motor | Implementado | Bucle ligado al empuje. |
| Música | Implementado | Pistas locales y cambios de ambiente. |
| Ambientes | Implementado | Exploración, tensión, estación. |
| Silencio | Implementado | Opciones de sonido y música. |
| Audio online | No implementado | Sin red. |
| Recursos en jars | Prohibido | Deben vivir en `assets/`. |

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| `AudioService` | Carga, reproducción, bucles, volumen y silencio. |
| `assets/audio/` | Recursos locales. |
| `MyGame` | Opciones y disparo de efectos/música. |

## 4. Flujo

1. `AudioService` carga los recursos al iniciar.
2. Los eventos del juego solicitan efectos o cambios de música.
3. El bucle de motor sigue el empuje.
4. Las opciones de audio persisten con el perfil.

## 5. Decisiones aplicables

- **D-009:** experiencia audiovisual completa, con música ambiental y efectos;
  sin dependencia de red.

## 6. Tareas relacionadas

T-1104 (infraestructura de audio), T-1108 (música y ambientes).

## 7. Pruebas existentes

`19_audio` (efectos y silencio), `21_music` (música y ambientes).

## 8. Problemas conocidos

- No hay streaming; los recursos deben estar en `assets/audio/`.
- No meter recursos de jars en el APK.
- Los cambios de audio no deben bloquear el hilo de render.

## 9. Dependencias

Interfaz, perfil/persistencia, assets y opciones de usuario.
