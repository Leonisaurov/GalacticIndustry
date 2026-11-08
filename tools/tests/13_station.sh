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

check_no_crash
echo "OK 13_station: nodriza a escala 200, zona segura limpia y cerca del inicio"
