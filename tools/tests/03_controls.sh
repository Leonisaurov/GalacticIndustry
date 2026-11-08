#!/usr/bin/env bash
# Test: controles de partida, acelerador y pausa (sin tienda)
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true"
wait_current_state MENU 10 || fail "no se llegó al menú"
tap "$((W / 2))" "$MENU_PLAY_Y" 0.2
wait_current_state PLAYING 5 || fail "no empezó la partida"
sleep 1.0
status=$(latest_status)
assert_eq "estado en partida" PLAYING "$(printf '%s' "$status" | json_get state)"
assert_ge "asteroides generados" 1 "$(printf '%s' "$status" | json_get asteroids)"
assert_ge "bidones generados" 1 "$(printf '%s' "$status" | json_get pickups)"
assert_ge "fps" 1 "$(printf '%s' "$status" | json_get fps)"
assert_eq "modo inmortal" true "$(printf '%s' "$status" | json_get immortal)"
fuel_before=$(printf '%s' "$status" | json_get fuel)
hull_before=$(printf '%s' "$status" | json_get hull)

ensure_throttle "$((W - PAD - 30 * U))" "$((H * 78 / 100))" 60 \
    || fail "el gesto del acelerador no se registró"
status=$(latest_status)
assert_lt "el acelerador consume combustible" "$fuel_before" "$(printf '%s' "$status" | json_get fuel)"
assert_ge "nivel de acelerador" 0.10 "$(printf '%s' "$status" | json_get throttle)"
assert_eq "sigue con el casco intacto" "$hull_before" "$(printf '%s' "$status" | json_get hull)"

tap "$PAUSE_X" "$PAUSE_Y" 0.2
wait_current_state PAUSED 5 || fail "no se pausó"
status=$(latest_status)
assert_eq "pausa sin tienda" PAUSED "$(printf '%s' "$status" | json_get state)"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.2
wait_current_state PLAYING 5 || fail "no se reanudó la partida"

check_no_crash
echo "OK 03_controls: PLAYING (inmortal, consume combustible) -> PAUSED -> PLAYING"
