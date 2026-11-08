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

# Muerte por piloto (0 HP): no debe quedarse vivo ni reiniciar el universo.
reset_app
launch_app "--ez pilot_death_test true"
wait_current_state DYING 6 || fail "no empezó la muerte por piloto"
wait_current_state OVER 10 || fail "no se llegó al fin por piloto"
sleep 0.5
status=$(latest_status)
assert_eq "muerte por piloto" DEAD "$(printf '%s' "$status" | json_get pilotStatus)"
assert_eq "la muerte por piloto no rompe el casco" \
    "$(printf '%s' "$status" | json_get maxHull)" \
    "$(printf '%s' "$status" | json_get hull)"
assert_eq "la nave legado queda marcada" true "$(printf '%s' "$status" | json_get legacyWreck)"
assert_eq "legado en la posición de muerte X" 0.0 "$(printf '%s' "$status" | json_get legacyX)"
assert_eq "legado en la posición de muerte Y" 0.0 "$(printf '%s' "$status" | json_get legacyY)"

tap "$((W / 2))" "$OVER_RETRY_Y" 0.2
wait_current_state PLAYING 5 || fail "nuevo personaje no inició partida"
sleep 0.5
status=$(latest_status)
assert_eq "nuevo personaje en partida" PLAYING "$(printf '%s' "$status" | json_get state)"
assert_eq "nuevo personaje con casco lleno" \
    "$(printf '%s' "$status" | json_get maxHull)" \
    "$(printf '%s' "$status" | json_get hull)"
assert_eq "el legado sigue en el mundo" true "$(printf '%s' "$status" | json_get legacyWreck)"

check_no_crash
echo "OK 05_death: casco DYING->OVER, piloto 0 HP->OVER sin romper casco, nuevo personaje con legado"
