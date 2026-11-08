#!/usr/bin/env bash
# Test: plataforma personal vacía y definiciones de módulos
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de estación personal"
sleep 0.5

status=$(latest_status)
assert_eq "plataforma no colocada" false "$(printf '%s' "$status" | json_get playerStationBuilt)"
assert_eq "ranuras" 6 "$(printf '%s' "$status" | json_get playerStationSlots)"
assert_eq "módulos construidos" 0 "$(printf '%s' "$status" | json_get playerStationModules)"
assert_eq "servicios de serie" 0 "$(printf '%s' "$status" | json_get playerStationServices)"
assert_eq "definiciones de módulos" 4 "$(printf '%s' "$status" | json_get moduleDefs)"

check_no_crash
echo "OK 17_player_station: plataforma vacía, 6 ranuras y 0 servicios"
