#!/usr/bin/env bash
# Test: mapa de sectores, descubrimientos y viaje warp desde el mapa
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/32_map.png"

launch_app "--ez test_mode true --ez warp_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de mapa"
sleep 0.5

tap "$MAP_BTN_X" "$MAP_BTN_Y" 0.4
wait_current_state MAP 5 || fail "MAPA no abrió el mapa"
sleep 0.4
status=$(latest_status)
assert_eq "sectores descubiertos" 5 "$(printf '%s' "$status" | json_get mapDiscovered)"
assert_eq "sectores ocultos" 1 "$(printf '%s' "$status" | json_get mapHidden)"
assert_eq "destino seleccionado" sol-belt "$(printf '%s' "$status" | json_get mapSelected)"
shot "$OUT" 32_map
validate_shot "$OUT/32_map.png"

tap "$MAP_TRAVEL_X" "$MAP_TRAVEL_Y" 0.5
wait_current_state PLAYING 5 || fail "VIAJAR no cerró el mapa"
wait_status_field sectorId sol-belt 6 || fail "el mapa no viajó a sol-belt"
sleep 0.4
status=$(latest_status)
assert_ge "el origen queda guardado" 1 "$(printf '%s' "$status" | json_get snapshotSectors)"

check_no_crash
echo "OK 18_map: mapa, descubrimientos, coste y viaje warp"
