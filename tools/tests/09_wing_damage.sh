#!/usr/bin/env bash
# Test: ala perdida visualmente y pestaña de reparaciones
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/11_wing_lost.png" "$OUT/12_shop_repair.png"

adb logcat -c
LOG_MARK=0
launch_app "--ez wing_test true"
wait_current_state PLAYING 8 || fail "no arrancó la prueba de alas"
sleep 0.6

status=$(latest_status)
wing=$(printf '%s' "$status" | python3 -c \
    'import json,sys; print(json.load(sys.stdin)["parts"][6])')
assert_eq "ala derecha destruida" 0.0 "$wing"
shot "$OUT" 11_wing_lost

tap "$PAUSE_X" "$PAUSE_Y" 0.4
wait_current_state PAUSED 5 || fail "no se pausó"
tap "$((W / 2))" "$PAUSE_SHOP_Y" 0.5
wait_current_state SHOP 5 || fail "no se abrió la tienda"
tap "$TAB_REPAIR_X" "$TAB_REPAIR_Y" 0.5
sleep 0.4
shot "$OUT" 12_shop_repair

validate_shot "$OUT/11_wing_lost.png"
validate_shot "$OUT/12_shop_repair.png"
check_no_crash
echo "OK 09_wing_damage: ala derecha perdida + pestaña REPARAR"
