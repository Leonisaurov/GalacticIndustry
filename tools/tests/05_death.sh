#!/usr/bin/env bash
# Test: explosión final, diálogo de fin de partida y reintento
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
banked_before=$(latest_status | json_get banked)
[ -n "$banked_before" ] || banked_before=0
adb logcat -c
LOG_MARK=0
launch_app "--ez debug_death true"
wait_current_state DYING 6 || fail "no empezó la secuencia de muerte"
wait_current_state OVER 10 || fail "no se llegó al diálogo de fin de partida"
sleep 0.5
status=$(latest_status)
assert_eq "estado final" OVER "$(printf '%s' "$status" | json_get state)"
assert_eq "monedas de la partida" 7 "$(printf '%s' "$status" | json_get overCoins)"
assert_eq "casco destruido" 0.0 "$(printf '%s' "$status" | json_get hull)"
assert_eq "monedas guardadas" "$((banked_before + 7))" "$(printf '%s' "$status" | json_get banked)"

tap "$((W / 2))" "$OVER_RETRY_Y" 0.2
wait_current_state PLAYING 5 || fail "reintentar no inició una partida"
sleep 0.5
status=$(latest_status)
assert_eq "reintento en partida" PLAYING "$(printf '%s' "$status" | json_get state)"
assert_eq "reintento con casco lleno" "$(printf '%s' "$status" | json_get maxHull)" \
    "$(printf '%s' "$status" | json_get hull)"

check_no_crash
echo "OK 05_death: DYING -> OVER (+7 monedas, casco 0) -> REINTENTAR PLAYING"
