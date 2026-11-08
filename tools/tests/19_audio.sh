#!/usr/bin/env bash
# Test: servicio de audio local y opción de silencio
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de audio"
sleep 0.5

status=$(latest_status)
assert_eq "audio activo" false "$(printf '%s' "$status" | json_get audioMuted)"
tap "$PAUSE_X" "$PAUSE_Y" 0.4
wait_current_state PAUSED 5 || fail "no abrió la pausa"
sleep 0.4
tap "$((W / 2))" "$MUTE_Y" 0.4
sleep 0.3
status=$(latest_status)
assert_eq "silencio activado" true "$(printf '%s' "$status" | json_get audioMuted)"
tap "$((W / 2))" "$MUTE_Y" 0.4
sleep 0.3
status=$(latest_status)
assert_eq "silencio desactivado" false "$(printf '%s' "$status" | json_get audioMuted)"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.4
wait_current_state PLAYING 5 || fail "no reanudó tras el audio"

check_no_crash
echo "OK 19_audio: efectos locales y silencio conmutable"
