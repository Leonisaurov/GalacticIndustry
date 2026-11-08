#!/usr/bin/env bash
# Test: el piloto automático frena y limpia el acelerador al destruir el objetivo
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
adb logcat -c
LOG_MARK=0
launch_app "--ez test_mode true --ez autopilot_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba del piloto"
wait_status_field pilot STOP 5 || fail "el piloto no entró en fase STOP"
wait_status_field pilot NONE 10 || fail "el piloto no terminó de frenar"
sleep 0.4
status=$(latest_status)
assert_eq "piloto desactivado" NONE "$(printf '%s' "$status" | json_get pilot)"
assert_lt "velocidad final tras frenar" 12 "$(printf '%s' "$status" | json_get speed)"
assert_lt "acelerador a cero tras el piloto" 0.01 "$(printf '%s' "$status" | json_get throttle)"

check_no_crash
echo "OK 04_autopilot: ASTEROID -> STOP -> NONE; velocidad < 12 u/s y acelerador 0"
