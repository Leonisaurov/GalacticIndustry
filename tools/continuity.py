#!/usr/bin/env python3
"""Coordinación descentralizada de sesiones para GalacticIndustry.

No hay un agente coordinador. Cada sesión usa este CLI para reservar su tarea,
aplicar cambios al tracker y sincronizar el panel sin pisar a las demás.

Uso rápido:
    python3 tools/continuity.py status
    python3 tools/continuity.py new-session --agent "OpenCode / modelo" --title "..."
    python3 tools/continuity.py claim T-XXX --session S-093 --agent "..."
    python3 tools/continuity.py apply plan.json          # aplica y sincroniza
    python3 tools/continuity.py append docs/INDEX.md --text "..."
    python3 tools/continuity.py lock-status

El plan de `apply` es un JSON con esta forma:

    {
      "sessionId": "S-093",
      "updatedAt": "2026-11-08T18:00:00-06:00",
      "operations": [
        {"op": "task.update", "id": "T-XXX", "expectUpdatedAt": "...",
         "set": {"status": "COMPLETADO"}, "append": {"evidence": ["..."]}},
        {"op": "session.add", "value": {...}},
        {"op": "meta.update", "set": {"lastSessionId": "S-093"}}
      ]
    }

Las operaciones disponibles son: task.update, updateTask.update, update.add,
update.update, decision.add, decision.update, session.add, session.update,
catalog.update y meta.update.

Reglas:
- El tracker se relee siempre dentro del bloqueo.
- `expectUpdatedAt` detecta que otra sesión tocó la misma tarea.
- Los cambios de tareas distintas se conservan.
- El JSON se valida antes de escribir.
- La escritura es atómica; una interrupción no deja JSON parcial.
- El bloqueo dura solo la lectura-modificación-escritura, no la tarea entera.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import os
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import continuity_io as cio  # noqa: E402

EXIT_OK = 0
EXIT_ERROR = 1
EXIT_CONFLICT = 2
EXIT_VALIDATION = 3
EXIT_LOCK_TIMEOUT = 4
EXIT_LOCK_UNAVAILABLE = 5

PROTECTED_TASK_FIELDS = {"id", "stageId", "updateId"}
PROTECTED_UPDATE_FIELDS = {"id", "tasks"}
PROTECTED_DECISION_FIELDS = {"id"}
PROTECTED_SESSION_FIELDS = {"id"}

_TASK_LIST_FIELDS = {
    "dependencies",
    "decisionIds",
    "files",
    "contextFiles",
    "contextDocs",
    "artifacts",
    "acceptanceCriteria",
    "tests",
    "executionLog",
    "evidence",
}


class ConflictError(cio.ContinuityError):
    """Otra sesión modificó el mismo campo o la misma tarea."""


class PlanError(cio.ContinuityError):
    """El plan de actualización no es válido."""


class ValidationError(cio.ContinuityError):
    """El tracker resultante no pasa el validador; no se escribe."""

    def __init__(self, errors: list[str], warnings: list[str] | None = None):
        self.errors = errors
        self.warnings = warnings or []
        super().__init__("; ".join(errors))


def now_iso() -> str:
    return datetime.now().astimezone().isoformat(timespec="seconds")


def root() -> Path:
    return cio.project_root()


def tracker_path(base: Path | None = None) -> Path:
    return Path(base or root()) / "docs" / "IMPLEMENTATION_TRACKER.json"


def load_sync_module(base: Path | None = None):
    """Carga el validador del sincronizador sin ejecutar su `main`."""
    base = Path(base or root())
    path = base / "tools" / "sync-dashboard.py"
    if not path.exists():
        raise PlanError(f"No existe el validador: {path}")
    spec = importlib.util.spec_from_file_location("gi_sync_dashboard", path)
    if spec is None or spec.loader is None:
        raise PlanError(f"No se pudo cargar el validador: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    # El validador calcula sus rutas al importarse. Si la sesión usa --root o
    # una raíz de sandbox, hay que apuntarlas a esa raíz, no a la del proceso.
    module.ROOT = base
    module.TRACKER = base / "docs" / "IMPLEMENTATION_TRACKER.json"
    module.STATUS = base / "docs" / "STATUS.md"
    module.INDEX = base / "docs" / "INDEX.md"
    module.DASHBOARD = base / "docs" / "dashboard" / "index.html"
    module.DASHBOARD_CSS = base / "docs" / "dashboard" / "dashboard.css"
    module.DASHBOARD_JS = base / "docs" / "dashboard" / "dashboard.js"
    return module


def validate_tracker(tracker: dict, base: Path | None = None) -> tuple[list[str], list[str]]:
    module = load_sync_module(base)
    module.errors.clear()
    module.warnings.clear()
    module.validate(tracker)
    return list(module.errors), list(module.warnings)


def _find_task(tracker: dict, task_id: str):
    for task in tracker.get("tasks", []):
        if task.get("id") == task_id:
            return task, None, None
    for update in tracker.get("updates", []):
        for task in update.get("tasks", []):
            if task.get("id") == task_id:
                return task, update, update.get("id")
    return None, None, None


def _find_update(tracker: dict, update_id: str):
    for update in tracker.get("updates", []):
        if update.get("id") == update_id:
            return update
    return None


def _find_decision(tracker: dict, decision_id: str):
    for decision in tracker.get("decisions", []):
        if decision.get("id") == decision_id:
            return decision
    return None


def _find_session(tracker: dict, session_id: str):
    for session in tracker.get("sessions", []):
        if session.get("id") == session_id:
            return session
    return None


def _require_dict(value: object, name: str) -> dict:
    if value is None:
        return {}
    if not isinstance(value, dict):
        raise PlanError(f"{name} debe ser un objeto JSON.")
    return value


def _apply_set(target: dict, values: object, protected: set[str], name: str) -> None:
    for key, value in _require_dict(values, f"{name}.set").items():
        if key in protected:
            raise PlanError(f"{name}: no se puede cambiar el campo protegido {key}.")
        target[key] = value


def _apply_append(target: dict, values: object, name: str) -> None:
    for key, value in _require_dict(values, f"{name}.append").items():
        current = target.get(key)
        if current is None:
            current = []
            target[key] = current
        if not isinstance(current, list):
            raise PlanError(f"{name}.append.{key}: el campo actual no es una lista.")
        if isinstance(value, list):
            current.extend(value)
        else:
            current.append(value)


def _check_task_expectation(task: dict, op: dict, session_id: str | None) -> None:
    expected = op.get("expectUpdatedAt")
    if expected is not None and task.get("updatedAt") != expected:
        raise ConflictError(
            f"La tarea {task.get('id')} cambió desde la lectura "
            f"(esperado {expected!r}, actual {task.get('updatedAt')!r}). "
            "Vuelve a leer el tracker y reintenta."
        )
    if expected is None and not op.get("force"):
        raise PlanError(
            f"task.update {task.get('id')}: falta expectUpdatedAt. "
            "Úsalo para detectar conflictos o añade force=true de forma justificada."
        )
    assigned = task.get("assignedSessionId")
    if (
        task.get("status") == "EN_PROGRESO"
        and assigned
        and session_id
        and assigned != session_id
        and not op.get("allowReassign")
    ):
        raise ConflictError(
            f"La tarea {task.get('id')} está EN_PROGRESO y asignada a {assigned}; "
            f"esta sesión es {session_id}. No se reasigna automáticamente."
        )


def _apply_task_update(tracker: dict, op: dict, session_id: str | None) -> None:
    task_id = op.get("id")
    if not task_id:
        raise PlanError("task.update sin id.")
    task, update, update_id = _find_task(tracker, task_id)
    if task is None:
        raise PlanError(f"task.update: no existe la tarea {task_id}.")
    _check_task_expectation(task, op, session_id)
    _apply_set(task, op.get("set"), PROTECTED_TASK_FIELDS, f"task {task_id}")
    _apply_append(task, op.get("append"), f"task {task_id}")
    task["updatedAt"] = op.get("updatedAt") or now_iso()
    if update_id:
        update["updatedAt"] = op.get("updatedAt") or now_iso()


def _apply_update_task_update(tracker: dict, op: dict, session_id: str | None) -> None:
    task_id = op.get("id")
    update_id = op.get("updateId")
    if not task_id or not update_id:
        raise PlanError("updateTask.update requiere id y updateId.")
    update = _find_update(tracker, update_id)
    if update is None:
        raise PlanError(f"updateTask.update: no existe la actualización {update_id}.")
    task = next((t for t in update.get("tasks", []) if t.get("id") == task_id), None)
    if task is None:
        raise PlanError(f"updateTask.update: {task_id} no pertenece a {update_id}.")
    _check_task_expectation(task, op, session_id)
    _apply_set(task, op.get("set"), PROTECTED_TASK_FIELDS, f"task {task_id}")
    _apply_append(task, op.get("append"), f"task {task_id}")
    task["updatedAt"] = op.get("updatedAt") or now_iso()
    update["updatedAt"] = op.get("updatedAt") or now_iso()


def _apply_update_add(tracker: dict, op: dict) -> None:
    value = _require_dict(op.get("value"), "update.add.value")
    update_id = value.get("id")
    if not update_id:
        raise PlanError("update.add sin id.")
    if _find_update(tracker, update_id) is not None:
        raise PlanError(f"update.add: ya existe {update_id}.")
    tracker.setdefault("updates", []).append(value)


def _apply_update_update(tracker: dict, op: dict) -> None:
    update_id = op.get("id")
    update = _find_update(tracker, update_id) if update_id else None
    if update is None:
        raise PlanError(f"update.update: no existe {update_id}.")
    _apply_set(update, op.get("set"), PROTECTED_UPDATE_FIELDS, f"update {update_id}")
    _apply_append(update, op.get("append"), f"update {update_id}")
    update["updatedAt"] = op.get("updatedAt") or now_iso()


def _apply_decision_add(tracker: dict, op: dict) -> None:
    value = _require_dict(op.get("value"), "decision.add.value")
    decision_id = value.get("id")
    if not decision_id:
        raise PlanError("decision.add sin id.")
    if _find_decision(tracker, decision_id) is not None:
        raise PlanError(f"decision.add: ya existe {decision_id}.")
    tracker.setdefault("decisions", []).append(value)


def _apply_decision_update(tracker: dict, op: dict) -> None:
    decision_id = op.get("id")
    decision = _find_decision(tracker, decision_id) if decision_id else None
    if decision is None:
        raise PlanError(f"decision.update: no existe {decision_id}.")
    _apply_set(decision, op.get("set"), PROTECTED_DECISION_FIELDS, f"decision {decision_id}")
    _apply_append(decision, op.get("append"), f"decision {decision_id}")
    decision["updatedAt"] = op.get("updatedAt") or now_iso()


def _apply_session_add(tracker: dict, op: dict) -> None:
    value = _require_dict(op.get("value"), "session.add.value")
    session_id = value.get("id")
    if not session_id:
        raise PlanError("session.add sin id.")
    if _find_session(tracker, session_id) is not None:
        raise PlanError(f"session.add: ya existe {session_id}.")
    tracker.setdefault("sessions", []).append(value)


def _apply_session_update(tracker: dict, op: dict) -> None:
    session_id = op.get("id")
    session = _find_session(tracker, session_id) if session_id else None
    if session is None:
        raise PlanError(f"session.update: no existe {session_id}.")
    _apply_set(session, op.get("set"), PROTECTED_SESSION_FIELDS, f"session {session_id}")
    _apply_append(session, op.get("append"), f"session {session_id}")


def _apply_meta_update(tracker: dict, op: dict) -> None:
    meta = tracker.setdefault("meta", {})
    for key, expected in _require_dict(op.get("expect"), "meta.update.expect").items():
        if meta.get(key) != expected:
            raise ConflictError(
                f"meta.{key} cambió (esperado {expected!r}, actual {meta.get(key)!r})."
            )
    _apply_set(meta, op.get("set"), {"schemaVersion"}, "meta")


def _apply_catalog_update(tracker: dict, op: dict) -> None:
    """Registra una actualización en el catálogo de revisión.

    El catálogo agrupa sistemas y funcionalidades ya documentados; no es una
    segunda fuente de verdad. Esta operación evita que una actualización nueva
    quede fuera del expediente de revisión.
    """
    update_id = op.get("updateId")
    if not update_id:
        raise PlanError("catalog.update requiere updateId.")
    if _find_update(tracker, update_id) is None:
        raise PlanError(f"catalog.update: no existe {update_id}.")
    catalog = tracker.setdefault("reviewCatalog", {})
    groups = (
        ("systems", "systems", "sistema"),
        ("features", "features", "funcionalidad"),
    )
    for group_key, op_key, label in groups:
        entries = catalog.get(group_key, [])
        ids = op.get(op_key, [])
        if not isinstance(ids, list):
            raise PlanError(f"catalog.update.{op_key} debe ser una lista.")
        for entry_id in ids:
            entry = next((e for e in entries if e.get("id") == entry_id), None)
            if entry is None:
                raise PlanError(
                    f"catalog.update: no existe {label} {entry_id}."
                )
            linked = entry.setdefault("updateIds", [])
            if update_id not in linked:
                linked.append(update_id)


OPERATIONS = {
    "task.update": _apply_task_update,
    "updateTask.update": _apply_update_task_update,
    "update.add": _apply_update_add,
    "update.update": _apply_update_update,
    "decision.add": _apply_decision_add,
    "decision.update": _apply_decision_update,
    "session.add": _apply_session_add,
    "session.update": _apply_session_update,
    "meta.update": _apply_meta_update,
    "catalog.update": _apply_catalog_update,
}


def apply_operations(tracker: dict, plan: dict) -> None:
    if not isinstance(plan, dict):
        raise PlanError("El plan debe ser un objeto JSON.")
    operations = plan.get("operations")
    if not isinstance(operations, list) or not operations:
        raise PlanError("El plan necesita una lista no vacía 'operations'.")
    session_id = plan.get("sessionId")
    updated_at = plan.get("updatedAt") or now_iso()
    for index, op in enumerate(operations, start=1):
        if not isinstance(op, dict):
            raise PlanError(f"Operación {index}: debe ser un objeto.")
        name = op.get("op")
        handler = OPERATIONS.get(name)
        if handler is None:
            raise PlanError(f"Operación {index}: tipo desconocido {name!r}.")
        if name in {"task.update", "updateTask.update"}:
            handler(tracker, op, session_id)
        else:
            handler(tracker, op)
    meta = tracker.setdefault("meta", {})
    meta["lastUpdated"] = updated_at
    if plan.get("closeSession") and session_id:
        meta["lastSessionId"] = session_id
        session = _find_session(tracker, session_id)
        if session is not None and session.get("outcome") == "EN_PROGRESO":
            session["outcome"] = "Completado"


def write_tracker(tracker: dict, base: Path | None = None) -> list[str]:
    errors, warnings = validate_tracker(tracker, base)
    if errors:
        raise ValidationError(errors, warnings)
    cio.atomic_write_json(tracker_path(base), tracker)
    return warnings


def apply_plan(plan: dict, sync: bool = True, base: Path | None = None, timeout: float | None = None) -> list[str]:
    base = Path(base or root()).resolve()
    with cio.project_lock(base, timeout=timeout):
        tracker = cio.read_json(tracker_path(base))
        apply_operations(tracker, plan)
        warnings = write_tracker(tracker, base)
    if sync:
        run_sync(base)
    return warnings


def run_sync(base: Path | None = None) -> None:
    base = Path(base or root()).resolve()
    env = dict(os.environ)
    env["GI_PROJECT_ROOT"] = str(base)
    result = subprocess.run(
        [sys.executable, str(base / "tools" / "sync-dashboard.py"), "--quiet"],
        env=env,
        check=False,
    )
    if result.returncode != 0:
        raise cio.ContinuityError(
            f"El sincronizador falló con código {result.returncode}."
        )


def claim_task(
    task_id: str,
    session_id: str,
    agent: str,
    summary: str = "",
    base: Path | None = None,
    timeout: float | None = None,
    allow_reassign: bool = False,
    reason: str = "",
    sync: bool = True,
) -> bool:
    base = Path(base or root()).resolve()
    changed = False
    with cio.project_lock(base, timeout=timeout):
        tracker = cio.read_json(tracker_path(base))
        task, update, update_id = _find_task(tracker, task_id)
        if task is None:
            raise PlanError(f"No existe la tarea {task_id}.")
        if update is not None and update.get("status") not in {"APROBADA", "EN_PROGRESO"}:
            raise PlanError(
                f"{task_id} pertenece a {update_id}, que está "
                f"{update.get('status')}; no se puede reclamar sin aprobación."
            )
        status = task.get("status")
        assigned = task.get("assignedSessionId")
        if status == "COMPLETADO":
            raise PlanError(f"{task_id} ya está COMPLETADO.")
        if status in {"POSPUESTO", "BLOQUEADO", "REQUIERE_DECISION"}:
            raise PlanError(f"{task_id} está {status}; no se puede reclamar.")
        if status == "EN_PROGRESO":
            if assigned == session_id:
                print(f"OK: {task_id} ya está asignada a {session_id}.")
                return False
            if not allow_reassign:
                raise ConflictError(
                    f"{task_id} está EN_PROGRESO"
                    + (f" y asignada a {assigned}" if assigned else " sin responsable registrado")
                    + f". No se reasigna automáticamente. Si la sesión está abandonada, "
                    "inspecciona el trabajo y usa --allow-reassign --reason '...'."
                )
            if not reason.strip():
                raise PlanError("--allow-reassign exige --reason con la justificación.")
        # Las dependencias pueden apuntar a tareas del plan base o a otras
        # tareas de actualización (por ejemplo UPD-003-T02 -> UPD-003-T01).
        by_id = {t.get("id"): t for t in tracker.get("tasks", [])}
        for upd in tracker.get("updates", []):
            for upd_task in upd.get("tasks", []):
                by_id.setdefault(upd_task.get("id"), upd_task)
        pending_deps = [
            dep
            for dep in task.get("dependencies", [])
            if by_id.get(dep, {}).get("status") != "COMPLETADO"
        ]
        if pending_deps:
            raise PlanError(f"{task_id} tiene dependencias sin completar: {pending_deps}.")
        pending_decisions = [
            did
            for did in task.get("decisionIds", [])
            if (_find_decision(tracker, did) or {}).get("status") == "PENDIENTE"
        ]
        if pending_decisions:
            raise PlanError(
                f"{task_id} depende de decisiones pendientes: {pending_decisions}."
            )
        now = now_iso()
        task["status"] = "EN_PROGRESO"
        task["assignedSessionId"] = session_id
        task["assignedAt"] = now
        task["updatedAt"] = now
        log = task.setdefault("executionLog", [])
        entry = {
            "sessionId": session_id,
            "date": now,
            "agent": agent,
            "status": "EN_PROGRESO",
            "summary": summary or f"Tarea reclamada por {session_id}.",
        }
        if allow_reassign:
            entry["summary"] += f" Reasignada: {reason.strip()}"
        log.append(entry)
        meta = tracker.setdefault("meta", {})
        meta["lastUpdated"] = now
        if update_id:
            meta["currentUpdateId"] = update_id
            meta["currentTaskId"] = None
            if update is not None and update.get("status") == "APROBADA":
                update["status"] = "EN_PROGRESO"
                update["updatedAt"] = now
        else:
            meta["currentStageId"] = task.get("stageId")
            meta["currentTaskId"] = task_id
        write_tracker(tracker, base)
        changed = True
    if changed and sync:
        run_sync(base)
    print(
        f"OK: {task_id} reclamada por {session_id}"
        + (f" (actualización {update_id})" if update_id else "")
        + "."
    )
    return changed


def _next_session_id(tracker: dict, base: Path) -> str:
    numbers = []
    for session in tracker.get("sessions", []):
        match = re.fullmatch(r"S-(\d+)", str(session.get("id", "")))
        if match:
            numbers.append(int(match.group(1)))
    sessions_dir = base / "docs" / "sessions"
    if sessions_dir.exists():
        for path in sessions_dir.glob("S-*.md"):
            match = re.fullmatch(r"S-(\d+)\.md", path.name)
            if match:
                numbers.append(int(match.group(1)))
    return f"S-{max(numbers, default=0) + 1:03d}"


def new_session(
    agent: str,
    title: str,
    task_id: str = "",
    update_id: str = "",
    base: Path | None = None,
    timeout: float | None = None,
) -> str:
    base = Path(base or root()).resolve()
    sessions_dir = base / "docs" / "sessions"
    sessions_dir.mkdir(parents=True, exist_ok=True)
    with cio.project_lock(base, timeout=timeout):
        tracker = cio.read_json(tracker_path(base))
        session_id = _next_session_id(tracker, base)
        path = sessions_dir / f"{session_id}.md"
        date = now_iso()
        content = f"""# {session_id} — {title.strip() or "Sesión sin título"}

- **Estado:** EN_PROGRESO
- **Fecha:** {date}
- **Agente:** {agent.strip() or "No documentado"}
- **Tarea:** {task_id or "No asignada"}
- **Actualización:** {update_id or "Ninguna"}
- **Documento canónico:** `docs/IMPLEMENTATION_TRACKER.json` (la sesión se registra al abrirse)

## Resumen

Pendiente.

## Archivos modificados

Pendiente.

## Pruebas ejecutadas

Pendiente.

## Evidencias

Pendiente.

## Cambios ajenos detectados

Pendiente.

## Pendiente / siguiente paso

Pendiente.
"""
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o644)
        try:
            with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as handle:
                handle.write(content)
                handle.flush()
                os.fsync(handle.fileno())
        except OSError:
            try:
                path.unlink()
            except OSError:
                pass
            raise
        # La sesión se registra al abrirse para que STATUS.md y el panel puedan
        # mostrar sesiones activas y detectar una sesión interrumpida. Se cierra
        # con session.update (outcome) o con plan.closeSession.
        planned_task_ids = []
        if task_id and _find_task(tracker, task_id)[0] is not None:
            planned_task_ids.append(task_id)
        tracker.setdefault("sessions", []).append({
            "id": session_id,
            "date": date,
            "agent": agent.strip() or "No documentado",
            "title": title.strip() or "Sesión sin título",
            "summary": "Sesión abierta; pendiente de cierre.",
            "tasks": planned_task_ids,
            "files": [],
            "tests": [],
            "outcome": "EN_PROGRESO",
            "notes": f"Actualización prevista: {update_id or 'ninguna'}.",
            "document": f"docs/sessions/{session_id}.md",
        })
        meta = tracker.setdefault("meta", {})
        meta["lastUpdated"] = date
        try:
            write_tracker(tracker, base)
        except Exception:
            try:
                path.unlink()
            except OSError:
                pass
            raise
    print(session_id)
    print(path)
    return session_id


def append_file(path_arg: str, text: str, base: Path | None = None, timeout: float | None = None) -> None:
    base = (base or root()).resolve()
    target = _project_file(base, path_arg)
    with cio.project_lock(base, timeout=timeout):
        current = target.read_text(encoding="utf-8") if target.exists() else ""
        if current and not current.endswith("\n"):
            current += "\n"
        if text and not text.endswith("\n"):
            text += "\n"
        cio.atomic_write_text(target, current + text)
    print(f"OK: añadido texto a {target.relative_to(base)}")


def _project_file(base: Path, path_arg: str) -> Path:
    target = (base / path_arg).resolve()
    try:
        target.relative_to(base)
    except ValueError:
        raise PlanError(f"La ruta debe estar dentro del proyecto: {path_arg}") from None
    return target


def replace_text(
    path_arg: str,
    old: str,
    new: str,
    count: int = 1,
    base: Path | None = None,
    timeout: float | None = None,
) -> None:
    """Sustituye texto exacto en un documento compartido, con bloqueo y validación.

    No reemplaza si el texto no existe o si aparece un número distinto de veces.
    Así dos sesiones no se pisan con un reemplazo global accidental.
    """
    if not old:
        raise PlanError("replace necesita un texto antiguo no vacío.")
    if count < 1:
        raise PlanError("replace necesita count >= 1.")
    base = (base or root()).resolve()
    target = _project_file(base, path_arg)
    with cio.project_lock(base, timeout=timeout):
        current = target.read_text(encoding="utf-8") if target.exists() else ""
        found = current.count(old)
        if found != count:
            raise ConflictError(
                f"{path_arg}: el texto aparece {found} veces, se esperaban {count}. "
                "Relee el archivo; otra sesión pudo cambiarlo."
            )
        updated = current.replace(old, new, count)
        cio.atomic_write_text(target, updated)
    print(f"OK: {count} sustitución(es) en {target.relative_to(base)}")


def status(base: Path | None = None, as_json: bool = False) -> dict:
    base = Path(base or root()).resolve()
    with cio.project_lock(base, timeout=10.0, shared=True):
        tracker = cio.read_json(tracker_path(base))
    base_tasks = tracker.get("tasks", [])
    update_tasks = [t for u in tracker.get("updates", []) for t in u.get("tasks", [])]
    active = [t for t in base_tasks + update_tasks if t.get("status") == "EN_PROGRESO"]
    active_sessions = [
        s for s in tracker.get("sessions", []) if s.get("outcome") == "EN_PROGRESO"
    ]
    data = {
        "project": tracker.get("meta", {}).get("displayName"),
        "lastUpdated": tracker.get("meta", {}).get("lastUpdated"),
        "lastSessionId": tracker.get("meta", {}).get("lastSessionId"),
        "currentStageId": tracker.get("meta", {}).get("currentStageId"),
        "currentTaskId": tracker.get("meta", {}).get("currentTaskId"),
        "currentUpdateId": tracker.get("meta", {}).get("currentUpdateId"),
        "nextTaskId": tracker.get("meta", {}).get("nextTaskId"),
        "activeTasks": [
            {
                "id": t.get("id"),
                "title": t.get("title"),
                "assignedSessionId": t.get("assignedSessionId"),
                "assignedAt": t.get("assignedAt"),
            }
            for t in active
        ],
        "activeSessions": [s.get("id") for s in active_sessions],
        "updates": [
            {"id": u.get("id"), "status": u.get("status")}
            for u in tracker.get("updates", [])
        ],
    }
    if as_json:
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print(f"Proyecto: {data['project']}")
        print(f"Última actualización: {data['lastUpdated']} · última sesión: {data['lastSessionId']}")
        print(f"Etapa: {data['currentStageId']} · tarea: {data['currentTaskId']} · actualización: {data['currentUpdateId']}")
        print(f"Próxima tarea: {data['nextTaskId']}")
        if data["activeTasks"]:
            for task in data["activeTasks"]:
                print(
                    f"EN_PROGRESO: {task['id']} · {task['title']} · "
                    f"{task['assignedSessionId'] or 'sin sesión'} · {task['assignedAt'] or 'sin fecha'}"
                )
        else:
            print("EN_PROGRESO: ninguna tarea")
        if data["activeSessions"]:
            print("Sesiones abiertas: " + ", ".join(data["activeSessions"]))
        if data["updates"]:
            print("Actualizaciones: " + ", ".join(f"{u['id']}={u['status']}" for u in data["updates"]))
    return data


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Coordinación descentralizada y escrituras seguras."
    )
    parser.add_argument(
        "--root",
        default=None,
        help="Raíz del proyecto (por defecto GI_PROJECT_ROOT o el directorio padre de tools/).",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p_status = sub.add_parser("status", help="Resumen de tareas y sesiones activas.")
    p_status.add_argument("--json", action="store_true", help="Salida JSON.")

    p_lock = sub.add_parser("lock-status", help="Comprueba si el bloqueo está libre.")

    p_new = sub.add_parser("new-session", help="Reserva un ID de sesión y crea su archivo.")
    p_new.add_argument("--agent", default="", help="Agente y modelo.")
    p_new.add_argument("--title", required=True, help="Título de la sesión.")
    p_new.add_argument("--task", default="", help="Tarea prevista (opcional).")
    p_new.add_argument("--update", default="", help="Actualización prevista (opcional).")
    p_new.add_argument("--timeout", type=float, default=None)

    p_claim = sub.add_parser("claim", help="Reserva una tarea para una sesión.")
    p_claim.add_argument("task_id")
    p_claim.add_argument("--session", required=True)
    p_claim.add_argument("--agent", default="")
    p_claim.add_argument("--summary", default="")
    p_claim.add_argument("--allow-reassign", action="store_true")
    p_claim.add_argument("--reason", default="")
    p_claim.add_argument("--no-sync", action="store_true")
    p_claim.add_argument("--timeout", type=float, default=None)

    p_apply = sub.add_parser("apply", help="Aplica un plan JSON al tracker.")
    p_apply.add_argument("plan")
    p_apply.add_argument("--no-sync", action="store_true")
    p_apply.add_argument("--timeout", type=float, default=None)

    p_append = sub.add_parser("append", help="Añade texto a un archivo con bloqueo.")
    p_append.add_argument("file")
    p_append.add_argument("--text", default="")
    p_append.add_argument("--text-file", default="")
    p_append.add_argument("--timeout", type=float, default=None)

    p_replace = sub.add_parser(
        "replace", help="Sustituye texto exacto en un documento compartido con bloqueo."
    )
    p_replace.add_argument("file")
    p_replace.add_argument("--old", default="")
    p_replace.add_argument("--old-file", default="")
    p_replace.add_argument("--new", default="")
    p_replace.add_argument("--new-file", default="")
    p_replace.add_argument("--count", type=int, default=1)
    p_replace.add_argument("--timeout", type=float, default=None)

    args = parser.parse_args()
    base = Path(args.root).expanduser().resolve() if args.root else root()

    try:
        if args.command == "status":
            status(base, args.json)
            return EXIT_OK
        if args.command == "lock-status":
            if cio.try_lock(base, shared=False):
                print("libre")
                return EXIT_OK
            print("ocupado")
            return EXIT_ERROR
        if args.command == "new-session":
            new_session(
                args.agent,
                args.title,
                args.task,
                args.update,
                base,
                args.timeout,
            )
            return EXIT_OK
        if args.command == "claim":
            claim_task(
                args.task_id,
                args.session,
                args.agent,
                args.summary,
                base,
                args.timeout,
                args.allow_reassign,
                args.reason,
                not args.no_sync,
            )
            return EXIT_OK
        if args.command == "apply":
            plan_path = Path(args.plan)
            if not plan_path.is_absolute():
                plan_path = (Path.cwd() / plan_path).resolve()
            plan = cio.read_json(plan_path)
            warnings = apply_plan(plan, not args.no_sync, base, args.timeout)
            for warning in warnings:
                print(f"AVISO: {warning}", file=sys.stderr)
            print(f"OK: plan aplicado y tracker válido ({plan_path.name}).")
            return EXIT_OK
        if args.command == "append":
            text = args.text
            if args.text_file:
                text = Path(args.text_file).read_text(encoding="utf-8")
            if not text:
                raise PlanError("append necesita --text o --text-file.")
            append_file(args.file, text, base, args.timeout)
            return EXIT_OK
        if args.command == "replace":
            old = args.old
            new = args.new
            if args.old_file:
                old = Path(args.old_file).read_text(encoding="utf-8")
            if args.new_file:
                new = Path(args.new_file).read_text(encoding="utf-8")
            replace_text(args.file, old, new, args.count, base, args.timeout)
            return EXIT_OK
        raise PlanError(f"Comando desconocido: {args.command}")
    except ConflictError as exc:
        print(f"CONFLICTO: {exc}", file=sys.stderr)
        return EXIT_CONFLICT
    except ValidationError as exc:
        print("ERRORES DE VALIDACIÓN (no se escribió nada):", file=sys.stderr)
        for error in exc.errors:
            print(f"  - {error}", file=sys.stderr)
        for warning in exc.warnings:
            print(f"  AVISO: {warning}", file=sys.stderr)
        return EXIT_VALIDATION
    except cio.LockTimeout as exc:
        print(f"BLOQUEO: {exc}", file=sys.stderr)
        return EXIT_LOCK_TIMEOUT
    except cio.LockUnavailable as exc:
        print(f"PLATAFORMA: {exc}", file=sys.stderr)
        return EXIT_LOCK_UNAVAILABLE
    except (cio.ContinuityError, PlanError, OSError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return EXIT_ERROR


if __name__ == "__main__":
    sys.exit(main())
