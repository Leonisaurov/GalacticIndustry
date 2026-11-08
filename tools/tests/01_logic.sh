#!/usr/bin/env bash
# Test: lógica del juego sin dispositivo (SelfTest)
set -euo pipefail
source "$(dirname "$0")/testlib.sh"

if [ "${SKIP_BUILD:-0}" != 1 ]; then
    build_apk
fi
run_selftest build/test-logs/selftest.txt
echo "OK 01_logic: SelfTest TODO OK"
