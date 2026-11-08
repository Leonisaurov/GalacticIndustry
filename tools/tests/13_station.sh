#!/usr/bin/env bash
# Test: nave nodriza, escala, zona segura y telemetría
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/25_station.png"

launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de estación"
sleep 0.6

status=$(latest_status)
assert_eq "estación en el sector inicial X" 0.0 "$(printf '%s' "$status" | json_get stationX)"
assert_eq "estación en el sector inicial Y" 480.0 "$(printf '%s' "$status" | json_get stationY)"
assert_eq "radio visual de la nodriza" 200.0 "$(printf '%s' "$status" | json_get stationRadius)"
assert_lt "la nave arranca cerca" 500 "$(printf '%s' "$status" | json_get stationDistance)"
assert_eq "sin asteroides en la zona segura" 0 "$(printf '%s' "$status" | json_get stationAsteroids)"
shot "$OUT" 25_station
validate_shot "$OUT/25_station.png"

# T-502: Autopilot.STATION, ACOPLAR y DESPEGAR.
reset_app
launch_app "--ez test_mode true --ez docking_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de acople"
wait_status_field pilot STATION 10 || fail "el piloto no entró en STATION"
wait_status_field canDock true 45 || fail "el piloto no llegó a la zona de acople"
sleep 0.4
status=$(latest_status)
assert_ge "distancia de acople" 200 "$(printf '%s' "$status" | json_get stationDistance)"
assert_lt "distancia de acople máxima" 261 "$(printf '%s' "$status" | json_get stationDistance)"
shot "$OUT" 26_dock

tap "$DOCK_X" "$DOCK_Y" 0.3
wait_current_state DOCKED 5 || fail "ACOPLAR no abrió la estación"
sleep 0.4
status=$(latest_status)
assert_eq "nave acoplada" true "$(printf '%s' "$status" | json_get docked)"
shot "$OUT" 27_docked
validate_shot "$OUT/27_docked.png"

tap "$UNDOCK_X" "$UNDOCK_Y" 0.3
wait_current_state PLAYING 5 || fail "DESPEGAR no devolvió al exterior"
sleep 0.4
status=$(latest_status)
assert_eq "nave desacoplada" false "$(printf '%s' "$status" | json_get docked)"
assert_eq "velocidad cero al despegar" 0.0 "$(printf '%s' "$status" | json_get speed)"

check_no_crash
echo "OK 13_station: nodriza, zona segura, Autopilot.STATION, acople y despegue"
