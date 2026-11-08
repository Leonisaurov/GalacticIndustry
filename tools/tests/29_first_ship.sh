#!/usr/bin/env bash
# Test: comienzo a pie, compra de la primera nave, equipamiento y salida
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/40_intro.png" "$OUT/41_dealer.png" "$OUT/42_bought.png"

launch_app "--ez test_mode true --ez starter_test true"
wait_current_state MENU 10 || fail "no se llegó al menú"
tap "$((W / 2))" "$MENU_PLAY_Y" 0.4
wait_current_state INTRO 10 || fail "no apareció la introducción"
sleep 0.4
shot "$OUT" 40_intro
validate_shot "$OUT/40_intro.png"

intro_y=$((H - (H * 26 / 100 + 26 * U)))
tap "$((W / 2))" "$intro_y" 0.5
wait_current_state INTERIOR 10 || fail "la introducción no abrió la nodriza"
sleep 0.5
status=$(latest_status)
assert_eq "interior de nodriza" MOTHERSHIP "$(printf '%s' "$status" | json_get interiorKind)"
assert_eq "sin nave al empezar" false "$(printf '%s' "$status" | json_get hasShip)"
assert_eq "presupuesto inicial" 1200 "$(printf '%s' "$status" | json_get coins)"

# El modo starter coloca al avatar en el concesionario.
tap "$((W / 2))" "$((H / 2))" 0.5
wait_current_state DEALER 5 || fail "no abrió el concesionario"
sleep 0.4
status=$(latest_status)
assert_eq "sin nave en el concesionario" false "$(printf '%s' "$status" | json_get hasShip)"
assert_eq "ofertas del mercado" 6 "$(printf '%s' "$status" | json_get dealerOffers)"
assert_eq "ofertas chatarra" 2 "$(printf '%s' "$status" | json_get marketScrap)"
assert_eq "ofertas usadas" 2 "$(printf '%s' "$status" | json_get marketUsed)"
assert_eq "ofertas nuevas" 2 "$(printf '%s' "$status" | json_get marketNew)"
shot "$OUT" 41_dealer
validate_shot "$OUT/41_dealer.png"

# T37: inspeccion con coste.
tap "$((8 * U + 20 * U + 132 * U / 2))" "$((H - (8 * U + 106 * U + 22 * U)))" 0.4
sleep 0.4
status=$(latest_status)
assert_eq "oferta inspeccionada" true "$(printf '%s' "$status" | json_get dealerInspected)"
assert_eq "coste de inspeccion" 1185 "$(printf '%s' "$status" | json_get coins)"

# T29: dos modulos extra en la misma confirmacion.
mod_x=$((8 * U + 20 * U + 104 * U + 35 * U))
mod_y=$((H - (8 * U + 52 * U + 20 * U)))
tap "$mod_x" "$mod_y" 0.3
tap "$mod_x" "$mod_y" 0.3
sleep 0.3
assert_eq "modulos extra elegidos" 2 "$(latest_status | json_get dealerExtras)"

row_h=$((H * 34 / 100 / 6))
[ "$row_h" -gt $((52 * U)) ] && row_h=$((52 * U))
dealer_row_y=$((H - (H * 49 / 100 - row_h / 2)))
tap "$((W / 2))" "$dealer_row_y" 0.4

btn_w=$((W * 30 / 100))
[ "$btn_w" -gt $((132 * U)) ] && btn_w=$((132 * U))
buy_x=$((8 * U + 20 * U + btn_w + 6 * U + btn_w / 2))
buy_y=$((H - (8 * U + 106 * U + 22 * U)))
tap "$buy_x" "$buy_y" 0.6
wait_current_state INTERIOR 6 || fail "la compra no volvió al interior"
sleep 0.5
status=$(latest_status)
assert_eq "nave comprada" true "$(printf '%s' "$status" | json_get hasShip)"
assert_ge "modulos instalados en la compra" 2 "$(printf '%s' "$status" | json_get shipModules)"
fuel_now=$(printf '%s' "$status" | json_get fuel)
python3 - "$fuel_now" <<'PY'
import sys
f=float(sys.argv[1])
if not (60.0 <= f <= 120.0):
    raise SystemExit(f"combustible inicial fuera de rango: {f}")
PY
assert_eq "NPC del interior" 8 "$(printf '%s' "$status" | json_get npcs)"
coins=$(printf '%s' "$status" | json_get coins)
assert_lt "la compra gasta presupuesto" 1200 "$coins"
shot "$OUT" 42_bought
validate_shot "$OUT/42_bought.png"

# El modo starter coloca al avatar junto a la nave abordable.
tap "$((W / 2))" "$((H / 2))" 0.6
wait_current_state PLAYING 8 || fail "ABORDAR NAVE no salió al espacio"
sleep 0.5
status=$(latest_status)
assert_eq "nave propia" true "$(printf '%s' "$status" | json_get hasShip)"
assert_eq "sin acople al salir" false "$(printf '%s' "$status" | json_get docked)"

check_no_crash
echo "OK 29_first_ship: intro, compra, configuración y salida al espacio"
