#!/usr/bin/env bash
# Test: captura de diseño del menú principal
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/01_menu.png"

launch_app "--ez test_mode true"
wait_current_state MENU 10 || fail "no se llegó al menú"
sleep 0.8
shot "$OUT" 01_menu

validate_shot "$OUT/01_menu.png"
check_no_crash
echo "OK 06_visual_menu: captura 01_menu"
