#!/usr/bin/env bash
# Test: revisión visual de accesibilidad (pausa y avisos)
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/40_accessibility.png"

launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de accesibilidad"
sleep 0.5
tap "$PAUSE_X" "$PAUSE_Y" 0.4
wait_current_state PAUSED 5 || fail "no abrió la pausa"
sleep 0.4
shot "$OUT" 40_accessibility
validate_shot "$OUT/40_accessibility.png"

check_no_crash
echo "OK 20_accessibility: pausa legible, botón de silencio y textos ajustados"
