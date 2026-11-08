#!/usr/bin/env bash
# Test: geometria de atraque, colision, campo de contencion y acople manual
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module

# T02: boca, punto de espera, corredor y guias.
launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de estación"
sleep 0.6
status=$(latest_status)
assert_eq "boca X" 0.0 "$(printf '%s' "$status" | json_get dockPointX)"
assert_eq "boca Y" 296.0 "$(printf '%s' "$status" | json_get dockPointY)"
assert_eq "espera X" 0.0 "$(printf '%s' "$status" | json_get approachX)"
assert_eq "espera Y" 60.0 "$(printf '%s' "$status" | json_get approachY)"
assert_eq "en corredor" true "$(printf '%s' "$status" | json_get dockCorridor)"
assert_eq "desviacion lateral" 0.0 "$(printf '%s' "$status" | json_get dockLateral)"
assert_eq "error de rumbo" 0.0 "$(printf '%s' "$status" | json_get dockAngleError)"
assert_eq "sin bidones en zona segura" 0 "$(printf '%s' "$status" | json_get fuelInSafeZone)"
assert_eq "energia inicial" 1000.0 "$(printf '%s' "$status" | json_get stationEnergy)"

# T03: impacto contra el casco fuera del corredor.
reset_app
launch_app "--ez test_mode true --ez collision_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de colisión"
sleep 1.2
status=$(latest_status)
assert_lt "el casco recibe daño" 100 "$(printf '%s' "$status" | json_get hull)"
max_hull=$(printf '%s' "$status" | json_get maxHull)
assert_ge "la nave queda fuera del casco" 215 "$(printf '%s' "$status" | json_get stationDistance)"
assert_eq "fuera del corredor" false "$(printf '%s' "$status" | json_get dockCorridor)"

# T04/T05: campo de contención, energía y expulsión.
reset_app
launch_app "--ez test_mode true --ez field_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de contención"
sleep 1.2
status=$(latest_status)
assert_ge "impactos del campo" 1 "$(printf '%s' "$status" | json_get containmentImpacts)"
assert_eq "sin asteroides dentro del radio seguro" 0 "$(printf '%s' "$status" | json_get stationAsteroids)"
assert_lt "el campo consume energía" 1000 "$(printf '%s' "$status" | json_get stationEnergy)"

# T06: acople manual sin módulo, guiado por la boca.
reset_app
launch_app "--ez test_mode true --ez manual_dock_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de acople manual"
sleep 0.5
status=$(latest_status)
assert_eq "acople disponible" true "$(printf '%s' "$status" | json_get canDock)"
assert_eq "distancia a la boca" 0.0 "$(printf '%s' "$status" | json_get dockDistanceToMouth)"
assert_eq "rumbo alineado" 0.0 "$(printf '%s' "$status" | json_get dockAngleError)"
tap "$DOCK_X" "$DOCK_Y" 0.4
wait_current_state DOCKED 5 || fail "ACOPLAR manual no abrió la estación"
sleep 0.3
status=$(latest_status)
assert_eq "acople manual completado" true "$(printf '%s' "$status" | json_get docked)"

check_no_crash
echo "OK 30_docking_physical: geometría, colisión, contención y acople manual"
