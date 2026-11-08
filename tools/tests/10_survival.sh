#!/usr/bin/env bash
# Test: HUD de supervivencia (O2, salud y fuga) y alertas
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/20_survival_hud.png"

launch_app "--ez test_mode true --ez survival_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de supervivencia"
sleep 0.8

status=$(latest_status)
assert_eq "estado en partida" PLAYING "$(printf '%s' "$status" | json_get state)"
assert_lt "O2 dentro del aviso" 25.1 "$(printf '%s' "$status" | json_get pilotOxygen)"
assert_ge "O2 restante" 20 "$(printf '%s' "$status" | json_get pilotOxygen)"
assert_eq "alerta de O2 bajo" 1 "$(printf '%s' "$status" | json_get o2Alert)"
assert_ge "salud separada y estable" 65 "$(printf '%s' "$status" | json_get pilotHealth)"
assert_ge "fuga activa" 4 "$(printf '%s' "$status" | json_get pilotLeak)"
assert_eq "brecha visible" true "$(printf '%s' "$status" | json_get pilotBreach)"

shot "$OUT" 20_survival_hud
validate_shot "$OUT/20_survival_hud.png"
check_no_crash
echo "OK 10_survival: O2/HP visibles, aviso de O2 y brecha"
