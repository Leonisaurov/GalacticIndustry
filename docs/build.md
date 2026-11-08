# Build, dependencias y layout

## Comandos (desde la raíz del repo)

- Dependencias: `apkc-deps sync` tras tocar `deps.json`. Añadir/quitar con
  `apkc-deps add/remove`, nunca copiando jars a mano en `jar/`.
- Build: `apkc build --inc` → `build/app.apk` (firma debug automática).
- Instalar/ejecutar: `apkc run --inc` (adb) o `apkc run --inc --rish` (Shizuku).
- Instalar el APK ya construido: `adb install -r build/app.apk`. Si da
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, la versión instalada está firmada con
  otra clave: hay que desinstalar (se pierde el progreso guardado) o firmar con
  esa clave.
- Watch: `apkc watch --inc --run`.
- Diagnóstico: `apkc-deps doctor`, `apkc-deps verify`, `apkc-deps dex-check`.

En este portátil, envuelve los builds pesados con `tcr`. El dexado de
dependencias lanza varios `d8` y el perfil quiet puede matar el build por RAM
libre baja; si pasa:

```sh
APKC_JOBS=1 tcr -c 3 -m 2G -r 128M apkc build --inc
```

Verificación del APK:

```sh
aapt2 dump badging build/app.apk
aapt2 dump resources build/app.apk
apksigner verify --print-certs build/app.apk
unzip -l build/app.apk
```

## Layout

- `src/com/example/codegame/` — todo el código Java.
- `res/`, `assets/`, `lib/<abi>/*.so`, `AndroidManifest.xml` — app.
- `tools/` — `run-tests.sh` y `tests/` (tests modulares y `testlib.sh`). No
  forman parte del APK.
- `docs/` — documentación especializada; `AGENTS.md` es solo el índice.
- `app/module.toml` — puente de CodeAssist (ver más abajo).
- `jar/`, `res/__deps/`, `deps.installed.json` — generados por apkc-deps
  (ignorados, no editar).
- `src/**/R.java` y `src/**/Manifest.java` — generados por aapt2 en cada build
  (ignorados, no editar).
- `deps.json` y `deps.lock.json` — sí se versionan.
- `build/` — APK, clases, log de tests y capturas de diseño; ignorado.

## Dependencias y nativos

- Directas en `deps.json`: `gdx` y `gdx-backend-android` 1.14.2.
- `gdx-box2d` está deliberadamente fuera: el código no usa Box2D y no hay
  `libgdx-box2d.so`. No lo añadas sin traer ese nativo.
- `lib/<abi>/libgdx.so` (arm64-v8a, armeabi-v7a, x86_64) son nativos puestos a
  mano; **`lib/` no está ignorado y debe seguir versionado**. `settings.abis`
  en `deps.json` declara las tres ABIs.
- `AndroidManifest.xml` debe conservar `<uses-sdk>` (minSdk 26, targetSdk 36):
  apkc lo usa para `d8` y para el empaquetado. `versionCode`/`versionName` se
  suben a mano antes de publicar. El nombre visible es **Asteroides**
  (`res/values/strings.xml`).
- La activity está bloqueada en vertical y usa pantalla completa con
  `windowLayoutInDisplayCutoutMode=shortEdges` en `res/values*/themes.xml`.

## CodeAssist (`app/module.toml`)

El layout canónico es el de apkc, pero `app/module.toml` es un puente para
CodeAssist (móvil). CodeAssist guarda en `.platform/workspace.json` (local, no
versionado) un módulo llamado `app` y lee `app/module.toml`; sin ese fichero
falla con "no encuentra el módulo app".

Ese `module.toml` no duplica nada: sus sourceSets apuntan a la raíz con rutas
relativas (`../src`, `../res`, `../assets`, `../lib`) y
`[android] manifest = "../AndroidManifest.xml"`. CodeAssist resuelve `..` sin
problema. Si mueves directorios, actualiza también `app/module.toml`; no lo
borres aunque solo uses apkc.

Los artefactos generados (`.platform/`, `app/build/`, `build/`, `jar/`,
`res/__deps/`, `R.java`) están ignorados, así que no viajan entre dispositivos.
Tras cambiar `app/module.toml` (o hacer pull en el móvil), **cierra y reabre el
proyecto** en CodeAssist: si estaba abierto, conserva en memoria el módulo que
no cargó y sigue diciendo que no encuentra `app`.

## Trampa: recursos dentro de `jar/`

apkc solo lleva las **clases** de `jar/` al DEX; los recursos internos de los
jars (`.fnt`, `.png`, shaders, etc.) **no** se empaquetan. Por eso `MyGame` no
usa `new BitmapFont()` (carga `com/badlogic/gdx/utils/lsans-15.*` del classpath
y revienta en el dispositivo): la fuente está copiada en
`assets/fonts/lsans-15.fnt` + `.png` y se carga con
`Gdx.files.internal("fonts/lsans-15.fnt")`. Si una librería necesita un recurso
de su jar, cópialo a `assets/` y cárgalo explícitamente.

`assets/fonts/lsans-15.fnt` está retocada a mano (espaciado, avance del espacio y
tabla de kerning). Si se reemplaza la fuente, hay que repetir esos ajustes o el
HUD vuelve a verse con las palabras pegadas.
