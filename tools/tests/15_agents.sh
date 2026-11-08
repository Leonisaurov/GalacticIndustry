#!/usr/bin/env bash
# Test: plantilla inicial de pilotos autónomos
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de agentes"
sleep 0.5

status=$(latest_status)
assert_eq "agentes iniciales" 8 "$(printf '%s' "$status" | json_get agents)"
assert_eq "máximo de agentes" 24 "$(printf '%s' "$status" | json_get agentsMax)"
assert_eq "agentes acoplados" 8 "$(printf '%s' "$status" | json_get agentsDocked)"
assert_eq "estado observable del agente 0" DOCKED "$(printf '%s' "$status" | json_get agent0State)"
wait_status_field agent0State TRAVEL 5 || fail "el agente no despertó a los 2 s"
wait_status_field agent0State MINE 6 || fail "el agente no llegó a minar"

check_no_crash
echo "OK 15_agents: 8 mineros, tick cada 2 s y estados observables sin render"
