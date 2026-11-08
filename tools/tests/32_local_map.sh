#!/usr/bin/env bash
# Test: mapa local, proyeccion, zoom, niebla y fijado de objetivos
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/44_local_map.png"

launch_app "--ez test_mode true --ez nav_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de mapa local"
sleep 0.5

tap "$MAP_BTN_X" "$MAP_BTN_Y" 0.4
wait_current_state MAP 5 || fail "MAPA no abrió el mapa"
sleep 0.3
# Boton LOCAL del mapa de navegacion.
local_w=$((W * 28 / 100))
[ "$local_w" -gt $((120 * U)) ] && local_w=$((120 * U))
local_x=$((W - 16 * U - local_w - 22 * U + local_w / 2))
local_y=$((H - (16 * U + (H - 74 * U) + 20 * U)))
tap "$local_x" "$((H - (H - 74 * U + 20 * U)))" 0.4
wait_current_state LOCAL_MAP 5 || fail "LOCAL no abrió el mapa local"
sleep 0.5
status=$(latest_status)
assert_ge "chunks explorados" 1 "$(printf '%s' "$status" | json_get chunks)"
scale0=$(printf '%s' "$status" | json_get localMapScale)
shot "$OUT" 44_local_map
validate_shot "$OUT/44_local_map.png"

# T14: seleccionar la nodriza con la escala inicial.
station_screen_y=$((H - (H / 2 + 2480 * 12 / 100)))
tap "$((W / 2))" "$station_screen_y" 0.4
sleep 0.4
selected=$(latest_status | json_get localMapSelected)
case "$selected" in
    *NODRIZA*) ;;
    *) fail "no se seleccionó la nodriza: $selected" ;;
esac

# T11/T12: zoom.
zoom_x=$((W - 39 * U))
zoom_y=$((H - (H - 130 * U + 23 * U)))
tap "$zoom_x" "$zoom_y" 0.3
sleep 0.3
scale1=$(latest_status | json_get localMapScale)
python3 - "$scale0" "$scale1" <<'PY'
import sys
a=float(sys.argv[1]); b=float(sys.argv[2])
if not b > a:
    raise SystemExit(f"zoom no aumento: {a} -> {b}")
PY
bw=$((W * 44 / 100))
[ "$bw" -gt $((180 * U)) ] && bw=$((180 * U))
x0=$(( (W - bw * 2 - 6 * U) / 2 ))
pin_x=$((x0 + bw * 12 / 10 / 2))
pin_y=$((H - (16 * U + 12 * U + 44 * U + 6 * U + 22 * U)))
tap "$pin_x" "$pin_y" 0.4
sleep 0.4
status=$(latest_status)
nav_label=$(printf '%s' "$status" | json_get navTargetLabel)
case "$nav_label" in
    *NODRIZA*) ;;
    *) fail "FIJAR OBJETIVO no fijó la nodriza: $nav_label" ;;
esac

check_no_crash
echo "OK 32_local_map: proyección, zoom, niebla y fijado de objetivos"
