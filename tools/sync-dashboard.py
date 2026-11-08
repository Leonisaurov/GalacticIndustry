#!/usr/bin/env python3
"""Valida y sincroniza el panel de continuidad de GalacticIndustry.

Uso:
    python3 tools/sync-dashboard.py            # valida, sincroniza el HTML y genera STATUS.md
    python3 tools/sync-dashboard.py --check    # solo valida y comprueba HTML y STATUS.md
    python3 tools/sync-dashboard.py --quiet    # no imprime el resumen, solo errores

Además del HTML, este script genera `docs/STATUS.md` a partir del tracker. Ese
archivo es derivado: no se edita a mano y nunca es una segunda fuente de verdad.

Este script no es un servidor y no usa red. Es una herramienta local de build.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import continuity_io as cio  # noqa: E402

ROOT = cio.project_root()
TRACKER = ROOT / "docs" / "IMPLEMENTATION_TRACKER.json"
STATUS = ROOT / "docs" / "STATUS.md"
INDEX = ROOT / "docs" / "INDEX.md"
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

VALID_UPDATE_STATUS = {
    "PROPUESTA",
    "EN_REVISION",
    "PENDIENTE_APROBACION",
    "APROBADA",
    "EN_PROGRESO",
    "BLOQUEADA",
    "COMPLETADA",
    "RECHAZADA",
    "ARCHIVADA",
}

APPROVED_UPDATE_STATUS = {"APROBADA", "EN_PROGRESO", "COMPLETADA"}

VALID_CHANGE_TYPES = {
    "CORRECCION",
    "MEJORA",
    "DISENO",
    "CARACTERISTICA",
    "REEQUILIBRIO",
    "REFACTORIZACION",
    "TECNICA",
    "INFRAESTRUCTURA",
}

VALID_PRIORITIES = {"P0", "P1", "P2", "P3"}

VERSION_RE = re.compile(r"^\d+\.\d+$")

REQUIRED_UPDATE_FIELDS = [
    "id",
    "name",
    "status",
    "changeType",
    "priority",
    "createdAt",
    "updatedAt",
    "description",
    "motivation",
    "affectedSystems",
    "affectedFeatures",
    "relatedStages",
    "relatedTasks",
    "relatedUpdates",
    "document",
    "approval",
    "dependencies",
    "acceptanceCriteria",
    "tasks",
    "observations",
    "implementationLog",
    "reviewExports",
]

REQUIRED_UPDATE_TASK_FIELDS = [
    "id",
    "updateId",
    "title",
    "description",
    "category",
    "priority",
    "status",
    "countsForProgress",
    "dependencies",
    "relatedTaskIds",
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

# Campos opcionales de contexto. `files` se conserva por compatibilidad; los
# nuevos campos separan lo que hay que leer de lo que se modificó al ejecutar.
OPTIONAL_TASK_LIST_FIELDS = ("contextFiles", "contextDocs", "artifacts")
OPTIONAL_TASK_SCALAR_FIELDS = ("assignedSessionId", "assignedAt")

errors: list[str] = []
warnings: list[str] = []


def err(msg: str) -> None:
    errors.append(msg)


def warn(msg: str) -> None:
    warnings.append(msg)


def check_optional_task_fields(task: dict, tid: str) -> None:
    """Valida los campos opcionales de contexto sin exigirlos."""
    for field in OPTIONAL_TASK_LIST_FIELDS:
        if field not in task:
            continue
        value = task[field]
        if not isinstance(value, list):
            err(f"Tarea {tid}: {field} debe ser una lista.")
            continue
        for item in value:
            if not isinstance(item, str) or not item.strip():
                err(f"Tarea {tid}: {field} contiene un valor no textual o vacío.")
    for field in OPTIONAL_TASK_SCALAR_FIELDS:
        if field in task and not isinstance(task[field], str):
            err(f"Tarea {tid}: {field} debe ser texto.")


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
        check_optional_task_fields(task, tid)
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

    # Actualizaciones complementarias
    updates = tracker.get("updates", [])
    if not isinstance(updates, list):
        err("updates debe ser una lista.")
        updates = []

    update_defs = tracker.get("updateStatusDefinitions", {})
    if not isinstance(update_defs, dict):
        err("updateStatusDefinitions debe ser un objeto.")
        update_defs = {}
    for status in VALID_UPDATE_STATUS:
        if status not in update_defs:
            err(f"updateStatusDefinitions sin el estado {status}.")

    all_by_id: dict[str, dict] = dict(by_id)
    update_ids: set[str] = set()
    update_by_id: dict[str, dict] = {}
    update_task_by_id: dict[str, dict] = {}
    display_versions: dict[str, str] = {}

    for upd in updates:
        uid = upd.get("id")
        if not uid:
            err("Actualización sin id.")
            continue
        if not re.fullmatch(r"UPD-\d{3}", uid):
            err(f"Actualización con id inválido: {uid}")
        if uid in update_ids:
            err(f"Actualización duplicada: {uid}")
        update_ids.add(uid)
        update_by_id[uid] = upd

        for field in REQUIRED_UPDATE_FIELDS:
            if field not in upd:
                err(f"Actualización {uid} sin campo {field}.")
        if upd.get("status") not in VALID_UPDATE_STATUS:
            err(f"Actualización {uid} con estado inválido: {upd.get('status')}")
        if upd.get("changeType") not in VALID_CHANGE_TYPES:
            err(f"Actualización {uid} con tipo de cambio inválido: {upd.get('changeType')}")
        if upd.get("priority") not in VALID_PRIORITIES:
            err(f"Actualización {uid} con prioridad inválida: {upd.get('priority')}")
        if not upd.get("name"):
            err(f"Actualización {uid} sin nombre.")
        if not upd.get("description"):
            err(f"Actualización {uid} sin descripción.")
        if not upd.get("motivation"):
            err(f"Actualización {uid} sin motivación.")

        display_version = upd.get("displayVersion")
        if display_version is None:
            warn(f"Actualización {uid} sin displayVersion; el panel usará una etiqueta derivada.")
        elif not isinstance(display_version, str) or not VERSION_RE.fullmatch(display_version):
            err(f"Actualización {uid} con displayVersion inválida: {display_version!r}")
        else:
            if display_version in display_versions:
                err(
                    f"displayVersion duplicada: {display_version} en "
                    f"{display_versions[display_version]} y {uid}."
                )
            display_versions[display_version] = uid

        if "objective" in upd and not isinstance(upd.get("objective"), str):
            err(f"Actualización {uid}: objective debe ser texto.")
        elif not upd.get("objective"):
            warn(f"Actualización {uid} sin objective; el panel usará la descripción o motivación.")

        document = upd.get("document")
        if not document:
            err(f"Actualización {uid} sin documento.")
        else:
            doc_path = (ROOT / document).resolve()
            try:
                doc_path.relative_to(ROOT)
            except ValueError:
                err(f"Actualización {uid}: document apunta fuera del proyecto: {document}")
            else:
                if not doc_path.exists():
                    err(f"Actualización {uid}: no existe el documento {document}")
            if not document.startswith("docs/updates/"):
                warn(f"Actualización {uid}: document no está en docs/updates/: {document}")

        approval = upd.get("approval")
        if not isinstance(approval, dict):
            err(f"Actualización {uid}: approval debe ser un objeto.")
            approval = {}
        if not isinstance(approval.get("required"), bool):
            err(f"Actualización {uid}: approval.required debe ser booleano.")
        if upd.get("status") in APPROVED_UPDATE_STATUS:
            if approval.get("status") != "APROBADA":
                err(f"Actualización {uid} en {upd.get('status')} sin approval.status=APROBADA.")
            if not approval.get("approvedBy"):
                err(f"Actualización {uid} aprobada sin approvedBy.")
            if not approval.get("approvedAt"):
                err(f"Actualización {uid} aprobada sin approvedAt.")

        for sid in upd.get("relatedStages", []):
            if sid not in stage_ids:
                err(f"Actualización {uid} referencia etapa inexistente: {sid}")
        for tid in upd.get("relatedTasks", []):
            if tid not in by_id:
                err(f"Actualización {uid} referencia tarea histórica inexistente: {tid}")

        if not isinstance(upd.get("tasks"), list):
            err(f"Actualización {uid}: tasks debe ser una lista.")
            continue

        for task in upd["tasks"]:
            tid = task.get("id")
            if not tid:
                err(f"Actualización {uid}: tarea sin id.")
                continue
            expected = re.compile(rf"^{re.escape(uid)}-T\d{{2,}}$")
            if not expected.fullmatch(tid):
                err(f"Tarea de actualización con id inválido para {uid}: {tid}")
            if tid in all_by_id:
                err(f"Tarea de actualización duplicada o en conflicto con el plan base: {tid}")
            all_by_id[tid] = task
            update_task_by_id[tid] = task
            for field in REQUIRED_UPDATE_TASK_FIELDS:
                if field not in task:
                    err(f"Tarea {tid} sin campo {field}.")
            check_optional_task_fields(task, tid)
            if task.get("updateId") != uid:
                err(f"Tarea {tid}: updateId {task.get('updateId')} no coincide con {uid}.")
            if task.get("status") not in VALID_STATUS:
                err(f"Tarea {tid} con estado inválido: {task.get('status')}")
            if task.get("priority") not in VALID_PRIORITIES:
                err(f"Tarea {tid} con prioridad inválida: {task.get('priority')}")
            if not isinstance(task.get("countsForProgress"), bool):
                err(f"Tarea {tid}: countsForProgress debe ser booleano.")
            if task.get("status") == "COMPLETADO":
                if not task.get("acceptanceCriteria"):
                    err(f"Tarea {tid} COMPLETADO sin criterios de aceptación.")
                if not task.get("tests"):
                    err(f"Tarea {tid} COMPLETADO sin pruebas.")
                if not task.get("evidence"):
                    warn(f"Tarea {tid} COMPLETADO sin evidencias registradas.")
            for did in task.get("decisionIds", []):
                if did not in decision_ids:
                    err(f"Tarea {tid} referencia decisión inexistente: {did}")
            for related in task.get("relatedTaskIds", []):
                if related not in by_id:
                    err(f"Tarea {tid} referencia tarea histórica inexistente: {related}")

        # Una propuesta no aprobada no puede tener trabajo activo ni completado.
        if upd.get("status") in {"PROPUESTA", "EN_REVISION", "PENDIENTE_APROBACION"}:
            active = [t.get("id") for t in upd["tasks"] if t.get("status") in {"EN_PROGRESO", "COMPLETADO"}]
            if active:
                err(
                    f"Actualización {uid} en {upd.get('status')} tiene tareas activas o "
                    f"completadas sin aprobación: {active}"
                )

        # Una actualización completada debe tener sus tareas de progreso completadas.
        if upd.get("status") == "COMPLETADA":
            pending = [
                t.get("id")
                for t in upd["tasks"]
                if t.get("category") == "implementation"
                and t.get("countsForProgress") is True
                and t.get("status") != "COMPLETADO"
            ]
            if pending:
                err(f"Actualización {uid} COMPLETADA con tareas de progreso sin completar: {pending}")

    # Grupos de trabajo opcionales de cada actualización. No son etapas del
    # plan base y solo pueden agrupar tareas de su propia actualización.
    for upd in updates:
        uid = upd.get("id")
        if not uid:
            continue
        groups = upd.get("groups")
        if groups is None:
            continue
        if not isinstance(groups, list):
            err(f"Actualización {uid}: groups debe ser una lista.")
            continue
        group_ids: set[str] = set()
        for group in groups:
            if not isinstance(group, dict):
                err(f"Actualización {uid}: cada grupo debe ser un objeto.")
                continue
            gid = group.get("id")
            if not gid:
                err(f"Actualización {uid}: grupo sin id.")
                continue
            if gid in group_ids:
                err(f"Actualización {uid}: grupo duplicado {gid}.")
            group_ids.add(gid)
            if not group.get("name"):
                err(f"Actualización {uid}: grupo {gid} sin nombre.")
            if group.get("status") is not None and group.get("status") not in VALID_STATUS:
                err(f"Actualización {uid}: grupo {gid} con estado inválido: {group.get('status')}")
            task_ids = group.get("taskIds", [])
            if not isinstance(task_ids, list):
                err(f"Actualización {uid}: grupo {gid} taskIds debe ser una lista.")
                task_ids = []
            for tid in task_ids:
                if tid not in update_task_by_id:
                    err(f"Actualización {uid}: grupo {gid} referencia tarea inexistente: {tid}")
                elif update_task_by_id[tid].get("updateId") != uid:
                    err(f"Actualización {uid}: grupo {gid} referencia tarea de otra actualización: {tid}")
            for dep in group.get("dependencies", []):
                if dep not in all_by_id:
                    err(f"Actualización {uid}: grupo {gid} depende de tarea inexistente: {dep}")
            for test in group.get("tests", []):
                if not isinstance(test, str) or not test.strip():
                    err(f"Actualización {uid}: grupo {gid} contiene una prueba vacía o no textual.")

    # Referencias entre actualizaciones (segunda pasada; permite referencias hacia delante).
    for upd in updates:
        uid = upd.get("id")
        if not uid:
            continue
        for rid in upd.get("relatedUpdates", []):
            if rid not in update_ids:
                err(f"Actualización {uid} referencia actualización inexistente: {rid}")
        for rid in upd.get("dependencies", []):
            if rid == uid:
                err(f"Actualización {uid} depende de sí misma.")
            if rid not in update_ids:
                err(f"Actualización {uid} depende de actualización inexistente: {rid}")

    # Ciclos entre actualizaciones
    visiting_u: set[str] = set()
    visited_u: set[str] = set()

    def dfs_update(uid: str) -> None:
        if uid in visited_u:
            return
        if uid in visiting_u:
            err(f"Ciclo de dependencias entre actualizaciones detectado en {uid}.")
            return
        visiting_u.add(uid)
        for dep in update_by_id[uid].get("dependencies", []):
            if dep in update_by_id:
                dfs_update(dep)
        visiting_u.remove(uid)
        visited_u.add(uid)

    for uid in update_by_id:
        dfs_update(uid)

    # Dependencias y ciclos de las tareas de actualización
    for tid, task in update_task_by_id.items():
        for dep in task.get("dependencies", []):
            if dep == tid:
                err(f"Tarea de actualización {tid} depende de sí misma.")
            elif dep not in all_by_id:
                err(f"Tarea de actualización {tid} depende de tarea inexistente: {dep}")

    visiting_t: set[str] = set()
    visited_t: set[str] = set()

    def dfs_update_task(tid: str) -> None:
        if tid in visited_t:
            return
        if tid in visiting_t:
            err(f"Ciclo de dependencias detectado en tarea de actualización {tid}.")
            return
        visiting_t.add(tid)
        for dep in update_task_by_id[tid].get("dependencies", []):
            if dep in update_task_by_id:
                dfs_update_task(dep)
        visiting_t.remove(tid)
        visited_t.add(tid)

    for tid in update_task_by_id:
        dfs_update_task(tid)

    # Catálogo de revisión
    catalog = tracker.get("reviewCatalog", {})
    system_ids: set[str] = set()
    feature_ids: set[str] = set()
    if catalog and not isinstance(catalog, dict):
        err("reviewCatalog debe ser un objeto.")
        catalog = {}
    for system in catalog.get("systems", []):
        sid = system.get("id")
        if not sid:
            err("Sistema de revisión sin id.")
            continue
        if sid in system_ids:
            err(f"Sistema de revisión duplicado: {sid}")
        system_ids.add(sid)
        for field in ("name", "description"):
            if not system.get(field):
                err(f"Sistema {sid} sin {field}.")
        for stage_id in system.get("stageIds", []):
            if stage_id not in stage_ids:
                err(f"Sistema {sid} referencia etapa inexistente: {stage_id}")
        for tid in system.get("taskIds", []):
            if tid not in all_by_id:
                err(f"Sistema {sid} referencia tarea inexistente: {tid}")
        for uid in system.get("updateIds", []):
            if uid not in update_ids:
                err(f"Sistema {sid} referencia actualización inexistente: {uid}")

    for feat in catalog.get("features", []):
        fid = feat.get("id")
        if not fid:
            err("Funcionalidad de revisión sin id.")
            continue
        if fid in feature_ids:
            err(f"Funcionalidad de revisión duplicada: {fid}")
        feature_ids.add(fid)
        for field in ("name", "description"):
            if not feat.get(field):
                err(f"Funcionalidad {fid} sin {field}.")
        for stage_id in feat.get("stageIds", []):
            if stage_id not in stage_ids:
                err(f"Funcionalidad {fid} referencia etapa inexistente: {stage_id}")
        for tid in feat.get("taskIds", []):
            if tid not in all_by_id:
                err(f"Funcionalidad {fid} referencia tarea inexistente: {tid}")
        for sid in feat.get("systemIds", []):
            if sid not in system_ids:
                err(f"Funcionalidad {fid} referencia sistema inexistente: {sid}")

    for upd in updates:
        uid = upd.get("id")
        if not uid:
            continue
        for sid in upd.get("affectedSystems", []):
            if sid not in system_ids:
                err(f"Actualización {uid} referencia sistema inexistente: {sid}")
        for fid in upd.get("affectedFeatures", []):
            if fid not in feature_ids:
                err(f"Actualización {uid} referencia funcionalidad inexistente: {fid}")

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
            if tid not in all_by_id:
                err(f"Sesión {sid} referencia tarea inexistente: {tid}")
        document = session.get("document")
        if document:
            doc_path = (ROOT / document).resolve()
            try:
                doc_path.relative_to(ROOT)
            except ValueError:
                err(f"Sesión {sid}: document apunta fuera del proyecto: {document}")
            else:
                if not document.startswith("docs/sessions/"):
                    warn(f"Sesión {sid}: document no está en docs/sessions/: {document}")
                if not doc_path.exists():
                    warn(f"Sesión {sid}: no existe el documento {document}")

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
            if tid not in all_by_id:
                err(f"Incidencia {iid} afecta a tarea inexistente: {tid}")

    # Decisiones que afectan a actualizaciones
    for decision in decisions:
        did = decision.get("id")
        for uid in decision.get("affectedUpdates", []):
            if uid not in update_ids:
                err(f"Decisión {did} afecta a actualización inexistente: {uid}")

    # Versiones de presentación
    initial_version = meta.get("initialVersion")
    initial_display_version = None
    if initial_version is not None:
        if not isinstance(initial_version, dict):
            err("meta.initialVersion debe ser un objeto.")
        else:
            initial_display_version = initial_version.get("displayVersion")
            if initial_display_version is None:
                warn("meta.initialVersion no define displayVersion; el panel usará 0.1.")
            elif not isinstance(initial_display_version, str) or not VERSION_RE.fullmatch(initial_display_version):
                err(f"meta.initialVersion.displayVersion inválido: {initial_display_version!r}")
            for field in ("name", "description", "objective", "date"):
                if field in initial_version and not isinstance(initial_version[field], str):
                    err(f"meta.initialVersion.{field} debe ser texto.")
            if initial_version.get("id") and not isinstance(initial_version.get("id"), str):
                err("meta.initialVersion.id debe ser texto.")

    if initial_display_version and initial_display_version in display_versions:
        err(
            "displayVersion duplicada entre la versión inicial y "
            f"{display_versions[initial_display_version]}: {initial_display_version}"
        )

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

    current_update = meta.get("currentUpdateId")
    if current_update and current_update not in update_ids:
        err(f"meta.currentUpdateId apunta a actualización inexistente: {current_update}")
    next_update = meta.get("nextUpdateId")
    if next_update and next_update not in update_ids:
        err(f"meta.nextUpdateId apunta a actualización inexistente: {next_update}")
    if next_update and next_update in update_by_id:
        nus = update_by_id[next_update].get("status")
        if nus in {"COMPLETADA", "RECHAZADA", "ARCHIVADA"}:
            warn(f"meta.nextUpdateId {next_update} está {nus}.")

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

    # Estado de tarea en progreso (plan base y actualizaciones).
    # El proyecto admite varias sesiones simultáneas: se permite más de una
    # tarea EN_PROGRESO si cada una está asignada a una sesión distinta. Si dos
    # tareas comparten responsable, es un error de coordinación.
    in_progress = [t for t in tasks if t.get("status") == "EN_PROGRESO"]
    if in_progress:
        owners: dict[str, str] = {}
        for task in in_progress:
            owner = task.get("assignedSessionId")
            if not owner:
                warn(
                    f"Tarea {task.get('id')} EN_PROGRESO sin assignedSessionId; "
                    "usa tools/continuity.py claim."
                )
                continue
            if owner in owners:
                err(
                    f"Dos tareas EN_PROGRESO comparten la sesión {owner}: "
                    f"{owners[owner]} y {task.get('id')}."
                )
            owners[owner] = task.get("id")
    if in_progress and current_task and current_task != in_progress[-1].get("id"):
        warn("meta.currentTaskId no coincide con la última tarea EN_PROGRESO del plan base.")

    update_in_progress = [t for t in update_task_by_id.values() if t.get("status") == "EN_PROGRESO"]
    if update_in_progress:
        owners = {}
        for task in update_in_progress:
            owner = task.get("assignedSessionId")
            if not owner:
                warn(
                    f"Tarea {task.get('id')} EN_PROGRESO sin assignedSessionId; "
                    "usa tools/continuity.py claim."
                )
                continue
            if owner in owners:
                err(
                    f"Dos tareas EN_PROGRESO comparten la sesión {owner}: "
                    f"{owners[owner]} y {task.get('id')}."
                )
            owners[owner] = task.get("id")
    if update_in_progress and in_progress:
        warn(
            "Hay trabajo simultáneo en el plan base y en una actualización; "
            "es válido si las sesiones son distintas."
        )


def progress(tracker: dict) -> tuple[int, int, int]:
    tasks = tracker.get("tasks", [])
    impl = [t for t in tasks if t.get("category") == "implementation" and t.get("countsForProgress") is True]
    done = [t for t in impl if t.get("status") == "COMPLETADO"]
    return len(done), len(impl), (round(100 * len(done) / len(impl)) if impl else 0)


def update_progress(tracker: dict) -> list[tuple[str, int, int, int]]:
    rows = []
    for upd in tracker.get("updates", []):
        tasks = upd.get("tasks", [])
        impl = [t for t in tasks if t.get("category") == "implementation" and t.get("countsForProgress") is True]
        done = [t for t in impl if t.get("status") == "COMPLETADO"]
        pct = round(100 * len(done) / len(impl)) if impl else 0
        rows.append((upd.get("id", "?"), len(done), len(impl), pct))
    return rows


def _md(value: object) -> str:
    """Escapa un valor para una celda Markdown de una sola línea."""
    return str(value if value is not None else "").replace("|", "\\|").replace("\n", " ").strip()


def _doc_href(document: object) -> str:
    """Convierte docs/sessions/S-XXX.md en una ruta relativa a docs/."""
    text = str(document or "")
    return text[5:] if text.startswith("docs/") else text


def _task_ref(task: dict | None) -> str:
    if not task:
        return "Ninguna"
    return f"`{_md(task.get('id'))}` — {_md(task.get('title'))}"


def _next_available(tracker: dict, by_id: dict[str, dict]) -> dict | None:
    """Devuelve meta.nextTaskId o la primera tarea LISTO con dependencias completas."""
    meta = tracker.get("meta", {})
    next_id = meta.get("nextTaskId")
    if next_id and next_id in by_id:
        return by_id[next_id]
    for task in tracker.get("tasks", []):
        if task.get("status") != "LISTO":
            continue
        deps = task.get("dependencies", [])
        if all(by_id.get(dep, {}).get("status") == "COMPLETADO" for dep in deps):
            return task
    return None


def status_markdown(tracker: dict) -> str:
    """Genera el resumen compacto docs/STATUS.md a partir del tracker."""
    meta = tracker.get("meta", {})
    stages = tracker.get("stages", [])
    tasks = tracker.get("tasks", [])
    decisions = tracker.get("decisions", [])
    sessions = tracker.get("sessions", [])
    updates = tracker.get("updates", [])
    by_id = {task.get("id"): task for task in tasks if task.get("id")}
    stage_by_id = {stage.get("id"): stage for stage in stages if stage.get("id")}

    counts: dict[str, int] = {}
    for task in tasks:
        status = task.get("status", "?")
        counts[status] = counts.get(status, 0) + 1

    current_stage_id = meta.get("currentStageId")
    current_stage = stage_by_id.get(current_stage_id)
    if current_stage:
        stage_text = f"`{_md(current_stage_id)}` — {_md(current_stage.get('name'))} ({_md(current_stage.get('status'))})"
    else:
        stage_text = "Ninguna"

    current_task = by_id.get(meta.get("currentTaskId"))
    if not current_task:
        current_task = next((t for t in tasks if t.get("status") == "EN_PROGRESO"), None)
    next_task = _next_available(tracker, by_id)

    done, total, pct = progress(tracker)
    pending_decisions = [d for d in decisions if d.get("status") == "PENDIENTE"]
    pending_ids = ", ".join(f"`{_md(d.get('id'))}`" for d in pending_decisions) or "ninguna"

    current_update = None
    current_update_id = meta.get("currentUpdateId")
    if current_update_id:
        current_update = next((u for u in updates if u.get("id") == current_update_id), None)
    if not current_update and updates:
        current_update = updates[-1]
    if current_update:
        upd_done = sum(
            1 for t in current_update.get("tasks", [])
            if t.get("category") == "implementation"
            and t.get("countsForProgress") is True
            and t.get("status") == "COMPLETADO"
        )
        upd_total = sum(
            1 for t in current_update.get("tasks", [])
            if t.get("category") == "implementation" and t.get("countsForProgress") is True
        )
        update_text = (
            f"`{_md(current_update.get('id'))}` — {_md(current_update.get('name'))} "
            f"({_md(current_update.get('status'))}, {upd_done}/{upd_total})"
        )
    else:
        update_text = "Ninguna"

    def session_number(session: dict) -> int:
        match = re.fullmatch(r"S-(\d+)", str(session.get("id", "")))
        return int(match.group(1)) if match else -1

    recent_sessions = sorted(
        sessions,
        key=lambda s: (session_number(s), s.get("date", "")),
        reverse=True,
    )[:3]
    if recent_sessions:
        session_lines = "\n".join(
            f"- `{_md(s.get('id'))}` · {_md(s.get('date'))} · {_md(s.get('title'))}"
            + (f" · [registro]({_md(_doc_href(s.get('document')))})" if s.get("document") else "")
            for s in recent_sessions
        )
    else:
        session_lines = "- No hay sesiones registradas."

    active_sessions = [s for s in sessions if s.get("outcome") == "EN_PROGRESO"]
    if active_sessions:
        active_lines = "\n".join(
            f"- `{_md(s.get('id'))}` · {_md(s.get('title'))}"
            + (f" · {_md(s.get('agent'))}" if s.get("agent") else "")
            for s in active_sessions
        )
    else:
        active_lines = "- Ninguna."

    context_lines = ""
    if current_task:
        context_files = current_task.get("contextFiles", [])
        context_docs = current_task.get("contextDocs", [])
        context_tests = current_task.get("tests", [])
        if context_files or context_docs:
            context_lines = "\n## Contexto de la tarea actual\n\n"
            if context_files:
                context_lines += "**Código:** " + ", ".join(
                    f"`{_md(item)}`" for item in context_files
                ) + "\n\n"
            if context_docs:
                context_lines += "**Documentación:** " + ", ".join(
                    f"`{_md(item)}`" for item in context_docs
                ) + "\n\n"
            if context_tests:
                context_lines += "**Pruebas:** " + ", ".join(
                    f"`{_md(item)}`" for item in context_tests
                ) + "\n"

    update_lines = ""
    if updates:
        rows = {uid: (done_u, total_u, pct_u) for uid, done_u, total_u, pct_u in update_progress(tracker)}
        update_lines = "\n".join(
            f"- `{_md(upd.get('id'))}` · v{_md(upd.get('displayVersion') or '?')} · {_md(upd.get('name'))} · "
            f"{_md(upd.get('status'))} · {rows.get(upd.get('id'), (0, 0, 0))[0]}/"
            f"{rows.get(upd.get('id'), (0, 0, 0))[1]} "
            f"({rows.get(upd.get('id'), (0, 0, 0))[2]} %)"
            for upd in updates
        )
    else:
        update_lines = "- No hay actualizaciones registradas."

    return f"""# Estado del proyecto — {_md(meta.get('displayName', 'GalacticIndustry'))}

> Generado automáticamente desde `docs/IMPLEMENTATION_TRACKER.json` por
> `tools/sync-dashboard.py`. **No editar a mano.** El tracker es la fuente
> canónica; este archivo solo resume su estado.

| Campo | Valor |
|---|---|
| Última actualización | {_md(meta.get('lastUpdated'))} |
| Última sesión cerrada | {_md(meta.get('lastSessionId'))} |
| Etapa actual | {stage_text} |
| Tarea actual | {_task_ref(current_task)} |
| Próxima tarea disponible | {_task_ref(next_task)} |
| Progreso de implementación | {done}/{total} ({pct} %) |
| Tareas completadas | {counts.get('COMPLETADO', 0)} |
| Tareas pendientes | {counts.get('PENDIENTE', 0)} |
| Tareas listas para empezar | {counts.get('LISTO', 0)} |
| Tareas en progreso | {counts.get('EN_PROGRESO', 0)} |
| Tareas bloqueadas | {counts.get('BLOQUEADO', 0)} |
| Tareas pospuestas | {counts.get('POSPUESTO', 0)} |
| Tareas que requieren decisión | {counts.get('REQUIERE_DECISION', 0)} |
| Decisiones pendientes | {len(pending_decisions)} ({pending_ids}) |
| Versión inicial | v{_md((meta.get('initialVersion') or {}).get('displayVersion', '0.1'))} — {_md((meta.get('initialVersion') or {}).get('name', 'Implementación inicial'))} |
| Actualización actual | {update_text} |
{context_lines}
## Sesiones activas

{active_lines}

## Actualizaciones registradas

{update_lines}

## Últimas sesiones

{session_lines}

## Referencias

- Fichas de sistemas: [docs/systems/](systems/)
- Plan maestro: [docs/MASTER_PLAN.md](MASTER_PLAN.md)
- Índice de sistemas: [docs/INDEX.md](INDEX.md)
- Tracker canónico: [docs/IMPLEMENTATION_TRACKER.json](IMPLEMENTATION_TRACKER.json)
- Decisiones: [docs/DECISIONS.md](DECISIONS.md)
- Sesiones: [docs/sessions/](sessions/)
- Log histórico: [docs/IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md)
- Panel: [docs/dashboard/index.html](dashboard/index.html)
- Coordinación: [docs/CONCURRENCY.md](CONCURRENCY.md)
"""


def write_text_atomic(path: Path, text: str) -> None:
    """Escribe de forma atómica; delega en continuity_io."""
    cio.atomic_write_text(path, text)


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
    if not INDEX.exists():
        warn(f"No existe {INDEX}; se recomienda mantener el índice de sistemas.")
    html = DASHBOARD.read_text(encoding="utf-8")
    if START not in html or END not in html:
        err(f"El panel no contiene los marcadores {START} / {END}.")
        return False
    block = dashboard_block(tracker)
    pattern = re.compile(re.escape(START) + r".*?" + re.escape(END), re.S)
    new_html = pattern.sub(lambda _: block, html, count=1)
    status_text = status_markdown(tracker)
    if check_only:
        embedded = embedded_tracker(html)
        if embedded is None:
            err("No se pudo leer el bloque tracker-data del HTML.")
            return False
        if embedded != tracker:
            err("El bloque tracker-data del HTML no coincide con el JSON canónico.")
            return False
        if not STATUS.exists():
            err(f"No existe {STATUS}; ejecuta el sincronizador sin --check para generarlo.")
            return False
        if STATUS.read_text(encoding="utf-8") != status_text:
            err("docs/STATUS.md no coincide con el tracker; ejecuta el sincronizador sin --check.")
            return False
        return True
    write_text_atomic(DASHBOARD, new_html)
    write_text_atomic(STATUS, status_text)
    return True


def summary(tracker: dict) -> str:
    tasks = tracker.get("tasks", [])
    done, total, pct = progress(tracker)
    counts: dict[str, int] = {}
    for task in tasks:
        counts[task.get("status", "?")] = counts.get(task.get("status", "?"), 0) + 1
    stages = tracker.get("stages", [])
    updates = tracker.get("updates", [])
    upd_lines = "".join(
        f"  {uid}: {done}/{total} ({pct}%)\n"
        for uid, done, total, pct in update_progress(tracker)
    )
    return (
        f"Proyecto: {tracker.get('meta', {}).get('displayName', 'GalacticIndustry')}\n"
        f"Etapas: {len(stages)} | Tareas base: {len(tasks)}\n"
        f"Implementación del plan base: {done}/{total} ({pct}%)\n"
        f"Actualizaciones: {len(updates)}\n"
        + (upd_lines if upd_lines else "")
        + f"Estados: " + ", ".join(f"{k}={v}" for k, v in sorted(counts.items())) + "\n"
        f"Siguiente tarea: {tracker.get('meta', {}).get('nextTaskId')}\n"
        f"Actualización actual: {tracker.get('meta', {}).get('currentUpdateId')}\n"
        f"Resumen: docs/STATUS.md | Índice: docs/INDEX.md"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida y sincroniza el panel de continuidad.")
    parser.add_argument("--check", action="store_true", help="No escribe; comprueba HTML y STATUS.md.")
    parser.add_argument("--quiet", action="store_true", help="No imprime el resumen.")
    parser.add_argument("--timeout", type=float, default=None, help="Segundos de espera por el bloqueo.")
    args = parser.parse_args()

    try:
        # Un único bloqueo breve cubre lectura + validación + escritura. Así una
        # sincronización antigua no puede pisar un tracker más reciente y dos
        # sincronizaciones simultáneas producen el mismo resultado.
        with cio.project_lock(ROOT, timeout=args.timeout):
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
    except cio.LockTimeout as exc:
        print(f"BLOQUEO: {exc}", file=sys.stderr)
        return 4
    except cio.LockUnavailable as exc:
        print(f"PLATAFORMA: {exc}", file=sys.stderr)
        return 5

    if not args.quiet:
        if warnings:
            print("AVISOS:")
            for w in warnings:
                print(f"  - {w}")
        print(summary(tracker))
        print("OK: tracker válido y panel sincronizado." if not args.check else "OK: tracker válido, HTML y STATUS.md coinciden.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
