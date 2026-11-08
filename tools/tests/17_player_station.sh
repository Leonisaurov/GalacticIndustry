#!/usr/bin/env bash
# Test: plataforma personal vacía y definiciones de módulos
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/33_base.png" "$OUT/34_modules.png"

launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de estación personal"
sleep 0.5

status=$(latest_status)
assert_eq "plataforma no colocada" false "$(printf '%s' "$status" | json_get playerStationBuilt)"
assert_eq "ranuras" 6 "$(printf '%s' "$status" | json_get playerStationSlots)"
assert_eq "módulos construidos" 0 "$(printf '%s' "$status" | json_get playerStationModules)"
assert_eq "servicios de serie" 0 "$(printf '%s' "$status" | json_get playerStationServices)"
assert_eq "definiciones de módulos" 4 "$(printf '%s' "$status" | json_get moduleDefs)"

# T-902: construir la plataforma vacía.
launch_app "--ez test_mode true --ez base_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de construcción"
sleep 0.5
status=$(latest_status)
assert_eq "se puede construir la base" true "$(printf '%s' "$status" | json_get canBuildBase)"
assert_eq "la base aún no existe" false "$(printf '%s' "$status" | json_get playerStationBuilt)"

tap "$BASE_X" "$BASE_Y" 0.5
wait_status_field playerStationBuilt true 5 || fail "la base no se construyó"
sleep 0.4
status=$(latest_status)
assert_eq "la base nace sin módulos" 0 "$(printf '%s' "$status" | json_get playerStationModules)"
assert_eq "la base nace sin servicios" 0 "$(printf '%s' "$status" | json_get playerStationServices)"
assert_eq "coste en créditos" 700 "$(printf '%s' "$status" | json_get coins)"
assert_eq "posición de la base" -1500.0 "$(printf '%s' "$status" | json_get playerStationX)"
shot "$OUT" 33_base
validate_shot "$OUT/33_base.png"

launch_app "--ez test_mode true"
wait_current_state PAUSED 10 || fail "no cargó la partida con base"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.3
wait_current_state PLAYING 5 || fail "no reanudó con la base"
sleep 0.4
status=$(latest_status)
assert_eq "la base persiste" true "$(printf '%s' "$status" | json_get playerStationBuilt)"
assert_eq "la posición persiste" -1500.0 "$(printf '%s' "$status" | json_get playerStationX)"
assert_eq "sigue sin servicios" 0 "$(printf '%s' "$status" | json_get playerStationServices)"

# T-903: módulos construidos y servicios derivados.
launch_app "--ez test_mode true --ez module_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de módulos"
sleep 0.5
status=$(latest_status)
assert_eq "módulos construidos" 2 "$(printf '%s' "$status" | json_get playerStationModules)"
assert_eq "servicios activos" 3 "$(printf '%s' "$status" | json_get playerStationServices)"
assert_eq "la base ya existe" false "$(printf '%s' "$status" | json_get canBuildBase)"
shot "$OUT" 34_modules
validate_shot "$OUT/34_modules.png"

tap "$PAUSE_X" "$PAUSE_Y" 0.3
wait_current_state PAUSED 5 || fail "no pausó para guardar los módulos"
launch_app "--ez test_mode true"
wait_current_state PAUSED 10 || fail "no cargó la partida con módulos"
sleep 0.5
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.5
wait_current_state PLAYING 5 || fail "no reanudó con módulos"
sleep 0.4
status=$(latest_status)
assert_eq "los módulos persisten" 2 "$(printf '%s' "$status" | json_get playerStationModules)"
assert_eq "los servicios persisten" 3 "$(printf '%s' "$status" | json_get playerStationServices)"

check_no_crash
echo "OK 17_player_station: vacía, construcción, persistencia y módulos"
