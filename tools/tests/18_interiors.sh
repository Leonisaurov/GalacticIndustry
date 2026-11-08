#!/usr/bin/env bash
# Test: marco de interiores derivado de la estación personal
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/36_interior.png"

launch_app "--ez test_mode true --ez module_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de interiores"
sleep 0.5

status=$(latest_status)
assert_eq "tipo de interior" PLAYER_STATION "$(printf '%s' "$status" | json_get interiorKind)"
assert_eq "habitaciones" 3 "$(printf '%s' "$status" | json_get interiorRooms)"
assert_eq "puertas" 2 "$(printf '%s' "$status" | json_get interiorDoors)"
assert_eq "interactuables" 3 "$(printf '%s' "$status" | json_get interiorInteractables)"

# T-1003: render del interior y cámara.
launch_app "--ez test_mode true --ez interior_test true"
wait_current_state INTERIOR 10 || fail "no abrió el interior"
sleep 0.6
status=$(latest_status)
assert_eq "avatar en el núcleo" hub "$(printf '%s' "$status" | json_get avatarRoom)"
fps=0
for _ in $(seq 1 20); do
    fps=$(latest_status | json_get fps)
    [ "$fps" -ge 25 ] && break
    sleep 0.3
done
assert_ge "fps interior" 25 "$fps"
shot "$OUT" 36_interior
validate_shot "$OUT/36_interior.png"

check_no_crash
echo "OK 18_interiors: marco 3/2/3, render del interior y cámara"
