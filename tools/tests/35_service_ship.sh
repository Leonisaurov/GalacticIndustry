#!/usr/bin/env bash
# Test: nave de servicio y deuda sin nave ni saldo
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez broke_test true"
wait_current_state DEALER 10 || fail "no arrancó el concesionario sin saldo"
sleep 0.4
status=$(latest_status)
assert_eq "sin saldo" 0 "$(printf '%s' "$status" | json_get coins)"
assert_eq "sin nave" false "$(printf '%s' "$status" | json_get hasShip)"

# Boton de nave de servicio.
tap "$((8 * U + 20 * U + 190 * U))" "$((H - (8 * U + 6 * U + 20 * U)))" 0.5
sleep 0.5
status=$(latest_status)
assert_eq "nave de servicio entregada" true "$(printf '%s' "$status" | json_get hasShip)"
assert_eq "deuda registrada" 300 "$(printf '%s' "$status" | json_get debt)"
check_no_crash
echo "OK 35_service_ship: nave de servicio y deuda"
