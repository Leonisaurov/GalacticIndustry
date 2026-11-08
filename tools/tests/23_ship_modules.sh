#!/usr/bin/env bash
# Test: compra, instalación, retirada y persistencia de módulos de nave
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module

launch_app "--ez test_mode true --ez station_service_test true"
wait_current_state DOCKED 10 || fail "no arrancó la estación de servicios"
sleep 0.5

assert_eq "sin módulos al empezar" 0 "$(latest_status | json_get shipModules)"
assert_eq "créditos iniciales" 200 "$(latest_status | json_get coins)"

# Pestaña MODULOS e instalación del piloto automático (150 CR).
tap "$ST_TAB_MOD_X" "$ST_TAB_Y" 0.4
tap "$((W / 2))" "$ST_MOD1_Y" 0.4
wait_status_field shipModules 1 5 || fail "AUTOPILOTO no se instaló"
status=$(latest_status)
assert_eq "AUTOPILOTO instalado" 1 "$(printf '%s' "$status" | json_get shipModules)"
assert_eq "créditos tras comprar AUTOPILOTO" 50 "$(printf '%s' "$status" | json_get coins)"

# Sin saldo suficiente no se instala el radar (140 CR).
tap "$((W / 2))" "$ST_MOD3_Y" 0.3
sleep 0.4
assert_eq "sin créditos no instala RADAR" 1 "$(latest_status | json_get shipModules)"

# Persistencia: el módulo sigue instalado tras reiniciar sin borrar datos.
launch_app "--ez test_mode true --ez station_service_test true"
wait_current_state DOCKED 10 || fail "no reabrió la estación para persistencia"
sleep 0.5
assert_eq "AUTOPILOTO persiste" 1 "$(latest_status | json_get shipModules)"

# Retirada sin reembolso.
tap "$ST_TAB_MOD_X" "$ST_TAB_Y" 0.4
tap "$((W / 2))" "$ST_MOD1_Y" 0.4
wait_status_field shipModules 0 5 || fail "AUTOPILOTO no se retiró"
status=$(latest_status)
assert_eq "sin módulos tras retirar" 0 "$(printf '%s' "$status" | json_get shipModules)"
assert_eq "retirar no reembolsa" 200 "$(printf '%s' "$status" | json_get coins)"

# Compra de otro módulo: RADAR (140 CR) en la primera ranura libre.
tap "$((W / 2))" "$ST_MOD3_Y" 0.4
wait_status_field shipModules 1 5 || fail "RADAR no se instaló"
status=$(latest_status)
assert_eq "RADAR instalado" 1 "$(printf '%s' "$status" | json_get shipModules)"
assert_eq "créditos tras comprar RADAR" 60 "$(printf '%s' "$status" | json_get coins)"

check_no_crash
echo "OK 23_ship_modules: compra, instalación, retirada y persistencia"
