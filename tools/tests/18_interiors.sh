#!/usr/bin/env bash
# Test: marco de interiores derivado de la estación personal
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/36_interior.png" "$OUT/37_interaction.png" "$OUT/39_exit.png"

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
# T-1005: salir a la nave desde la salida.
tap "$((W / 2))" "$((H / 2))" 0.5
wait_current_state PLAYING 5 || fail "la salida no devolvió a la nave"
sleep 0.4
status=$(latest_status)
assert_eq "sin acople tras salir" false "$(printf '%s' "$status" | json_get docked)"
shot "$OUT" 39_exit
validate_shot "$OUT/39_exit.png"

# T-1004: interacción por proximidad con un módulo.
launch_app "--ez test_mode true --ez interior_mgmt_test true"
wait_current_state INTERIOR 10 || fail "no abrió el interior junto al módulo"
sleep 0.6
status=$(latest_status)
assert_eq "avatar junto al almacén" module-0 "$(printf '%s' "$status" | json_get avatarRoom)"
assert_eq "gestión cerrada" false "$(printf '%s' "$status" | json_get interiorManagement)"
tap "$((W / 2))" "$((H / 2))" 0.5
wait_status_field interiorManagement true 5 || fail "no abrió la gestión desde el interior"
sleep 0.4
shot "$OUT" 37_interaction
validate_shot "$OUT/37_interaction.png"
tap "$((W / 2))" "$MG_UNDOCK_Y" 0.5
wait_status_field interiorManagement false 5 || fail "no volvió al interior"
sleep 0.3
status=$(latest_status)
assert_eq "sigue en el interior" INTERIOR "$(printf '%s' "$status" | json_get state)"

check_no_crash
echo "OK 18_interiors: marco, render, cámara e interacción con módulos"
