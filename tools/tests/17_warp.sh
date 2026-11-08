#!/usr/bin/env bash
# Test: viaje warp libre y regreso entre sectores
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez warp_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de warp"
sleep 0.5

status=$(latest_status)
assert_eq "sector inicial" sol-prime "$(printf '%s' "$status" | json_get sectorId)"
assert_eq "motor warp" 1 "$(printf '%s' "$status" | json_get warpLevel)"
fuel_before=$(printf '%s' "$status" | json_get fuel)

# Zonas invisibles de prueba: izquierda viaja, derecha vuelve.
tap "$((W * 39 / 100))" "$((H * 91 / 100))" 0.3
wait_status_field sectorId sol-belt 6 || fail "el warp no llegó a sol-belt"
sleep 0.4
status=$(latest_status)
fuel_after=$(printf '%s' "$status" | json_get fuel)
assert_lt "el warp consume combustible" "$fuel_before" "$fuel_after"
assert_eq "el origen queda guardado" 1 "$(printf '%s' "$status" | json_get snapshotSectors)"
assert_eq "solo se carga lo cercano" 2 "$(printf '%s' "$status" | json_get loadedSectors)"

tap "$((W * 61 / 100))" "$((H * 91 / 100))" 0.3
wait_status_field sectorId sol-prime 6 || fail "no regresó al sector anterior"
sleep 0.4
status=$(latest_status)
assert_eq "el regreso conserva la nave" sol-prime "$(printf '%s' "$status" | json_get sectorId)"
assert_ge "el sector visitado queda guardado" 1 "$(printf '%s' "$status" | json_get snapshotSectors)"

# El sector actual viaja en el guardado.
launch_app "--ez test_mode true"
wait_current_state PAUSED 10 || fail "no cargó la partida tras el warp"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.3
wait_current_state PLAYING 5 || fail "no reanudó tras el warp"
sleep 0.4
status=$(latest_status)
assert_eq "el sector actual persiste" sol-prime "$(printf '%s' "$status" | json_get sectorId)"
assert_ge "el snapshot persiste" 1 "$(printf '%s' "$status" | json_get snapshotSectors)"

check_no_crash
echo "OK 17_warp: viaje libre, coste de combustible y regreso con snapshot"
