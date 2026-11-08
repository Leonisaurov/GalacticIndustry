#!/usr/bin/env bash
# Test: bodega, masa y HUD de carga
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/21_cargo_hud.png"

launch_app "--ez test_mode true --ez cargo_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de carga"
sleep 0.6

status=$(latest_status)
assert_eq "estado en partida" PLAYING "$(printf '%s' "$status" | json_get state)"
assert_eq "masa de carga" 100.0 "$(printf '%s' "$status" | json_get cargoMass)"
assert_eq "capacidad de bodega" 100.0 "$(printf '%s' "$status" | json_get cargoCapacity)"
assert_eq "unidades de carga" 100 "$(printf '%s' "$status" | json_get cargoUnits)"
shot "$OUT" 21_cargo_hud
validate_shot "$OUT/21_cargo_hud.png"

tap "$PAUSE_X" "$PAUSE_Y" 0.2
wait_current_state PAUSED 5 || fail "no se pausó"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.2
wait_current_state PLAYING 5 || fail "no se reanudó"
sleep 0.5
status=$(latest_status)
assert_eq "la carga persiste al pausar" 100.0 "$(printf '%s' "$status" | json_get cargoMass)"

check_no_crash
echo "OK 11_cargo: bodega 100/100 UM, HUD y persistencia al pausar"
