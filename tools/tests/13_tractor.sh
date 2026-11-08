#!/usr/bin/env bash
# Test: rayo tractor, energía y captura de fragmentos
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/24_tractor.png"

launch_app "--ez test_mode true --ez tractor_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba del tractor"
wait_status_field tractorActive true 5 || fail "el tractor no se activó"

sleep 0.5
status=$(latest_status)
assert_ge "fragmentos atraídos" 1 "$(printf '%s' "$status" | json_get tractorTargets)"
assert_lt "energía consumida por el tractor" \
    "$(printf '%s' "$status" | json_get energyMax)" \
    "$(printf '%s' "$status" | json_get energy)"
assert_eq "tractor sin mejora" 0 "$(printf '%s' "$status" | json_get tractorLevel)"
shot "$OUT" 24_tractor

wait_status_field cargoUnits 12 20 || fail "el tractor no capturó los fragmentos"
sleep 0.3
status=$(latest_status)
assert_eq "fragmentos capturados" 12 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "sin fragmentos restantes" 0 "$(printf '%s' "$status" | json_get fragments)"
assert_eq "el tractor se apaga sin objetivos" false "$(printf '%s' "$status" | json_get tractorActive)"
assert_eq "capturar no da monedas" 0 "$(printf '%s' "$status" | json_get coins)"
validate_shot "$OUT/24_tractor.png"

check_no_crash
echo "OK 13_tractor: atrae 3 fragmentos, consume energía y los mete en bodega"
