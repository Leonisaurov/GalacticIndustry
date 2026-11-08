#!/usr/bin/env bash
# Test: capturas de diseño de partida y pausa
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/03_play.png" "$OUT/04_play_thrust.png" "$OUT/05_pause.png"

launch_app "--ez test_mode true"
wait_current_state MENU 10 || fail "no se llegó al menú"
tap "$((W / 2))" "$MENU_PLAY_Y" 0.2
wait_current_state PLAYING 5 || fail "no empezó la partida"
sleep 0.7
shot "$OUT" 03_play

ensure_throttle "$((W - PAD - 30 * U))" "$((H * 78 / 100))" 60 \
    || fail "el gesto del acelerador no se registró"
sleep 0.6
shot "$OUT" 04_play_thrust

tap "$PAUSE_X" "$PAUSE_Y" 0.2
wait_current_state PAUSED 5 || fail "no se pausó"
sleep 0.5
shot "$OUT" 05_pause

validate_shot "$OUT/03_play.png"
validate_shot "$OUT/04_play_thrust.png"
validate_shot "$OUT/05_pause.png"
check_no_crash
echo "OK 07_visual_play: capturas 03_play, 04_play_thrust y 05_pause"
