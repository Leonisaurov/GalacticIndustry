#!/usr/bin/env bash
# Test: jerarquía del universo (galaxias, sistemas y sectores)
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de universo"
sleep 0.5

status=$(latest_status)
assert_eq "galaxias" 3 "$(printf '%s' "$status" | json_get galaxyCount)"
assert_eq "sistemas" 3 "$(printf '%s' "$status" | json_get systemCount)"
assert_eq "sectores" 6 "$(printf '%s' "$status" | json_get sectorCount)"
assert_eq "galaxia de arranque" milky-way "$(printf '%s' "$status" | json_get galaxyId)"
assert_eq "sector de arranque" sol-prime "$(printf '%s' "$status" | json_get sectorId)"
assert_eq "ancho del sector" 12000 "$(printf '%s' "$status" | json_get sectorWidth)"
assert_eq "alto del sector" 12000 "$(printf '%s' "$status" | json_get sectorHeight)"
assert_eq "perfil del sector" INICIAL "$(printf '%s' "$status" | json_get sectorProfile)"

check_no_crash
echo "OK 16_universe: 3 galaxias, 3 sistemas, 6 sectores y IDs estables"
