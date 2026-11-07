#!/usr/bin/env bash
# Regenera legacy/ con el layout que esperaba app/module.toml, a partir del
# layout canónico de apkc. Enlaza ficheros, no directorios: así las herramientas
# que recorren el árbol sin seguir symlinks de directorio también ven el contenido.
#
# Uso: tools/make-legacy.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LEGACY="$ROOT/legacy"
OUT="$LEGACY/app/src/main"

rm -rf "$LEGACY"
mkdir -p "$OUT/java" "$OUT/res" "$OUT/assets" "$OUT/jniLibs"

link() {
    local src="$1" dest="$2"
    mkdir -p "$(dirname "$dest")"
    ln -s "$(realpath --relative-to="$(dirname "$dest")" "$src")" "$dest"
}

# Fuentes de la app: todo menos lo que genera aapt2 en cada build.
while IFS= read -r -d '' f; do
    case "$(basename "$f")" in
        R.java|Manifest.java) continue ;;
    esac
    link "$f" "$OUT/java/${f#"$ROOT/src/"}"
done < <(find "$ROOT/src" -type f -name '*.java' -print0)

# Recursos propios: res/__deps es de apkc-deps y el otro build no debe verlo.
while IFS= read -r -d '' f; do
    link "$f" "$OUT/res/${f#"$ROOT/res/"}"
done < <(find "$ROOT/res" -path "$ROOT/res/__deps" -prune -o -type f -print0)

while IFS= read -r -d '' f; do
    link "$f" "$OUT/assets/${f#"$ROOT/assets/"}"
done < <(find "$ROOT/assets" -type f -print0)

while IFS= read -r -d '' f; do
    link "$f" "$OUT/jniLibs/${f#"$ROOT/lib/"}"
done < <(find "$ROOT/lib" -type f -print0)

link "$ROOT/AndroidManifest.xml" "$OUT/AndroidManifest.xml"
cp "$ROOT/tools/legacy-module.toml" "$LEGACY/app/module.toml"

echo "legacy/ regenerado: $(find "$LEGACY/app" -type l | wc -l) enlaces"
