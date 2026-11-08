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
# UPD-006: el módulo de atraque recorre el corredor y acopla solo.
wait_current_state DOCKED 45 || fail "el piloto no completó el atraque"
sleep 0.4
status=$(latest_status)
assert_eq "nave acoplada" true "$(printf '%s' "$status" | json_get docked)"
assert_ge "distancia a la boca" 150 "$(printf '%s' "$status" | json_get stationDistance)"
assert_lt "distancia a la boca máxima" 220 "$(printf '%s' "$status" | json_get stationDistance)"
shot "$OUT" 26_dock

sleep 0.2
shot "$OUT" 27_docked
validate_shot "$OUT/27_docked.png"

tap "$UNDOCK_X" "$UNDOCK_Y" 0.3
wait_current_state PLAYING 5 || fail "DESPEGAR no devolvió al exterior"
sleep 0.4
status=$(latest_status)
assert_eq "nave desacoplada" false "$(printf '%s' "$status" | json_get docked)"
assert_eq "velocidad cero al despegar" 0.0 "$(printf '%s' "$status" | json_get speed)"

# T-503: servicios de estación (descarga, venta, combustible, O2, reparación y mejoras).
reset_app
launch_app "--ez test_mode true --ez station_service_test true"
wait_current_state DOCKED 10 || fail "no arrancó la estación de servicios"
sleep 0.5
status=$(latest_status)
assert_eq "carga inicial" 100 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "créditos iniciales" 200 "$(printf '%s' "$status" | json_get coins)"
assert_eq "casco dañado" 50.0 "$(printf '%s' "$status" | json_get hull)"
assert_eq "combustible bajo" 40.0 "$(printf '%s' "$status" | json_get fuel)"
assert_eq "O2 bajo" 40.0 "$(printf '%s' "$status" | json_get pilotOxygen)"

tap "$((W / 2))" "$ST_ROW1_Y" 0.3
wait_status_field cargoUnits 0 5 || fail "DESCARGAR no vació la bodega"
status=$(latest_status)
assert_eq "almacén recibe la carga" 100 "$(printf '%s' "$status" | json_get stationStorageUnits)"

tap "$((W / 2))" "$ST_ROW2_Y" 0.3
wait_status_field stationStorageUnits 0 5 || fail "VENDER no vació el almacén"
status=$(latest_status)
assert_eq "venta a precio base" 400 "$(printf '%s' "$status" | json_get coins)"

tap "$((W / 2))" "$ST_ROW3_Y" 0.3
sleep 0.4
status=$(latest_status)
assert_eq "repostaje completo" 160.0 "$(printf '%s' "$status" | json_get fuel)"

tap "$((W / 2))" "$ST_ROW4_Y" 0.3
sleep 0.4
status=$(latest_status)
assert_eq "O2 completo" 100.0 "$(printf '%s' "$status" | json_get pilotOxygen)"

tap "$((W / 2))" "$ST_ROW5_Y" 0.3
sleep 0.4
status=$(latest_status)
assert_eq "casco reparado" 100.0 "$(printf '%s' "$status" | json_get hull)"

tap "$ST_TAB_UPG_X" "$ST_TAB_Y" 0.3
sleep 0.4
tap "$((W / 2))" "$ST_UPG1_Y" 0.3
sleep 0.5
status=$(latest_status)
assert_eq "mejora Motor comprada" 1 "$(printf '%s' "$status" | json_get motorLevel)"

tap "$((W / 2))" "$ST_UNDOCK_Y" 0.3
wait_current_state PLAYING 5 || fail "DESPEGAR de estación no volvió al exterior"
status=$(latest_status)
assert_eq "nave desacoplada tras servicios" false "$(printf '%s' "$status" | json_get docked)"

# T-505: persistencia de almacén y estado acoplado.
reset_app
launch_app "--ez test_mode true --ez station_service_test true"
wait_current_state DOCKED 10 || fail "no arrancó la estación para persistencia"
tap "$((W / 2))" "$ST_ROW1_Y" 0.3
wait_status_field stationStorageUnits 100 5 || fail "no se descargó antes de guardar"
launch_app "--ez test_mode true"
wait_current_state PAUSED 10 || fail "la partida guardada no cargó en pausa"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.3
wait_current_state DOCKED 5 || fail "el estado acoplado no se restauró"
sleep 0.4
status=$(latest_status)
assert_eq "acople restaurado" true "$(printf '%s' "$status" | json_get docked)"
assert_eq "almacén restaurado" 100 "$(printf '%s' "$status" | json_get stationStorageUnits)"
assert_eq "bodega sin duplicar" 0 "$(printf '%s' "$status" | json_get cargoUnits)"

tap "$((W / 2))" "$ST_UNDOCK_Y" 0.3
wait_current_state PLAYING 5 || fail "no despegó tras restaurar"
launch_app "--ez test_mode true"
wait_current_state PAUSED 10 || fail "no recargó tras despegar"
tap "$((W / 2))" "$PAUSE_CONTINUE_Y" 0.3
wait_current_state PLAYING 5 || fail "no reanudó en el exterior"
sleep 0.4
status=$(latest_status)
assert_eq "acople no persiste tras despegar" false "$(printf '%s' "$status" | json_get docked)"
assert_eq "almacén persiste tras despegar" 100 "$(printf '%s' "$status" | json_get stationStorageUnits)"

# T-506: rescate por falta de combustible.
reset_app
launch_app "--ez test_mode true --ez rescue_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de rescate"
wait_status_field canRescue true 5 || fail "el rescate no está disponible"
sleep 0.4
status=$(latest_status)
assert_eq "sin combustible" 0.0 "$(printf '%s' "$status" | json_get fuel)"
assert_eq "carga antes del rescate" 100 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "créditos antes del rescate" 200 "$(printf '%s' "$status" | json_get coins)"
shot "$OUT" 28_rescue

tap "$RESCUE_X" "$RESCUE_Y" 0.3
wait_status_field rescueActive true 5 || fail "el rescate no se activó"
sleep 0.5
status=$(latest_status)
assert_eq "el rescate no es instantáneo" true "$(printf '%s' "$status" | json_get rescueActive)"
wait_status_field rescueActive false 6 || fail "el rescate no terminó"
sleep 0.4
status=$(latest_status)
assert_eq "combustible de emergencia" 20.0 "$(printf '%s' "$status" | json_get fuel)"
assert_eq "carga perdida por el rescate" 75 "$(printf '%s' "$status" | json_get cargoUnits)"
assert_eq "coste del rescate" 100 "$(printf '%s' "$status" | json_get coins)"
assert_ge "distancia tras el remolque" 261 "$(printf '%s' "$status" | json_get stationDistance)"
assert_eq "rescate registrado en el log" 1 "$(
    adb logcat -d -s AsteroidsTelemetry:D | grep -c '"event":"rescue"'
)"

check_no_crash
echo "OK 13_station: nodriza, acople, servicios, persistencia, rescate y despegue"
