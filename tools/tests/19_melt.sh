#!/usr/bin/env bash
# Test: fundición temporal de mineral a créditos
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez cargo_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de carga"
sleep 0.5

tap "$PAUSE_X" "$PAUSE_Y" 0.3
wait_current_state PAUSED 5 || fail "no se pausó"
tap "$((W / 2))" "$PAUSE_SHOP_Y" 0.3
wait_current_state SHOP 5 || fail "no se abrió la tienda"
tap "$TAB_MELT_X" "$TAB_MELT_Y" 0.4
sleep 0.4

status=$(latest_status)
assert_eq "carga antes de fundir" 100 "$(printf '%s' "$status" | json_get cargoUnits)"
tap "$SHOP_FIRST_BTN_X" "$SHOP_FIRST_BTN_Y" 0.4
sleep 0.5
status=$(latest_status)
assert_eq "bodega vacía tras fundir" 0 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "masa vacía tras fundir" 0.0 "$(printf '%s' "$status" | json_get cargoMass)"
assert_ge "créditos por 100 Silicato" 200 "$(printf '%s' "$status" | json_get coins)"

check_no_crash
echo "OK 19_melt: 100 Silicato -> 200 créditos y bodega vacía"
