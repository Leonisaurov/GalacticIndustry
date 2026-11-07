# GalacticIndustry

Juego Android en **Java + libGDX 1.14.2**, construido con **apkc + apkc-deps**.
No hay Gradle ni `module.toml`: no los reintroduzcas.

## Comandos (siempre desde la raíz del repo)

- Dependencias: `apkc-deps sync` tras tocar `deps.json`. Añadir/quitar con
  `apkc-deps add/remove`, nunca copiando jars a mano en `jar/`.
- Build: `apkc build --inc` → `build/app.apk` (firma debug automática).
- Instalar/ejecutar: `apkc run --inc` (adb) o `apkc run --inc --rish` (Shizuku).
- Watch: `apkc watch --inc --run`.
- Diagnóstico: `apkc-deps doctor`, `apkc-deps verify`, `apkc-deps dex-check`.

En este portátil, envuelve los builds pesados con `tcr`. El dexado de
dependencias lanza varios `d8` y el perfil quiet puede matar el build por RAM
libre baja; si pasa:

```sh
APKC_JOBS=1 tcr -c 3 -m 2G -r 128M apkc build --inc
```

## Pruebas

`SelfTest` es el arnés de lógica sin dispositivo (no usa Android ni ventana).
Requiere que `apkc build` haya creado `build/classes`:

```sh
apkc build --inc
java -cp "build/classes:jar/*" com.example.codegame.SelfTest
```

Imprime `PASS`/`FAIL` y termina en `TODO OK` si todo pasa. Incluye benchmarks
del piloto automático (`BENCH`/`TRAZA`), no solo aserciones.

Verificación del APK: `aapt2 dump badging build/app.apk` y
`apksigner verify --print-certs build/app.apk`.

## Layout

- `src/com/example/codegame/` — todo el código Java.
- `res/`, `assets/`, `lib/<abi>/*.so`, `AndroidManifest.xml` — app.
- `jar/`, `res/__deps/`, `deps.installed.json` — generados por apkc-deps
  (ignorados, no editar).
- `src/**/R.java` y `src/**/Manifest.java` — generados por aapt2 en cada build
  (ignorados, no editar).
- `deps.json` y `deps.lock.json` — sí se versionan.

## Compatibilidad con el layout antiguo (`module.toml`)

El layout canónico es el de apkc. Para construir también con la herramienta
anterior no hay una segunda copia: hay una proyección generada.

```sh
tools/make-legacy.sh
```

Crea `legacy/` (ignorado) con `app/module.toml` (plantilla versionada en
`tools/legacy-module.toml`) y enlaces **por fichero** a `src/`, `res/`,
`assets/` y `lib/`. Excluye los `R.java`/`Manifest.java` generados por aapt2 y
`res/__deps/` de apkc-deps, que romperían el otro build. Las ediciones se ven
sin regenerar; ejecuta el script solo tras añadir, renombrar o borrar ficheros.
No enlaza directorios, así que las herramientas que recorren el árbol sin
seguir symlinks de directorio también ven el contenido.

La herramienta antigua no está instalada en este equipo: el layout se validó
compilando los enlaces con `javac`, no ejecutando ese build.

## Dependencias y nativos

- Directas en `deps.json`: `gdx` y `gdx-backend-android` 1.14.2.
- `gdx-box2d` está deliberadamente fuera: el código no usa Box2D y no hay
  `libgdx-box2d.so`. No lo añadas sin traer ese nativo.
- `lib/<abi>/libgdx.so` (arm64-v8a, armeabi-v7a, x86_64) son nativos puestos a
  mano; **`lib/` no está ignorado y debe seguir versionado**. `settings.abis`
  en `deps.json` declara las tres ABIs.
- `AndroidManifest.xml` debe conservar `<uses-sdk>` (minSdk 26, targetSdk 36):
  apkc lo usa para `d8` y para el empaquetado. `versionCode`/`versionName` se
  suben a mano antes de publicar.

### Trampa: recursos dentro de `jar/`

apkc solo lleva las **clases** de `jar/` al DEX; los recursos internos de los
jars (`.fnt`, `.png`, shaders, etc.) **no** se empaquetan. Por eso `MyGame` no
usa `new BitmapFont()` (carga `com/badlogic/gdx/utils/lsans-15.*` del classpath
y revienta en el dispositivo): la fuente está copiada en
`assets/fonts/lsans-15.fnt` + `.png` y se carga con
`Gdx.files.internal("fonts/lsans-15.fnt")`. Si una librería necesita un recurso
de su jar, cópialo a `assets/` y cárgalo explícitamente.

## Arquitectura

- `MainActivity` → `MyGame` (`ApplicationListener`).
- `MyGame`: máquina de estados `MENU`/`PLAYING`/`PAUSED`/`SHOP`/`OVER`, entrada
  multitáctil (joystick + barra de aceleración) y teclado, HUD/menús, y
  guardado en `Preferences` (clave `run`).
- `GameWorld`: simulación. Mundo infinito por *chunks* de 800 unidades con
  semilla derivada de las coordenadas; dueño de `Ship`, arrays de
  `asteroids`/`missiles`/`fuel`, `Effects` y `Autopilot`. `serialize()`/
  `restore()` usan texto plano.
- `Upgrades`: tienda, monedas y niveles. `Upgrades(true)` usa un almacén en
  memoria para tests; `Upgrades()` usa `Preferences` de Android.
- `Autopilot`: lleva la nave a un bidón o meteorito; cualquier mando manual lo
  cancela. `SelfTest` mide su eficiencia, no solo su corrección.
- Entidades/UI auxiliares: `Ship`, `Asteroid`, `Missile`, `FuelPickup`,
  `Effects`, `Button`, `Controls`.

## Convenciones

- Comentarios, textos de UI y mensajes de test en **español**.
- Ángulos en grados, 0 = derecha, sentido antihorario.
- Acelerar y disparar consumen combustible; no rompas esa economía sin
  actualizar `SelfTest`.
