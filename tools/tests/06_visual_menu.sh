#!/usr/bin/env bash
# Test: capturas de diseño del menú y la tienda
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/01_menu.png" "$OUT/02_shop.png"

launch_app "--ez test_mode true"
wait_current_state MENU 10 || fail "no se llegó al menú"
sleep 0.8
shot "$OUT" 01_menu
tap "$((W / 2))" "$MENU_SHOP_Y" 0.2
wait_current_state SHOP 5 || fail "no se abrió la tienda"
sleep 0.6
shot "$OUT" 02_shop

validate_shot "$OUT/01_menu.png"
validate_shot "$OUT/02_shop.png"
check_no_crash
echo "OK 06_visual_menu: capturas 01_menu y 02_shop"
