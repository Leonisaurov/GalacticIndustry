#!/usr/bin/env bash
# Utilidades compartidas por los tests de `tools/tests/`.
# No se ejecuta directamente: cada test hace `source "$(dirname "$0")/testlib.sh"`.

set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
PKG=com.example.codegame
TAG=AsteroidsTelemetry

W=0
H=0
U=1
PAD=0
BW=0
GAP=0
LOG_MARK=0

MENU_PLAY_Y=0
MENU_SHOP_Y=0
PAUSE_X=0
PAUSE_Y=0
PAUSE_CONTINUE_Y=0
PAUSE_SHOP_Y=0
PAUSE_MENU_Y=0
OVER_RETRY_Y=0
OVER_MENU_Y=0
BACK_X=0
BACK_Y=0
TAB_REPAIR_X=0
TAB_REPAIR_Y=0
TAB_MELT_X=0
TAB_MELT_Y=0
SHOP_FIRST_BTN_X=0
SHOP_FIRST_BTN_Y=0
RECOVER_X=0
RECOVER_Y=0
DOCK_X=0
DOCK_Y=0
UNDOCK_X=0
UNDOCK_Y=0

fail() {
    echo "FALLO: $*" >&2
    exit 1
}

ensure_device() {
    command -v adb >/dev/null 2>&1 || fail "adb no está en PATH"
    adb get-state >/dev/null 2>&1 || fail "no hay dispositivo conectado"
}

ensure_magick() {
    command -v magick >/dev/null 2>&1 || fail "ImageMagick (magick) no está en PATH"
    command -v python3 >/dev/null 2>&1 || fail "python3 no está en PATH"
}

device_geometry() {
    read -r W H < <(adb shell wm size | sed -E 's/.*: ([0-9]+)x([0-9]+).*/\1 \2/')
    local density
    density=$(adb shell wm density | sed -E 's/.*: ([0-9]+).*/\1/')
    U=$((density / 160))
    [ "$U" -ge 1 ] || U=1
    PAD=$((16 * U))
    BW=$((56 * U))
    GAP=$((18 * U))

    MENU_PLAY_Y=$((H * 67 / 100 - (BW + GAP) / 2))
    MENU_SHOP_Y=$((H * 67 / 100 + (BW + GAP) / 2))
    PAUSE_X=$((W - PAD - 27 * U))
    PAUSE_Y=$((50 * U))
    PAUSE_CONTINUE_Y=$((H - (H * 44 / 100 + BW + GAP)))
    PAUSE_SHOP_Y=$((H - H * 44 / 100))
    PAUSE_MENU_Y=$((H - (H * 44 / 100 - BW - GAP)))
    OVER_RETRY_Y=$((H - (H * 41 / 100 + (BW + GAP) / 2)))
    OVER_MENU_Y=$((H - (H * 41 / 100 - (BW + GAP) / 2)))
    BACK_X=$((W / 2))
    BACK_Y=$((H - (PAD + BW / 2)))
    # Pestañas de la tienda con partida: MEJORAS / REPARAR / FUNDIR.
    TAB_REPAIR_X=$((W / 2))
    TAB_REPAIR_Y=$((77 * U))
    TAB_MELT_X=$((W - 24 * U - (W - 64 * U) / 6))
    TAB_MELT_Y=$((77 * U))
    # Botón derecho de la primera fila de la tienda (con una sola fila).
    local shop_card_w=$((W - 44 * U))
    local shop_bbw=$((shop_card_w * 2 / 5))
    [ "$shop_bbw" -gt $((158 * U)) ] && shop_bbw=$((158 * U))
    local shop_row_h=$((H - 210 * U))
    [ "$shop_row_h" -gt $((112 * U)) ] && shop_row_h=$((112 * U))
    SHOP_FIRST_BTN_X=$((W - 32 * U - shop_bbw / 2))
    SHOP_FIRST_BTN_Y=$((112 * U + shop_row_h / 2))
    # Botón RECUPERAR NAVE: centro de la pantalla, a 30 % de altura + 23 u.
    RECOVER_X=$((W / 2))
    RECOVER_Y=$((H - (H * 30 / 100 + 23 * U)))
    # Botones contextuales ACOPLAR (38 %) y DESPEGAR (30 %).
    DOCK_X=$((W / 2))
    DOCK_Y=$((H - (H * 38 / 100 + 23 * U)))
    UNDOCK_X=$((W / 2))
    UNDOCK_Y=$((H - (H * 30 / 100 + 23 * U)))
}

# ---------- build y SelfTest ----------

build_apk() {
    echo "[build]"
    if command -v tcr >/dev/null 2>&1; then
        tcr -c 3 -m 2G -r 128M apkc build --inc
    else
        apkc build --inc
    fi
}

run_selftest() {
    local out=${1:-build/test-logs/selftest.txt}
    mkdir -p "$(dirname "$out")"
    echo "[selftest]"
    java -cp "build/classes:jar/*" "$PKG.SelfTest" > "$out" 2>&1 || true
    grep -q "TODO OK" "$out" || { tail -n 25 "$out" >&2; fail "SelfTest no terminó en TODO OK"; }
}

install_app() {
    echo "[install]"
    adb install -r build/app.apk >/dev/null
    adb shell pm clear "$PKG" >/dev/null
    adb logcat -c
    LOG_MARK=0
}

launch_app() {
    # $1 = extras opcionales para `am start`
    adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
    # shellcheck disable=SC2086
    adb shell am start -n "$PKG/.MainActivity" $1 >/dev/null
}

reset_app() {
    adb shell pm clear "$PKG" >/dev/null
    adb logcat -c
    LOG_MARK=0
}

# Prepara un test de dispositivo. Si lo lanza run-tests.sh (PREPARED=1), el APK
# ya está instalado y solo se limpian datos; si se ejecuta suelto, compila e instala.
prepare_device_module() {
    ensure_device
    device_geometry
    if [ "${DND_ACTIVE:-0}" != 1 ]; then
        dnd_on
    fi
    if [ "${PREPARED:-0}" = 1 ]; then
        reset_app
    else
        build_apk
        install_app
    fi
}

# ---------- avisos: evitar notificaciones personales en capturas ----------

ZEN_OLD=""
HEADS_OLD=""

dnd_restore() {
    [ -n "$ZEN_OLD" ] && adb shell settings put global zen_mode "$ZEN_OLD" >/dev/null 2>&1 || true
    [ -n "$HEADS_OLD" ] && adb shell settings put global heads_up_notifications_enabled "$HEADS_OLD" >/dev/null 2>&1 || true
}

dnd_on() {
    ZEN_OLD=$(adb shell settings get global zen_mode 2>/dev/null | tr -d '\r' || true)
    HEADS_OLD=$(adb shell settings get global heads_up_notifications_enabled 2>/dev/null | tr -d '\r' || true)
    trap dnd_restore EXIT INT TERM
    adb shell settings put global heads_up_notifications_enabled 0 >/dev/null 2>&1 || true
    adb shell settings put global zen_mode 2 >/dev/null 2>&1 || true
}

# ---------- interacción ----------

tap() {
    adb shell input swipe "$1" "$2" "$1" "$2" 200
    sleep "${3:-1}"
}

swipe_horizontal() {
    local x=$1 y=$2 dx=${3:-80}
    adb shell input swipe "$x" "$y" "$((x + dx))" "$y" 450
    sleep "${4:-1}"
}

# Reintenta el gesto del acelerador hasta que la telemetría confirme empuje.
ensure_throttle() {
    local x=$1 y=$2 dx=${3:-60} i value
    for ((i = 0; i < 3; i++)); do
        swipe_horizontal "$x" "$y" "$dx" 0.2
        sleep 1.0
        value=$(latest_status | json_get throttle)
        if awk -v a="${value:-0}" 'BEGIN { exit !(a > 0.05) }'; then
            return 0
        fi
    done
    return 1
}

# ---------- telemetría ----------

log_mark() {
    LOG_MARK=$(adb logcat -d -s "$TAG":I | wc -l)
}

new_logs() {
    adb logcat -d -s "$TAG":I | tail -n +$((LOG_MARK + 1))
}

wait_state() {
    local want=$1 timeout=${2:-8} i n
    n=$((timeout * 10))
    for ((i = 0; i < n; i++)); do
        if new_logs | grep -q "\"state\":\"$want\""; then
            LOG_MARK=$(adb logcat -d -s "$TAG":I | wc -l)
            return 0
        fi
        sleep 0.1
    done
    return 1
}

# Espera a que la última telemetría publicada tenga el estado indicado.
wait_current_state() {
    local want=$1 timeout=${2:-10} i n
    n=$((timeout * 5))
    for ((i = 0; i < n; i++)); do
        if [ "$(latest_status | json_get state)" = "$want" ]; then
            return 0
        fi
        sleep 0.2
    done
    return 1
}

# Espera a que la última telemetría tenga un campo con el valor indicado.
wait_status_field() {
    local field=$1 want=$2 timeout=${3:-10} i n
    n=$((timeout * 5))
    for ((i = 0; i < n; i++)); do
        if [ "$(latest_status | json_get "$field")" = "$want" ]; then
            return 0
        fi
        sleep 0.2
    done
    return 1
}

wait_event() {
    local want=$1 timeout=${2:-8} i n
    n=$((timeout * 10))
    for ((i = 0; i < n; i++)); do
        if new_logs | grep -q "\"event\":\"$want\""; then
            return 0
        fi
        sleep 0.1
    done
    return 1
}

latest_status() {
    # Sin telemetría todavía, grep sale con 1; devolver cadena vacía en vez de
    # abortar el test por pipefail (json_get ya tolera entradas vacías).
    adb logcat -d -s "$TAG":I | grep '"event":"status"' | tail -n 1 | sed 's/^.*{/{/' || true
}

json_get() {
    local field=$1
    python3 -c 'import json,sys; v=json.loads(sys.stdin.read()).get(sys.argv[1],""); print(str(v).lower() if isinstance(v,bool) else v)' "$field" 2>/dev/null || true
}

assert_eq() {
    local name=$1 expected=$2 actual=$3
    [ "$expected" = "$actual" ] || fail "$name: esperado '$expected', obtenido '$actual'"
}

assert_lt() {
    local name=$1 upper=$2 value=$3
    awk -v a="$value" -v b="$upper" 'BEGIN { exit !(a < b) }' \
        || fail "$name: $value no es menor que $upper"
}

assert_ge() {
    local name=$1 lower=$2 value=$3
    awk -v a="$value" -v b="$lower" 'BEGIN { exit !(a >= b) }' \
        || fail "$name: $value no es mayor o igual que $lower"
}

check_no_crash() {
    mkdir -p build/test-logs
    if adb logcat -d -s AndroidRuntime:E | grep -q "$PKG"; then
        adb logcat -d -s AndroidRuntime:E > build/test-logs/crash.txt
        fail "hay una excepción de $PKG en logcat (ver build/test-logs/crash.txt)"
    fi
}

# ---------- capturas (solo para diseño) ----------

# Captura rápida: no analiza la imagen para no perder fotogramas de animación.
shot() {
    local dir=$1 name=$2
    mkdir -p "$dir"
    adb exec-out screencap -p > "$dir/$name.png"
    [ -s "$dir/$name.png" ] || fail "captura vacía: $name"
    echo "  captura $name"
}

# Valida después de capturar, sobre una miniatura para que sea rápido.
validate_shot() {
    local file=$1
    local colors sd
    colors=$(magick "$file" -sample 20% -format '%k' info:)
    sd=$(magick identify -format '%[fx:standard_deviation]' "$file")
    [ "$colors" -ge 80 ] || fail "$(basename "$file") parece vacía ($colors colores)"
    awk -v sd="$sd" 'BEGIN { exit !(sd > 0.010) }' || fail "$(basename "$file") tiene muy poco contraste (sd=$sd)"
}

validate_shots() {
    local dir=$1
    for f in "$dir"/*.png; do
        [ -e "$f" ] || continue
        validate_shot "$f"
        echo "  ok $(basename "$f")"
    done
}
