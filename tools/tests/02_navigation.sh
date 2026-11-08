#!/usr/bin/env bash
# Test: menú, partida, pausa y vuelta al menú
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
launch_app "--ez test_mode true"
wait_current_state MENU 10 || fail "no se llegó al menú"
sleep 0.5
status=$(latest_status)
assert_eq "estado inicial" MENU "$(printf '%s' "$status" | json_get state)"
assert_eq "modo inmortal" true "$(printf '%s' "$status" | json_get immortal)"

tap "$((W / 2))" "$MENU_PLAY_Y" 0.2
wait_current_state PLAYING 5 || fail "no empezó la partida"
tap "$PAUSE_X" "$PAUSE_Y" 0.2
wait_current_state PAUSED 5 || fail "no se pausó"
tap "$((W / 2))" "$PAUSE_MENU_Y" 0.2
wait_current_state MENU 5 || fail "no se volvió al menú"
tap "$((W / 2))" "$MENU_PLAY_Y" 0.2
wait_current_state PLAYING 5 || fail "no se reanudó la partida"

check_no_crash
echo "OK 02_navigation: MENU -> PLAYING -> PAUSED -> MENU -> PLAYING"
