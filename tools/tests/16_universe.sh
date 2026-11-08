#!/usr/bin/env bash
# Test: nuevo personaje, mundo persistente y recuperación de nave legado
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez pilot_death_test true"
wait_current_state OVER 10 || fail "no se llegó al fin por piloto"
tap "$((W / 2))" "$OVER_RETRY_Y" 0.2
wait_current_state PLAYING 6 || fail "no se creó el nuevo personaje"
sleep 0.6

status=$(latest_status)
assert_eq "mundo legado presente" true "$(printf '%s' "$status" | json_get legacyWreck)"
assert_eq "nave legado cerca" true "$(printf '%s' "$status" | json_get legacyNear)"
coins_before=$(printf '%s' "$status" | json_get coins)

tap "$RECOVER_X" "$RECOVER_Y" 0.3
sleep 0.6
status=$(latest_status)
assert_eq "nave legado recuperada" false "$(printf '%s' "$status" | json_get legacyWreck)"
assert_eq "nave legado ya no está cerca" false "$(printf '%s' "$status" | json_get legacyNear)"
coins_after=$(printf '%s' "$status" | json_get coins)
assert_ge "el desguace da créditos" "$((coins_before + 10))" "$coins_after"

check_no_crash
echo "OK 16_universe: nuevo personaje, mundo persistente y nave legado recuperada"
