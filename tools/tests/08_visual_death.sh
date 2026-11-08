#!/usr/bin/env bash
# Test: capturas de diseño de la explosión final y el diálogo
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

prepare_device_module
ensure_magick
OUT=${OUT:-build/screens}
mkdir -p "$OUT"
rm -f "$OUT/07_death_a.png" "$OUT/08_death_b.png" "$OUT/09_death_c.png" "$OUT/10_gameover.png"

adb logcat -c
LOG_MARK=0
launch_app "--ez debug_death true --ez slow_death true"
wait_current_state DYING 6 || fail "no empezó la secuencia de muerte"
sleep 0.2
shot "$OUT" 07_death_a
sleep 0.6
shot "$OUT" 08_death_b
sleep 0.8
shot "$OUT" 09_death_c
wait_current_state OVER 15 || fail "no se llegó al diálogo de fin de partida"
sleep 0.4
shot "$OUT" 10_gameover

for pair in "07_death_a 08_death_b" "08_death_b 09_death_c"; do
    set -- $pair
    [ "$(md5sum "$OUT/$1.png" | cut -d' ' -f1)" != \
      "$(md5sum "$OUT/$2.png" | cut -d' ' -f1)" ] \
        || fail "la explosión no avanza entre $1 y $2"
done

validate_shot "$OUT/07_death_a.png"
validate_shot "$OUT/08_death_b.png"
validate_shot "$OUT/09_death_c.png"
validate_shot "$OUT/10_gameover.png"
check_no_crash
echo "OK 08_visual_death: capturas 07_death_a, 08_death_b, 09_death_c y 10_gameover"
