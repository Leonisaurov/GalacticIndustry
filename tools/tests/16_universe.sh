#!/usr/bin/env bash
# Test: jerarquía del universo y límites finitos del sector
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/31_bounds.png"

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
assert_eq "mitad del ancho" 6000 "$(printf '%s' "$status" | json_get sectorHalfWidth)"
assert_eq "mitad del alto" 6000 "$(printf '%s' "$status" | json_get sectorHalfHeight)"
assert_eq "perfil del sector" INICIAL "$(printf '%s' "$status" | json_get sectorProfile)"
assert_eq "sectores cargados" 2 "$(printf '%s' "$status" | json_get loadedSectors)"
assert_eq "sectores descargados" 4 "$(printf '%s' "$status" | json_get unloadedSectors)"
assert_eq "mapa X" 0.0 "$(printf '%s' "$status" | json_get sectorMapX)"
assert_eq "mapa Y" 0.0 "$(printf '%s' "$status" | json_get sectorMapY)"

# T-802: límites finitos y borde visible.
launch_app "--ez test_mode true --ez bounds_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de límites"
sleep 0.6
status=$(latest_status)
assert_eq "borde superior" 5860 "$(printf '%s' "$status" | json_get y | cut -d. -f1)"
assert_eq "mitad del alto en runtime" 6000 "$(printf '%s' "$status" | json_get sectorHalfHeight)"
shot "$OUT" 31_bounds
validate_shot "$OUT/31_bounds.png"
sleep 1
status=$(latest_status)
y_now=$(printf '%s' "$status" | json_get y)
assert_ge "la nave no sale por abajo" -6000 "$y_now"
assert_lt "la nave no sale por arriba" 6001 "$y_now"

check_no_crash
echo "OK 16_universe: 3 galaxias, 6 sectores, borde finito y streaming 2/4"
