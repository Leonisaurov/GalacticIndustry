#!/usr/bin/env python3
"""Valida y sincroniza el panel de continuidad de GalacticIndustry.

Uso:
    python3 tools/sync-dashboard.py            # valida, sincroniza el HTML y escribe
    python3 tools/sync-dashboard.py --check    # solo valida y comprueba que el HTML coincide
    python3 tools/sync-dashboard.py --quiet    # no imprime el resumen, solo errores

Este script no es un servidor y no usa red. Es una herramienta local de build.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRACKER = ROOT / "docs" / "IMPLEMENTATION_TRACKER.json"
DASHBOARD = ROOT / "docs" / "dashboard" / "index.html"
DASHBOARD_CSS = ROOT / "docs" / "dashboard" / "dashboard.css"
DASHBOARD_JS = ROOT / "docs" / "dashboard" / "dashboard.js"
START = "<!-- TRACKER_DATA_START -->"
END = "<!-- TRACKER_DATA_END -->"

VALID_STATUS = {
    "PENDIENTE",
    "LISTO",
    "EN_PROGRESO",
    "COMPLETADO",
    "BLOQUEADO",
    "POSPUESTO",
    "REQUIERE_DECISION",
}

REQUIRED_TASK_FIELDS = [
    "id",
    "stageId",
    "title",
    "description",
    "category",
    "priority",
    "status",
    "countsForProgress",
    "dependencies",
    "decisionIds",
    "files",
    "acceptanceCriteria",
    "tests",
    "technicalNotes",
    "executionLog",
    "evidence",
    "createdAt",
    "updatedAt",
]

errors: list[str] = []
warnings: list[str] = []


def err(msg: str) -> None:
    errors.append(msg)


def warn(msg: str) -> None:
    warnings.append(msg)


def load_tracker() -> dict:
    if not TRACKER.exists():
        err(f"No existe {TRACKER}")
        return {}
    try:
        return json.loads(TRACKER.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        err(f"JSON inválido en {TRACKER}: {exc}")
        return {}


def validate(tracker: dict) -> None:
    if not tracker:
        return
    meta = tracker.get("meta", {})
    stages = tracker.get("stages", [])
    tasks = tracker.get("tasks", [])
    decisions = tracker.get("decisions", [])
    sessions = tracker.get("sessions", [])
    issues = tracker.get("issues", [])

    if not meta:
        err("Falta meta.")
    if not isinstance(stages, list) or not stages:
        err("Faltan etapas.")
    if not isinstance(tasks, list) or not tasks:
        err("Faltan tareas.")

    stage_ids: set[str] = set()
    for stage in stages:
        sid = stage.get("id")
        if not sid:
            err("Etapa sin id.")
            continue
        if sid in stage_ids:
            err(f"Etapa duplicada: {sid}")
        stage_ids.add(sid)
        if stage.get("status") not in VALID_STATUS:
            err(f"Etapa {sid} con estado inválido: {stage.get('status')}")
        for field in ("name", "objective", "type", "status"):
            if not stage.get(field):
                err(f"Etapa {sid} sin {field}.")

    task_ids: list[str] = []
    by_id: dict[str, dict] = {}
    for task in tasks:
        tid = task.get("id")
        if not tid:
            err("Tarea sin id.")
            continue
        if tid in by_id:
            err(f"Tarea duplicada: {tid}")
        by_id[tid] = task
        task_ids.append(tid)
        for field in REQUIRED_TASK_FIELDS:
            if field not in task:
                err(f"Tarea {tid} sin campo {field}.")
        if task.get("stageId") not in stage_ids:
            err(f"Tarea {tid} apunta a etapa inexistente: {task.get('stageId')}")
        if task.get("status") not in VALID_STATUS:
            err(f"Tarea {tid} con estado inválido: {task.get('status')}")
        if not isinstance(task.get("countsForProgress"), bool):
            err(f"Tarea {tid}: countsForProgress debe ser booleano.")
        if task.get("status") == "COMPLETADO":
            if not task.get("acceptanceCriteria"):
                err(f"Tarea {tid} COMPLETADO sin criterios de aceptación.")
            if not task.get("tests"):
                err(f"Tarea {tid} COMPLETADO sin pruebas.")
            if not task.get("evidence"):
                warn(f"Tarea {tid} COMPLETADO sin evidencias registradas.")

    # Dependencias
    for task in tasks:
        tid = task.get("id")
        for dep in task.get("dependencies", []):
            if dep == tid:
                err(f"Tarea {tid} depende de sí misma.")
            if dep not in by_id:
                err(f"Tarea {tid} depende de tarea inexistente: {dep}")

    # Ciclos
    visiting: set[str] = set()
    visited: set[str] = set()

    def dfs(tid: str) -> None:
        if tid in visited:
            return
        if tid in visiting:
            err(f"Ciclo de dependencias detectado en {tid}.")
            return
        visiting.add(tid)
        for dep in by_id[tid].get("dependencies", []):
            if dep in by_id:
                dfs(dep)
        visiting.remove(tid)
        visited.add(tid)

    for tid in by_id:
        dfs(tid)

    # Decisiones
    decision_ids: set[str] = set()
    for decision in decisions:
        did = decision.get("id")
        if not did:
            err("Decisión sin id.")
            continue
        if did in decision_ids:
            err(f"Decisión duplicada: {did}")
        decision_ids.add(did)
        for affected in decision.get("affectedTasks", []):
            if affected not in by_id:
                err(f"Decisión {did} afecta a tarea inexistente: {affected}")

    # Referencias a decisiones
    for task in tasks:
        for did in task.get("decisionIds", []):
            if did not in decision_ids:
                err(f"Tarea {task.get('id')} referencia decisión inexistente: {did}")

    # Sesiones
    session_ids: set[str] = set()
    for session in sessions:
        sid = session.get("id")
        if not sid:
            err("Sesión sin id.")
            continue
        if sid in session_ids:
            err(f"Sesión duplicada: {sid}")
        session_ids.add(sid)
        for tid in session.get("tasks", []):
            if tid not in by_id:
                err(f"Sesión {sid} referencia tarea inexistente: {tid}")

    # Issues
    issue_ids: set[str] = set()
    for issue in issues:
        iid = issue.get("id")
        if not iid:
            err("Incidencia sin id.")
            continue
        if iid in issue_ids:
            err(f"Incidencia duplicada: {iid}")
        issue_ids.add(iid)
        for tid in issue.get("affectedTasks", []):
            if tid not in by_id:
                err(f"Incidencia {iid} afecta a tarea inexistente: {tid}")

    # Meta
    current_stage = meta.get("currentStageId")
    if current_stage and current_stage not in stage_ids:
        err(f"meta.currentStageId apunta a etapa inexistente: {current_stage}")
    current_task = meta.get("currentTaskId")
    if current_task and current_task not in by_id:
        err(f"meta.currentTaskId apunta a tarea inexistente: {current_task}")
    next_task = meta.get("nextTaskId")
    if next_task and next_task not in by_id:
        err(f"meta.nextTaskId apunta a tarea inexistente: {next_task}")
    if next_task and next_task in by_id:
        nt = by_id[next_task]
        if nt.get("status") not in {"LISTO", "EN_PROGRESO"}:
            warn(f"meta.nextTaskId {next_task} no está LISTO ni EN_PROGRESO (está {nt.get('status')}).")
        pending = [d for d in nt.get("dependencies", []) if by_id.get(d, {}).get("status") != "COMPLETADO"]
        if pending:
            warn(f"meta.nextTaskId {next_task} tiene dependencias no completadas: {pending}")

    # Conteos de etapa
    for stage in stages:
        sid = stage.get("id")
        computed_total = sum(1 for t in tasks if t.get("stageId") == sid)
        computed_impl = sum(
            1
            for t in tasks
            if t.get("stageId") == sid
            and t.get("category") == "implementation"
            and t.get("countsForProgress") is True
        )
        if stage.get("taskCount") != computed_total:
            warn(f"Etapa {sid}: taskCount {stage.get('taskCount')} != {computed_total}.")
        if stage.get("implementationTaskCount") != computed_impl:
            warn(f"Etapa {sid}: implementationTaskCount {stage.get('implementationTaskCount')} != {computed_impl}.")

    # Estado de tarea en progreso
    in_progress = [t for t in tasks if t.get("status") == "EN_PROGRESO"]
    if len(in_progress) > 1:
        err(f"Hay más de una tarea EN_PROGRESO: {[t['id'] for t in in_progress]}")
    if in_progress and current_task != in_progress[0].get("id"):
        warn("Hay una tarea EN_PROGRESO que no coincide con meta.currentTaskId.")


def progress(tracker: dict) -> tuple[int, int, int]:
    tasks = tracker.get("tasks", [])
    impl = [t for t in tasks if t.get("category") == "implementation" and t.get("countsForProgress") is True]
    done = [t for t in impl if t.get("status") == "COMPLETADO"]
    return len(done), len(impl), (round(100 * len(done) / len(impl)) if impl else 0)


def dashboard_block(tracker: dict) -> str:
    raw = json.dumps(tracker, ensure_ascii=False, indent=2)
    # Evita cerrar el <script> si algún texto contiene </script>.
    raw = raw.replace("</", "<\\/")
    return f"{START}\n<script id=\"tracker-data\" type=\"application/json\">\n{raw}\n</script>\n{END}"


def embedded_tracker(html: str) -> dict | None:
    pattern = re.compile(
        re.escape(START) + r"\s*<script id=\"tracker-data\" type=\"application/json\">\s*(.*?)\s*</script>\s*" + re.escape(END),
        re.S,
    )
    match = pattern.search(html)
    if not match:
        return None
    try:
        return json.loads(match.group(1).replace("<\\/", "</"))
    except json.JSONDecodeError:
        return None


def sync(tracker: dict, check_only: bool) -> bool:
    if not DASHBOARD.exists():
        err(f"No existe {DASHBOARD}")
        return False
    for asset in (DASHBOARD_CSS, DASHBOARD_JS):
        if not asset.exists():
            err(f"Falta el recurso del panel: {asset}")
            return False
    html = DASHBOARD.read_text(encoding="utf-8")
    if START not in html or END not in html:
        err(f"El panel no contiene los marcadores {START} / {END}.")
        return False
    block = dashboard_block(tracker)
    pattern = re.compile(re.escape(START) + r".*?" + re.escape(END), re.S)
    new_html = pattern.sub(lambda _: block, html, count=1)
    if check_only:
        embedded = embedded_tracker(html)
        if embedded is None:
            err("No se pudo leer el bloque tracker-data del HTML.")
            return False
        if embedded != tracker:
            err("El bloque tracker-data del HTML no coincide con el JSON canónico.")
            return False
        return True
    tmp = DASHBOARD.with_suffix(".html.tmp")
    tmp.write_text(new_html, encoding="utf-8")
    tmp.replace(DASHBOARD)
    return True


def summary(tracker: dict) -> str:
    tasks = tracker.get("tasks", [])
    done, total, pct = progress(tracker)
    counts: dict[str, int] = {}
    for task in tasks:
        counts[task.get("status", "?")] = counts.get(task.get("status", "?"), 0) + 1
    stages = tracker.get("stages", [])
    return (
        f"Proyecto: {tracker.get('meta', {}).get('displayName', 'GalacticIndustry')}\n"
        f"Etapas: {len(stages)} | Tareas: {len(tasks)}\n"
        f"Implementación: {done}/{total} ({pct}%)\n"
        f"Estados: " + ", ".join(f"{k}={v}" for k, v in sorted(counts.items())) + "\n"
        f"Siguiente tarea: {tracker.get('meta', {}).get('nextTaskId')}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida y sincroniza el panel de continuidad.")
    parser.add_argument("--check", action="store_true", help="No escribe; comprueba que el HTML coincide.")
    parser.add_argument("--quiet", action="store_true", help="No imprime el resumen.")
    args = parser.parse_args()

    tracker = load_tracker()
    validate(tracker)
    if errors:
        print("ERRORES:", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        if warnings:
            print("AVISOS:", file=sys.stderr)
            for w in warnings:
                print(f"  - {w}", file=sys.stderr)
        return 1

    if not sync(tracker, args.check):
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        return 1

    if not args.quiet:
        if warnings:
            print("AVISOS:")
            for w in warnings:
                print(f"  - {w}")
        print(summary(tracker))
        print("OK: tracker válido y panel sincronizado." if not args.check else "OK: tracker válido y HTML coincide.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
