#!/usr/bin/env bash
# Test: brujula basica/avanzada, sensores y radar circular
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/43_radar.png"

# T08: brujula basica con la nodriza delante.
launch_app "--ez test_mode true --ez nav_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de navegación"
sleep 0.6
status=$(latest_status)
assert_eq "brujula apunta a la nodriza" NODRIZA "$(printf '%s' "$status" | json_get compassLabel)"
assert_eq "nodriza delante" false "$(printf '%s' "$status" | json_get compassBehind)"
assert_eq "rumbo relativo" 0.0 "$(printf '%s' "$status" | json_get compassAngle)"
assert_ge "distancia a la nodriza" 2000 "$(printf '%s' "$status" | json_get compassDistance)"

# T43: radar circular con módulo instalado.
nav_w=$((W * 20 / 100))
[ "$nav_w" -gt $((100 * U)) ] && nav_w=$((100 * U))
radar_x=$((W / 2 + 2 * U + nav_w / 2))
radar_y=$((H - (16 * U + 22 * U)))
tap "$radar_x" "$radar_y" 0.5
wait_current_state RADAR 5 || fail "RADAR no abrió el radar"
sleep 0.5
shot "$OUT" 43_radar
validate_shot "$OUT/43_radar.png"

# Contacto conocido: asteroide al este, dentro del alcance.
radius=$((W * 40 / 100))
[ "$radius" -gt $((H * 30 / 100)) ] && radius=$((H * 30 / 100))
contact_x=$((W / 2 + 400 * radius / 1870))
contact_y=$((H - H * 54 / 100))
tap "$contact_x" "$contact_y" 0.5
sleep 0.5
status=$(latest_status)
nav_label=$(printf '%s' "$status" | json_get navTargetLabel)
case "$nav_label" in
    *ASTEROIDE*) ;;
    *) fail "el radar no fijó el asteroide: $nav_label" ;;
esac

# Volver y comprobar brújula avanzada con el objetivo fijado.
tap "$((W / 2))" "$((H - 53 * U))" 0.4
wait_current_state PLAYING 5 || fail "VOLVER no cerró el radar"
sleep 0.4
status=$(latest_status)
assert_eq "modulo de navegacion" true "$(printf '%s' "$status" | json_get hasNavigation)"
assert_ge "objetivo fijado" 0 "$(printf '%s' "$status" | json_get navTargetDistance)"

# T08: caso detras y brujula sin radar.
reset_app
launch_app "--ez test_mode true --ez compass_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de brújula trasera"
sleep 0.6
status=$(latest_status)
assert_eq "nodriza detras" true "$(printf '%s' "$status" | json_get compassBehind)"
assert_eq "brujula basica sin radar" NODRIZA "$(printf '%s' "$status" | json_get compassLabel)"

check_no_crash
echo "OK 31_compass_radar: brújula básica/avanzada, sensores y radar"
