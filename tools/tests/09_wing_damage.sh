#!/usr/bin/env bash
# Test: ala perdida y reparación en la estación
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/11_wing_lost.png" "$OUT/12_station_repair.png"

launch_app "--ez wing_test true"
wait_current_state PLAYING 8 || fail "no arrancó la prueba de alas"
sleep 0.6

status=$(latest_status)
wing=$(printf '%s' "$status" | python3 -c \
    'import json,sys; print(json.load(sys.stdin)["parts"][6])')
assert_eq "ala derecha destruida" 0.0 "$wing"
shot "$OUT" 11_wing_lost

# La reparación ya no está en la pausa: se hace en la estación.
reset_app
launch_app "--ez test_mode true --ez station_service_test true"
wait_current_state DOCKED 10 || fail "no se abrió la estación"
sleep 0.5
shot "$OUT" 12_station_repair
tap "$((W / 2))" "$ST_ROW5_Y" 0.4
sleep 0.5
status=$(latest_status)
wing_after=$(printf '%s' "$status" | python3 -c \
    'import json,sys; print(json.load(sys.stdin)["parts"][6])')
assert_eq "ala reparada en estación" 100.0 "$wing_after"

validate_shot "$OUT/11_wing_lost.png"
validate_shot "$OUT/12_station_repair.png"
check_no_crash
echo "OK 09_wing_damage: ala derecha perdida y reparada en la estación"
