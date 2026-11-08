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
PAUSE_X=0
PAUSE_Y=0
PAUSE_CONTINUE_Y=0
PAUSE_MENU_Y=0
OVER_RETRY_Y=0
OVER_MENU_Y=0
RECOVER_X=0
RECOVER_Y=0
DOCK_X=0
DOCK_Y=0
UNDOCK_X=0
UNDOCK_Y=0
ST_TAB_SERV_X=0
ST_TAB_UPG_X=0
ST_TAB_MOD_X=0
ST_TAB_MKT_X=0
ST_TAB_Y=0
ST_ROW1_Y=0
ST_ROW2_Y=0
ST_ROW3_Y=0
ST_ROW4_Y=0
ST_ROW5_Y=0
ST_UPG1_Y=0
ST_UNDOCK_Y=0
RESCUE_X=0
RESCUE_Y=0
MC_QTY_MINUS10_X=0
MC_QTY_MINUS1_X=0
MC_QTY_PLUS1_X=0
MC_QTY_PLUS10_X=0
MC_QTY_Y=0
MC_TODO_X=0
MC_NADA_X=0
MC_TODO_Y=0
MC_ACTION_Y=0
MC_BUY_X=0
MC_SELL_X=0
MC_BACK_X=0
MC_BACK_Y=0
MAP_BTN_X=0
MAP_BTN_Y=0
MAP_TRAVEL_X=0
MAP_TRAVEL_Y=0
BASE_X=0
BASE_Y=0
MG_UPGRADE_X=0
MG_ACTION1_Y=0
MG_DEPOSIT_X=0
MG_ACTION2_Y=0
MG_UNDOCK_Y=0
MG_ENTER_X=0
MG_ENTER_Y=0
MUTE_Y=0
MUSIC_Y=0

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

    MENU_PLAY_Y=$((H * 67 / 100))
    PAUSE_X=$((W - PAD - 27 * U))
    PAUSE_Y=$((67 * U))
    PAUSE_CONTINUE_Y=$((H / 2 - 78 * U))
    PAUSE_MENU_Y=$((H / 2 - 6 * U))
    OVER_RETRY_Y=$((H / 2 + 28 * U))
    OVER_MENU_Y=$((H / 2 + 88 * U))
    # Botón RECUPERAR NAVE: centro de la pantalla, a 30 % de altura + 23 u.
    RECOVER_X=$((W / 2))
    RECOVER_Y=$((H - (H * 30 / 100 + 23 * U)))
    # Botones contextuales ACOPLAR (38 %) y DESPEGAR (30 %).
    DOCK_X=$((W / 2))
    DOCK_Y=$((H - (H * 38 / 100 + 23 * U)))
    UNDOCK_X=$((W / 2))
    UNDOCK_Y=$((H - 68 * U))
    # Pantalla de estación: pestañas y filas (5 servicios / 8 mejoras).
    local st_px=$((8 * U))
    local st_pw=$((W - 16 * U))
    local st_tab_w=$(( (st_pw - 70 * U) / 4 ))
    ST_TAB_SERV_X=$(( st_px + 20 * U + st_tab_w / 2 ))
    ST_TAB_UPG_X=$(( st_px + 30 * U + st_tab_w + st_tab_w / 2 ))
    ST_TAB_MOD_X=$(( st_px + 40 * U + 2 * st_tab_w + st_tab_w / 2 ))
    ST_TAB_MKT_X=$(( st_px + 50 * U + 3 * st_tab_w + st_tab_w / 2 ))
    ST_TAB_Y=$((76 * U))
    local st_list_top=$((H - 160 * U))
    local st_bottom=$((8 * U + 100 * U))
    local st_gap=$((8 * U))
    local st_avail=$((st_list_top - st_bottom))
    local st_row_h=$(( (st_avail - 4 * st_gap) / 5 ))
    [ "$st_row_h" -gt $((200 * U)) ] && st_row_h=$((200 * U))
    ST_ROW1_Y=$((H - (st_list_top - st_row_h / 2)))
    ST_ROW2_Y=$((H - (st_list_top - (st_row_h + st_gap) - st_row_h / 2)))
    ST_ROW3_Y=$((H - (st_list_top - 2 * (st_row_h + st_gap) - st_row_h / 2)))
    ST_ROW4_Y=$((H - (st_list_top - 3 * (st_row_h + st_gap) - st_row_h / 2)))
    ST_ROW5_Y=$((H - (st_list_top - 4 * (st_row_h + st_gap) - st_row_h / 2)))
    local st_row_h8=$(( (st_avail - 7 * st_gap) / 8 ))
    [ "$st_row_h8" -gt $((200 * U)) ] && st_row_h8=$((200 * U))
    ST_UPG1_Y=$((H - (st_list_top - st_row_h8 / 2)))
    # UPD-006: el catalogo de modulos tiene 7 filas.
    local st_row_h7=$(( (st_avail - 6 * st_gap) / 7 ))
    [ "$st_row_h7" -gt $((200 * U)) ] && st_row_h7=$((200 * U))
    ST_MOD1_Y=$((H - (st_list_top - st_row_h7 / 2)))
    ST_MOD2_Y=$((H - (st_list_top - (st_row_h7 + st_gap) - st_row_h7 / 2)))
    ST_MOD3_Y=$((H - (st_list_top - 2 * (st_row_h7 + st_gap) - st_row_h7 / 2)))
    ST_MOD5_Y=$((H - (st_list_top - 4 * (st_row_h7 + st_gap) - st_row_h7 / 2)))
    ST_UNDOCK_Y=$((H - (8 * U + 60 * U)))
    RESCUE_X=$((W / 2))
    RESCUE_Y=$((H - (H * 46 / 100 + 23 * U)))
    # Mercado: filas, cantidades y acciones.
    local mc_py=$((8 * U))
    local mc_pw=$((W - 16 * U))
    local mc_qty_w=$(( (mc_pw - 44 * U) / 5 ))
    MC_QTY_MINUS10_X=$((8 * U + 22 * U + (mc_qty_w - 4 * U) / 2))
    MC_QTY_MINUS1_X=$((MC_QTY_MINUS10_X + mc_qty_w))
    MC_QTY_PLUS1_X=$((MC_QTY_MINUS10_X + 3 * mc_qty_w))
    MC_QTY_PLUS10_X=$((MC_QTY_MINUS10_X + 4 * mc_qty_w))
    MC_QTY_Y=$((H - (mc_py + 217 * U)))
    local mc_half=$(( (mc_pw - 52 * U) / 2 ))
    MC_TODO_X=$((8 * U + 22 * U + mc_half / 2))
    MC_NADA_X=$((8 * U + 22 * U + mc_half + 8 * U + mc_half / 2))
    MC_TODO_Y=$((H - (mc_py + 166 * U)))
    MC_ACTION_Y=$((H - (mc_py + 116 * U)))
    MC_BUY_X=$((MC_TODO_X))
    MC_SELL_X=$((MC_NADA_X))
    MC_BACK_X=$((W - 8 * U - 96 * U + 39 * U))
    MC_BACK_Y=$((36 * U))
    # Mapa: botón MAPA en el HUD y VIAJAR en el pie del mapa.
    MAP_BTN_X=$((W / 2))
    MAP_BTN_Y=$((H - 38 * U))
    local map_half=$(( (W - 64 * U) / 2 ))
    MAP_TRAVEL_X=$((28 * U + map_half / 2))
    MAP_TRAVEL_Y=$((H - 118 * U))
    # Botón CONSTRUIR BASE (centro, 54 % de altura + 23 u).
    BASE_X=$((W / 2))
    BASE_Y=$((H - (H * 54 / 100 + 23 * U)))
    # Gestión de la estación personal.
    local mg_px=$((8 * U))
    local mg_pw=$((W - 16 * U))
    local mg_half=$(( (mg_pw - 48 * U) / 2 ))
    MG_UPGRADE_X=$((mg_px + 20 * U + mg_half + 8 * U + mg_half / 2))
    MG_ACTION1_Y=$((H - 197 * U))
    MG_DEPOSIT_X=$((mg_px + 20 * U + mg_half / 2))
    MG_ACTION2_Y=$((H - 101 * U))
    MG_UNDOCK_Y=$((H - 50 * U))
    MG_ENTER_X=$((W - 74 * U))
    MG_ENTER_Y=$((37 * U))
    # Botones de audio en la pausa (panel centrado de 440 U).
    MUTE_Y=$((H / 2 + 56 * U))
    MUSIC_Y=$((H / 2 + 112 * U))
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
