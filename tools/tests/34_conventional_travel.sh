#!/usr/bin/env bash
# Test: viaje convencional sin motor warp desde el mapa
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez nav_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de viaje convencional"
sleep 0.5
status=$(latest_status)
assert_eq "sin motor warp" 0 "$(printf '%s' "$status" | json_get warpLevel)"

tap "$MAP_BTN_X" "$MAP_BTN_Y" 0.4
wait_current_state MAP 5 || fail "MAPA no abrió el mapa"
sleep 0.3
# Cambiar WARP -> CONVENCIONAL.
mode_w=$((W * 40 / 100))
[ "$mode_w" -gt $((160 * U)) ] && mode_w=$((160 * U))
tap "$((8 * U + 24 * U + mode_w / 2))" "$((H - (8 * U + 190 * U + 19 * U)))" 0.4
sleep 0.3
tap "$MAP_TRAVEL_X" "$MAP_TRAVEL_Y" 0.6
wait_current_state PLAYING 5 || fail "VIAJAR convencional no cerró el mapa"
wait_status_field sectorId sol-belt 6 || fail "el viaje convencional no llegó a sol-belt"
status=$(latest_status)
assert_eq "sin warp tras viajar" 0 "$(printf '%s' "$status" | json_get warpLevel)"
check_no_crash
echo "OK 34_conventional_travel: viaje convencional sin warp"
