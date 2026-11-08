#!/usr/bin/env bash
# Test: música ambiental local y silencio independiente
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/41_music.png"

launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de música"
sleep 0.6
status=$(latest_status)
assert_eq "música activa" false "$(printf '%s' "$status" | json_get musicMuted)"
assert_eq "modo ambiente" EXPLORE "$(printf '%s' "$status" | json_get musicMode)"
tap "$PAUSE_X" "$PAUSE_Y" 0.4
wait_current_state PAUSED 5 || fail "no abrió la pausa"
sleep 0.4
tap "$((W / 2))" "$MUSIC_Y" 0.4
sleep 0.3
status=$(latest_status)
assert_eq "música silenciada" true "$(printf '%s' "$status" | json_get musicMuted)"
shot "$OUT" 41_music
validate_shot "$OUT/41_music.png"
tap "$((W / 2))" "$MUSIC_Y" 0.4
sleep 0.3
status=$(latest_status)
assert_eq "música reactivada" false "$(printf '%s' "$status" | json_get musicMuted)"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.4
wait_current_state PLAYING 5 || fail "no reanudó"

check_no_crash
echo "OK 21_music: ambiente exploración y silencio de música independiente"
