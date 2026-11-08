#!/usr/bin/env bash
# Test: minería automática MINE, bodega y sin misiles
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/22_mining.png"

launch_app "--ez test_mode true --ez mining_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de minería"
wait_status_field pilot MINE 5 || fail "el piloto no entró en MINE"
wait_status_field cargoUnits 15 15 || fail "la bodega no se llenó"
wait_status_field pilot NONE 15 || fail "el piloto no terminó tras agotar la roca"
sleep 0.4

status=$(latest_status)
assert_eq "unidades minadas" 15 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_ge "masa minada" 14 "$(printf '%s' "$status" | json_get cargoMass)"
assert_eq "sin misiles en MINE" 0 "$(printf '%s' "$status" | json_get missiles)"
assert_eq "minar no da monedas" 0 "$(printf '%s' "$status" | json_get coins)"
shot "$OUT" 22_mining
validate_shot "$OUT/22_mining.png"

check_no_crash
echo "OK 12_mining: MINE aproxima, mina 15 UM sin misiles y termina con la roca agotada"
