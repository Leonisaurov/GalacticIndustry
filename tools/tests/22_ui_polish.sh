#!/usr/bin/env bash
# Test: pausa, audio y navegación Atrás sin solapes ni salidas accidentales
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module

launch_app "--ez test_mode true --ez station_test true"
wait_current_state PLAYING 10 || fail "no arrancó la prueba de UI"
sleep 0.5

# Pausa y toggles de audio.
tap "$PAUSE_X" "$PAUSE_Y" 0.4
wait_current_state PAUSED 5 || fail "no abrió la pausa"
tap "$((W / 2))" "$MUTE_Y" 0.4
sleep 0.3
assert_eq "sonido silenciado" true "$(latest_status | json_get audioMuted)"
tap "$((W / 2))" "$MUSIC_Y" 0.4
sleep 0.3
assert_eq "música silenciada" true "$(latest_status | json_get musicMuted)"
tap "$((W / 2))" "$MUSIC_Y" 0.4
tap "$((W / 2))" "$MUTE_Y" 0.4
sleep 0.3
assert_eq "sonido restaurado" false "$(latest_status | json_get audioMuted)"
assert_eq "música restaurada" false "$(latest_status | json_get musicMuted)"

# Atrás: pausa, reanuda y vuelve a pausar sin cerrar la app.
adb shell input keyevent KEYCODE_BACK
wait_current_state PLAYING 5 || fail "Atrás no reanudó la partida"
adb shell input keyevent KEYCODE_BACK
wait_current_state PAUSED 5 || fail "Atrás no pausó la partida"
adb shell input keyevent KEYCODE_BACK
wait_current_state PLAYING 5 || fail "Atrás no reanudó desde la pausa"

# Atrás desde el mapa vuelve a la partida.
tap "$MAP_BTN_X" "$MAP_BTN_Y" 0.4
wait_current_state MAP 5 || fail "no abrió el mapa"
adb shell input keyevent KEYCODE_BACK
wait_current_state PLAYING 5 || fail "Atrás no cerró el mapa"

# El botón MENU de la pausa sigue funcionando.
tap "$PAUSE_X" "$PAUSE_Y" 0.4
wait_current_state PAUSED 5 || fail "no reabrió la pausa"
tap "$((W / 2))" "$PAUSE_MENU_Y" 0.4
wait_current_state MENU 5 || fail "no volvió al menú"

check_no_crash
echo "OK 22_ui_polish: pausa sin solapes, audio y navegación Atrás"
