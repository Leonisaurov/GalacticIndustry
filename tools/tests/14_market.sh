#!/usr/bin/env bash
# Test: mercado de la estación (stock, precios y compraventa)
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/29_market.png"

launch_app "--ez test_mode true --ez market_test true"
wait_current_state MARKET 10 || fail "no se abrió el mercado"
sleep 0.5
status=$(latest_status)
assert_eq "stock inicial de hierro" 120 "$(printf '%s' "$status" | json_get marketIronStock)"
assert_eq "carga inicial" 50 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "créditos iniciales" 500 "$(printf '%s' "$status" | json_get coins)"
assert_eq "objetivo de hierro" 150 "$(printf '%s' "$status" | json_get marketIronTarget)"
assert_lt "la venta es menor que la compra" \
    "$(printf '%s' "$status" | json_get marketIronBuy)" \
    "$(printf '%s' "$status" | json_get marketIronSell)"
shot "$OUT" 29_market
validate_shot "$OUT/29_market.png"

# Vender 10.
tap "$MC_QTY_PLUS10_X" "$MC_QTY_Y" 0.2
tap "$MC_SELL_X" "$MC_ACTION_Y" 0.2
tap "$MC_SELL_X" "$MC_ACTION_Y" 0.3
sleep 0.4
status=$(latest_status)
assert_eq "venta de 10" 40 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "stock sube al vender" 130 "$(printf '%s' "$status" | json_get marketIronStock)"
assert_ge "créditos al vender" 520 "$(printf '%s' "$status" | json_get coins)"

# Vender todo.
tap "$MC_TODO_X" "$MC_TODO_Y" 0.2
tap "$MC_SELL_X" "$MC_ACTION_Y" 0.2
tap "$MC_SELL_X" "$MC_ACTION_Y" 0.3
sleep 0.4
status=$(latest_status)
assert_eq "venta total deja bodega vacía" 0 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "stock sube al vender todo" 170 "$(printf '%s' "$status" | json_get marketIronStock)"

# Nada no vende.
coins_before=$(printf '%s' "$status" | json_get coins)
tap "$MC_NADA_X" "$MC_TODO_Y" 0.2
tap "$MC_SELL_X" "$MC_ACTION_Y" 0.2
sleep 0.3
status=$(latest_status)
assert_eq "NADA no cambia créditos" "$coins_before" "$(printf '%s' "$status" | json_get coins)"

# Comprar 1.
tap "$MC_QTY_PLUS1_X" "$MC_QTY_Y" 0.2
tap "$MC_BUY_X" "$MC_ACTION_Y" 0.2
tap "$MC_BUY_X" "$MC_ACTION_Y" 0.3
sleep 0.4
status=$(latest_status)
assert_eq "compra de 1" 1 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "stock baja al comprar" 169 "$(printf '%s' "$status" | json_get marketIronStock)"
assert_lt "créditos bajan al comprar" "$coins_before" "$(printf '%s' "$status" | json_get coins)"
coins_after_buy=$(printf '%s' "$status" | json_get coins)

# Anti-arbitraje: comprar y vender la misma unidad pierde créditos.
tap "$MC_QTY_PLUS1_X" "$MC_QTY_Y" 0.2
tap "$MC_SELL_X" "$MC_ACTION_Y" 0.2
tap "$MC_SELL_X" "$MC_ACTION_Y" 0.3
sleep 0.4
status=$(latest_status)
assert_eq "venta de la unidad" 0 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "stock vuelve al vender" 170 "$(printf '%s' "$status" | json_get marketIronStock)"
coins_after_sell=$(printf '%s' "$status" | json_get coins)
assert_ge "comprar y vender no da beneficio" "$coins_after_sell" "$coins_before"

# Recomprar para comprobar persistencia.
tap "$MC_QTY_PLUS1_X" "$MC_QTY_Y" 0.2
tap "$MC_BUY_X" "$MC_ACTION_Y" 0.2
tap "$MC_BUY_X" "$MC_ACTION_Y" 0.3
sleep 0.4
status=$(latest_status)
coins_after=$(printf '%s' "$status" | json_get coins)
assert_lt "recompra cuesta créditos" "$coins_after_sell" "$coins_after"

tap "$MC_BACK_X" "$MC_BACK_Y" 0.3
wait_current_state DOCKED 5 || fail "VOLVER no regresó a la estación"

# T-605: el mercado persiste tras reiniciar.
launch_app "--ez test_mode true"
wait_current_state PAUSED 10 || fail "la partida con mercado no cargó"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.3
wait_current_state DOCKED 5 || fail "no volvió acoplado tras cargar"
sleep 0.4
status=$(latest_status)
assert_eq "stock de hierro persistido" 169 "$(printf '%s' "$status" | json_get marketIronStock)"
assert_eq "carga persistida" 1 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "créditos persistidos" "$coins_after" "$(printf '%s' "$status" | json_get coins)"

check_no_crash
echo "OK 14_market: precios, compraventa, confirmación, anti-arbitraje y persistencia"
