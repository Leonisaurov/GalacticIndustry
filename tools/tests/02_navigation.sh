#!/usr/bin/env bash
# Test: menú y tienda (sin partida)
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true"
wait_current_state MENU 10 || fail "no se llegó al menú"
sleep 0.5
status=$(latest_status)
assert_eq "estado inicial" MENU "$(printf '%s' "$status" | json_get state)"
assert_eq "modo inmortal" true "$(printf '%s' "$status" | json_get immortal)"

tap "$((W / 2))" "$MENU_SHOP_Y" 0.2
wait_current_state SHOP 5 || fail "no se abrió la tienda"
sleep 0.5
status=$(latest_status)
assert_eq "origen de la tienda" MENU "$(printf '%s' "$status" | json_get shopReturn)"

tap "$BACK_X" "$BACK_Y" 0.2
wait_current_state MENU 5 || fail "no se volvió al menú"

check_no_crash
echo "OK 02_navigation: MENU -> SHOP -> MENU"
