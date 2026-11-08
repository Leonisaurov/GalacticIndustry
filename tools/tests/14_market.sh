#!/usr/bin/env bash
# Test: mercado de la estación (stock, objetivo y precios)
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de mercado"
sleep 0.5

status=$(latest_status)
assert_eq "stock inicial de hierro" 120 "$(printf '%s' "$status" | json_get marketIronStock)"
assert_eq "objetivo de hierro" 150 "$(printf '%s' "$status" | json_get marketIronTarget)"
assert_eq "stock inicial de oro" 32 "$(printf '%s' "$status" | json_get marketOroStock)"
assert_eq "objetivo de oro" 40 "$(printf '%s' "$status" | json_get marketOroTarget)"
assert_lt "la venta es menor que la compra" \
    "$(printf '%s' "$status" | json_get marketIronBuy)" \
    "$(printf '%s' "$status" | json_get marketIronSell)"
assert_ge "la compra respeta el precio base" 3.0 \
    "$(printf '%s' "$status" | json_get marketIronBuy)"
assert_ge "la venta respeta el precio base" 2.7 \
    "$(printf '%s' "$status" | json_get marketIronSell)"

check_no_crash
echo "OK 14_market: stock, objetivo y spread de hierro/oro en la estación"
