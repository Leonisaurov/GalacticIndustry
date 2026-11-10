#!/usr/bin/env bash
# Runner de tests de tools/tests/.
#
# Uso:
#   tools/run-tests.sh --list
#   tools/run-tests.sh 03_controls
#   tools/run-tests.sh 01_logic 04_autopilot
#   tools/run-tests.sh 06 07 08              # por prefijo
#   tools/run-tests.sh --all                 # todo, solo si lo pides
#   tools/run-tests.sh --tools               # pruebas de herramientas, sin dispositivo ni build
#   tools/run-tests.sh --no-build 04_autopilot
#
# Sin argumentos solo muestra la ayuda y la lista; no ejecuta nada.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
# shellcheck source=tools/tests/testlib.sh
source "$ROOT/tools/tests/testlib.sh"

TESTS_DIR="$ROOT/tools/tests"
DO_BUILD=1
RUN_ALL=0
RUN_TOOLS=0
SELECTED=()

usage() {
    cat <<'EOF'
Uso: tools/run-tests.sh [--list] [--all] [--no-build] [test ...]

  --list       muestra los tests disponibles
  --all        ejecuta todos los tests (solo si lo pides)
  --tools      ejecuta las pruebas de continuidad y del panel, sin dispositivo ni build
  --no-build   no compila; usa build/app.apk y build/classes actuales
  test ...     nombre o prefijo de test: 01, 01_logic, 04_autopilot...

Ejemplos:
  tools/run-tests.sh --list
  tools/run-tests.sh --tools
  tools/run-tests.sh 03_controls
  tools/run-tests.sh 01_logic 04_autopilot
  tools/run-tests.sh --no-build 04_autopilot
EOF
}

list_tests() {
    echo "Tests disponibles:"
    local f name desc
    for f in "$TESTS_DIR"/[0-9][0-9]_*.sh; do
        [ -e "$f" ] || continue
        name=$(basename "$f" .sh)
        desc=$(sed -n '2s/^# //p' "$f")
        printf '  %-22s %s\n' "$name" "$desc"
    done
}

for arg in "$@"; do
    case "$arg" in
        --list|-l) list_tests; exit 0 ;;
        --all|-a) RUN_ALL=1 ;;
        --tools) RUN_TOOLS=1 ;;
        --no-build) DO_BUILD=0 ;;
        -h|--help) usage; exit 0 ;;
        *) SELECTED+=("$arg") ;;
    esac
done

# Grupo de herramientas: no toca build/, no pide dispositivo Android y no
# forma parte de --all (all solo descubre [0-9][0-9]_*.sh).
if [ "$RUN_TOOLS" = 1 ]; then
    echo "===== herramientas (sin dispositivo ni build) ====="
    python3 tools/sync-dashboard.py --check
    python3 tools/tests/continuity_test.py
    node tools/tests/dashboard_versions_test.js
    python3 tools/tests/dashboard_versions_test.py
    echo
    echo "OK runner: grupo --tools superado"
    exit 0
fi

if [ "$RUN_ALL" = 0 ] && [ ${#SELECTED[@]} -eq 0 ]; then
    usage
    echo
    list_tests
    exit 0
fi

resolve_selector() {
    local sel=$1 f
    local -a matches=()
    while IFS= read -r f; do
        [ -n "$f" ] && matches+=("$f")
    done < <(find "$TESTS_DIR" -maxdepth 1 -type f -name "${sel%.sh}*.sh" | sort)
    if [ ${#matches[@]} -eq 0 ]; then
        fail "no hay ningún test que empiece por '$sel' (usa --list)"
    fi
    printf '%s\n' "${matches[@]}"
}

if [ "$RUN_ALL" = 1 ]; then
    while IFS= read -r f; do
        [ -n "$f" ] && SELECTED+=("$f")
    done < <(find "$TESTS_DIR" -maxdepth 1 -type f -name '[0-9][0-9]_*.sh' | sort)
else
    RESOLVED=()
    for sel in "${SELECTED[@]}"; do
        while IFS= read -r f; do
            [ -n "$f" ] && RESOLVED+=("$f")
        done < <(resolve_selector "$sel")
    done
    SELECTED=()
    declare -A seen=()
    for f in "${RESOLVED[@]}"; do
        if [ -z "${seen[$f]:-}" ]; then
            seen[$f]=1
            SELECTED+=("$f")
        fi
    done
fi

[ ${#SELECTED[@]} -gt 0 ] || fail "no se seleccionó ningún test"

needs_device=0
for f in "${SELECTED[@]}"; do
    case "$(basename "$f")" in
        01_logic.sh) ;;
        *) needs_device=1 ;;
    esac
done

if [ "$needs_device" = 1 ]; then
    ensure_device
    dnd_on
fi

if [ "$DO_BUILD" = 1 ]; then
    build_apk
fi

# Los módulos no vuelven a compilar: el runner ya lo hizo (o se pidió --no-build).
export SKIP_BUILD=1

if [ "$needs_device" = 1 ]; then
    install_app
    export PREPARED=1
    export DND_ACTIVE=1
fi

echo
for test in "${SELECTED[@]}"; do
    echo "===== $(basename "$test" .sh) ====="
    bash "$test"
    echo
done

echo "OK runner: ${#SELECTED[@]} test(s) superados"
