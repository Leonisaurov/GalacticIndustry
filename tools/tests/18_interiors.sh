#!/usr/bin/env bash
# Test: marco de interiores derivado de la estación personal
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez module_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de interiores"
sleep 0.5

status=$(latest_status)
assert_eq "tipo de interior" PLAYER_STATION "$(printf '%s' "$status" | json_get interiorKind)"
assert_eq "habitaciones" 3 "$(printf '%s' "$status" | json_get interiorRooms)"
assert_eq "puertas" 2 "$(printf '%s' "$status" | json_get interiorDoors)"
assert_eq "interactuables" 3 "$(printf '%s' "$status" | json_get interiorInteractables)"

check_no_crash
echo "OK 18_interiors: núcleo + 2 módulos, 2 puertas y 3 interactuables"
