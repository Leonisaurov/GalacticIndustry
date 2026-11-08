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

check_no_crash
echo "OK 15_agents: 8 mineros iniciales, máximo 24 y sin render"
