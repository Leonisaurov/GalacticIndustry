#!/usr/bin/env bash
# Test: pantalla visual de equipamiento, compatibilidad y previsualizacion
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/45_equipment.png"

launch_app "--ez test_mode true --ez station_service_test true"
wait_current_state DOCKED 10 || fail "no arrancó la estación de servicios"
sleep 0.5

equip_w=$((W * 26 / 100))
[ "$equip_w" -gt $((120 * U)) ] && equip_w=$((120 * U))
tap "$((8 * U + 20 * U + equip_w / 2))" "$((H - (8 * U + 34 * U + 26 * U)))" 0.5
wait_current_state EQUIPMENT 5 || fail "VISTA NAVE no abrió el equipamiento"
sleep 0.5
shot "$OUT" 45_equipment
validate_shot "$OUT/45_equipment.png"

# Ranura 2 (PROPULSION) y modulo PROPULSION (fila 4).
tap "$((W / 2 - 204))" "$((H - (H * 62 / 100)))" 0.4
row3_y=$((H - (H * 38 / 100 - 3 * (46 * U + 5 * U) - 46 * U / 2)))
tap "$((W / 2))" "$row3_y" 0.4
sleep 0.5
status=$(latest_status)
preview=$(printf '%s' "$status" | json_get equipPreview)
case "$preview" in
    *PROPULSION*) ;;
    *) fail "la ficha no muestra PROPULSION: $preview" ;;
esac
assert_eq "previsualizacion valida" true "$(printf '%s' "$status" | json_get equipPreviewOk)"
before=$(printf '%s' "$status" | json_get equipThrust)
after=$(printf '%s' "$status" | json_get equipThrustAfter)
python3 - "$before" "$after" <<'PY'
import sys
if not float(sys.argv[2]) > float(sys.argv[1]):
    raise SystemExit("la previsualizacion no mejora el empuje")
PY

# Instalar (200 CR) y retirar sin reembolso.
tap "$((8 * U + 22 * U + 150))" "$((H - (8 * U + 16 * U + 25 * U)))" 0.5
sleep 0.4
status=$(latest_status)
assert_eq "modulo instalado" 1 "$(printf '%s' "$status" | json_get shipModules)"
assert_eq "creditos tras instalar" 0 "$(printf '%s' "$status" | json_get coins)"
tap "$((8 * U + 22 * U + 150))" "$((H - (8 * U + 16 * U + 25 * U)))" 0.5
sleep 0.4
status=$(latest_status)
assert_eq "modulo retirado" 0 "$(printf '%s' "$status" | json_get shipModules)"
assert_eq "retirar no reembolsa" 0 "$(printf '%s' "$status" | json_get coins)"

check_no_crash
echo "OK 33_equipment: ranuras, compatibilidad, previsualización e instalación"
