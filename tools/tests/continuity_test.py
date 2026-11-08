#!/usr/bin/env python3
"""Pruebas de concurrencia de la continuidad de GalacticIndustry.

Se ejecutan en un sandbox temporal: copian el tracker, el panel y las
herramientas, y nunca modifican el proyecto real.

Uso:
    python3 tools/tests/continuity_test.py
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
import textwrap
import time
import unittest
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[2]
COPY_FILES = [
    "tools/continuity.py",
    "tools/continuity_io.py",
    "tools/sync-dashboard.py",
    "docs/IMPLEMENTATION_TRACKER.json",
    "docs/INDEX.md",
    "docs/STATUS.md",
    "docs/dashboard/index.html",
    "docs/dashboard/dashboard.css",
    "docs/dashboard/dashboard.js",
    "docs/sessions/README.md",
]
COPY_GLOBS = ["docs/updates/*.md", "docs/systems/*.md"]


class ContinuityTest(unittest.TestCase):
    def setUp(self) -> None:
        base = Path("/tmp/opencode")
        base.mkdir(parents=True, exist_ok=True)
        self.root = Path(tempfile.mkdtemp(prefix="gi-cont-", dir=str(base)))
        for rel in COPY_FILES:
            self._copy(rel)
        for pattern in COPY_GLOBS:
            for path in PROJECT.glob(pattern):
                self._copy(str(path.relative_to(PROJECT)))
        (self.root / "docs" / "sessions").mkdir(parents=True, exist_ok=True)
        (self.root / "docs" / "systems").mkdir(parents=True, exist_ok=True)
        self.env = dict(os.environ)
        self.env["GI_PROJECT_ROOT"] = str(self.root)
        self.env["GI_LOCK_TIMEOUT"] = "10"
        # Deja los derivados coherentes antes de cada prueba.
        result = self.run_tool("sync-dashboard.py", "--quiet")
        self.assertEqual(result.returncode, 0, result.stderr)

    def tearDown(self) -> None:
        shutil.rmtree(self.root, ignore_errors=True)

    def _copy(self, rel: str) -> None:
        src = PROJECT / rel
        dst = self.root / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)

    def run_tool(self, script: str, *args: str, timeout: float = 30.0):
        return subprocess.run(
            [sys.executable, str(self.root / "tools" / script), *args],
            cwd=str(self.root),
            env=self.env,
            capture_output=True,
            text=True,
            timeout=timeout,
        )

    def run_continuity(self, *args: str, timeout: float = 30.0):
        return self.run_tool("continuity.py", *args, timeout=timeout)

    def tracker(self) -> dict:
        return json.loads((self.root / "docs" / "IMPLEMENTATION_TRACKER.json").read_text(encoding="utf-8"))

    def task(self, task_id: str) -> dict:
        return next(t for t in self.tracker()["tasks"] if t["id"] == task_id)

    def plan(self, task_id: str, marker: str, expect: str | None = None) -> dict:
        current = self.task(task_id)
        return {
            "sessionId": "S-900",
            "operations": [
                {
                    "op": "task.update",
                    "id": task_id,
                    "expectUpdatedAt": expect if expect is not None else current["updatedAt"],
                    "set": {"technicalNotes": marker},
                }
            ],
        }

    def write_plan(self, name: str, plan: dict) -> str:
        path = self.root / name
        path.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
        return str(path)

    def start_apply(self, plan_path: str, *extra: str):
        return subprocess.Popen(
            [
                sys.executable,
                str(self.root / "tools" / "continuity.py"),
                "apply",
                plan_path,
                "--no-sync",
                *extra,
            ],
            cwd=str(self.root),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

    # A. Modificaciones independientes -------------------------------------
    def test_a_independent_task_updates_are_preserved(self) -> None:
        plan_a = self.write_plan("a.json", self.plan("T-102", "marca-A"))
        plan_b = self.write_plan("b.json", self.plan("T-103", "marca-B"))
        first = self.start_apply(plan_a)
        second = self.start_apply(plan_b)
        out_a, err_a = first.communicate(timeout=30)
        out_b, err_b = second.communicate(timeout=30)
        self.assertEqual(first.returncode, 0, err_a + out_a)
        self.assertEqual(second.returncode, 0, err_b + out_b)
        self.assertEqual(self.task("T-102")["technicalNotes"], "marca-A")
        self.assertEqual(self.task("T-103")["technicalNotes"], "marca-B")

    # B. Misma tarea, mismos campos ---------------------------------------
    def test_b_same_task_conflict_is_explicit(self) -> None:
        expected = self.task("T-102")["updatedAt"]
        plan_a = self.write_plan("same-a.json", self.plan("T-102", "marca-A", expected))
        plan_b = self.write_plan("same-b.json", self.plan("T-102", "marca-B", expected))
        first = self.start_apply(plan_a)
        second = self.start_apply(plan_b)
        out_a, err_a = first.communicate(timeout=30)
        out_b, err_b = second.communicate(timeout=30)
        codes = sorted([first.returncode, second.returncode])
        self.assertEqual(codes, [0, 2], (err_a, err_b))
        value = self.task("T-102")["technicalNotes"]
        self.assertIn(value, {"marca-A", "marca-B"})
        # No se pierde en silencio: queda exactamente una de las dos marcas.
        self.assertEqual(value.count("marca-"), 1)

    # C. Interrupción durante la escritura ---------------------------------
    def test_c_interrupted_write_keeps_valid_tracker(self) -> None:
        before = (self.root / "docs" / "IMPLEMENTATION_TRACKER.json").read_text(encoding="utf-8")
        plan = self.write_plan("crash.json", self.plan("T-102", "no-debe-escribirse"))
        script = textwrap.dedent(
            f"""
            import json, os, sys
            sys.path.insert(0, {str(self.root / 'tools')!r})
            import continuity
            import continuity_io as cio
            cio.os.replace = lambda src, dst: os._exit(9)
            plan = json.load(open({plan!r}, encoding="utf-8"))
            continuity.apply_plan(plan, sync=False, base={str(self.root)!r})
            """
        )
        proc = subprocess.run(
            [sys.executable, "-c", script],
            cwd=str(self.root),
            env=self.env,
            capture_output=True,
            text=True,
            timeout=30,
        )
        self.assertEqual(proc.returncode, 9, proc.stderr)
        after = (self.root / "docs" / "IMPLEMENTATION_TRACKER.json").read_text(encoding="utf-8")
        self.assertEqual(before, after)
        json.loads(after)  # sigue siendo JSON válido
        # El bloqueo se liberó al morir el proceso.
        self.assertEqual(self.run_continuity("lock-status").returncode, 0)

    # D. Bloqueos ----------------------------------------------------------
    def test_d_lock_blocks_times_out_and_is_released(self) -> None:
        holder = self._start_lock_holder(seconds=3)
        try:
            line = holder.stdout.readline().strip()
            self.assertEqual(line, "locked")
            busy = self.run_continuity("lock-status")
            self.assertEqual(busy.returncode, 1, busy.stdout + busy.stderr)
            self.assertIn("ocupado", busy.stdout)
            plan = self.write_plan("locked.json", self.plan("T-102", "tras-bloqueo"))
            timeout = self.run_continuity("apply", plan, "--no-sync", "--timeout", "0.2")
            self.assertEqual(timeout.returncode, 4, timeout.stderr)
            holder.wait(timeout=10)
            free = self.run_continuity("lock-status")
            self.assertEqual(free.returncode, 0, free.stdout + free.stderr)
            self.assertIn("libre", free.stdout)
        finally:
            if holder.poll() is None:
                holder.kill()
                holder.wait()
            for stream in (holder.stdout, holder.stderr):
                if stream:
                    stream.close()

    def test_d2_killed_process_releases_lock(self) -> None:
        holder = self._start_lock_holder(seconds=30)
        try:
            self.assertEqual(holder.stdout.readline().strip(), "locked")
            holder.kill()
            holder.wait(timeout=10)
            free = self.run_continuity("lock-status")
            self.assertEqual(free.returncode, 0, free.stderr)
        finally:
            if holder.poll() is None:
                holder.kill()
                holder.wait()
            for stream in (holder.stdout, holder.stderr):
                if stream:
                    stream.close()

    # N. lock-status detecta lectores compartidos ------------------------
    def test_n_lock_status_detects_shared_reader(self) -> None:
        holder = self._start_lock_holder(seconds=2, shared=True)
        try:
            self.assertEqual(holder.stdout.readline().strip(), "locked")
            busy = self.run_continuity("lock-status")
            self.assertEqual(busy.returncode, 1, busy.stdout + busy.stderr)
            self.assertIn("ocupado", busy.stdout)
            holder.wait(timeout=10)
            free = self.run_continuity("lock-status")
            self.assertEqual(free.returncode, 0, free.stdout + free.stderr)
        finally:
            if holder.poll() is None:
                holder.kill()
                holder.wait()
            for stream in (holder.stdout, holder.stderr):
                if stream:
                    stream.close()

    def _start_lock_holder(self, seconds: int, shared: bool = False):
        script = textwrap.dedent(
            f"""
            import sys, time
            sys.path.insert(0, {str(self.root / 'tools')!r})
            import continuity_io as cio
            with cio.project_lock({str(self.root)!r}, timeout=5, shared={shared!r}):
                print("locked", flush=True)
                time.sleep({seconds})
            """
        )
        return subprocess.Popen(
            [sys.executable, "-c", script],
            cwd=str(self.root),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

    # E. Sincronización concurrente ---------------------------------------
    def test_e_concurrent_sync_is_coherent(self) -> None:
        plan = self.write_plan("sync.json", self.plan("T-102", "sync-concurrente"))
        applied = self.run_continuity("apply", plan, "--no-sync")
        self.assertEqual(applied.returncode, 0, applied.stderr)
        first = subprocess.Popen(
            [sys.executable, str(self.root / "tools" / "sync-dashboard.py"), "--quiet"],
            cwd=str(self.root),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        second = subprocess.Popen(
            [sys.executable, str(self.root / "tools" / "sync-dashboard.py"), "--quiet"],
            cwd=str(self.root),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        out_a, err_a = first.communicate(timeout=30)
        out_b, err_b = second.communicate(timeout=30)
        self.assertEqual(first.returncode, 0, err_a + out_a)
        self.assertEqual(second.returncode, 0, err_b + out_b)
        check = self.run_tool("sync-dashboard.py", "--check", "--quiet")
        self.assertEqual(check.returncode, 0, check.stderr)
        embedded = (self.root / "docs" / "dashboard" / "index.html").read_text(encoding="utf-8")
        self.assertIn('"technicalNotes": "sync-concurrente"', embedded)

    # F. Conservación del trabajo ajeno ------------------------------------
    def test_f_foreign_changes_are_preserved(self) -> None:
        foreign = self.root / "docs" / "FOREIGN.md"
        foreign.write_text("trabajo de otra sesión\n", encoding="utf-8")
        plan_foreign = self.write_plan("foreign.json", self.plan("T-103", "marca-ajena"))
        plan_own = self.write_plan("own.json", self.plan("T-102", "marca-propia"))
        self.assertEqual(self.run_continuity("apply", plan_foreign, "--no-sync").returncode, 0)
        self.assertEqual(self.run_continuity("apply", plan_own, "--no-sync").returncode, 0)
        self.assertEqual(self.task("T-103")["technicalNotes"], "marca-ajena")
        self.assertEqual(self.task("T-102")["technicalNotes"], "marca-propia")
        self.assertEqual(foreign.read_text(encoding="utf-8"), "trabajo de otra sesión\n")
        self.assertEqual(self.run_tool("sync-dashboard.py", "--quiet").returncode, 0)
        self.assertEqual(foreign.read_text(encoding="utf-8"), "trabajo de otra sesión\n")

    # G. Reserva de tarea --------------------------------------------------
    def test_g_claim_prevents_duplicate_work(self) -> None:
        # El plan base está cerrado; el sandbox prepara una tarea reabrible.
        tracker = self.tracker()
        task = next(t for t in tracker["tasks"] if t["id"] == "T-102")
        task["status"] = "LISTO"
        task.pop("assignedSessionId", None)
        task.pop("assignedAt", None)
        (self.root / "docs" / "IMPLEMENTATION_TRACKER.json").write_text(
            json.dumps(tracker, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        first = self.run_continuity(
            "claim", "T-102", "--session", "S-900", "--agent", "test", "--no-sync"
        )
        self.assertEqual(first.returncode, 0, first.stderr)
        task = self.task("T-102")
        self.assertEqual(task["status"], "EN_PROGRESO")
        self.assertEqual(task["assignedSessionId"], "S-900")
        second = self.run_continuity(
            "claim", "T-102", "--session", "S-901", "--agent", "test", "--no-sync"
        )
        self.assertEqual(second.returncode, 2, second.stderr)
        self.assertIn("CONFLICTO", second.stderr)

    # H. IDs de sesión únicos bajo concurrencia ----------------------------
    def test_h_new_session_ids_are_unique(self) -> None:
        procs = [
            subprocess.Popen(
                [
                    sys.executable,
                    str(self.root / "tools" / "continuity.py"),
                    "new-session",
                    "--agent",
                    "test",
                    "--title",
                    f"sesión {index}",
                ],
                cwd=str(self.root),
                env=self.env,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            for index in (1, 2)
        ]
        ids = []
        for proc in procs:
            out, err = proc.communicate(timeout=30)
            self.assertEqual(proc.returncode, 0, err)
            ids.append(out.splitlines()[0].strip())
        self.assertEqual(len(set(ids)), 2, ids)
        for session_id in ids:
            self.assertTrue((self.root / "docs" / "sessions" / f"{session_id}.md").exists())


    # I. Reemplazo seguro de documentos compartidos ------------------------
    def test_i_safe_replace_requires_expected_count(self) -> None:
        path = self.root / "docs" / "EDIT.md"
        path.write_text("A\nB\nA\n", encoding="utf-8")
        ok = self.run_continuity(
            "replace", "docs/EDIT.md", "--old", "A", "--new", "X", "--count", "2"
        )
        self.assertEqual(ok.returncode, 0, ok.stderr)
        self.assertEqual(path.read_text(encoding="utf-8"), "X\nB\nX\n")
        before = path.read_text(encoding="utf-8")
        conflict = self.run_continuity(
            "replace", "docs/EDIT.md", "--old", "X", "--new", "Y", "--count", "1"
        )
        self.assertEqual(conflict.returncode, 2, conflict.stderr)
        self.assertEqual(path.read_text(encoding="utf-8"), before)


    # J. Dependencias entre tareas de actualización ------------------------
    def test_j_claim_resolves_update_task_dependencies(self) -> None:
        tracker = self.tracker()
        upd = next(u for u in tracker["updates"] if u["id"] == "UPD-002")
        upd["status"] = "EN_PROGRESO"
        first = next(t for t in upd["tasks"] if t["id"] == "UPD-002-T01")
        second = next(t for t in upd["tasks"] if t["id"] == "UPD-002-T02")
        second["status"] = "LISTO"
        second["dependencies"] = ["UPD-002-T01"]
        second.pop("assignedSessionId", None)
        second.pop("assignedAt", None)
        (self.root / "docs" / "IMPLEMENTATION_TRACKER.json").write_text(
            json.dumps(tracker, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )

        # Con la dependencia completada, la tarea de actualización se reclama.
        ok = self.run_continuity(
            "claim", "UPD-002-T02", "--session", "S-900", "--agent", "test", "--no-sync"
        )
        self.assertEqual(ok.returncode, 0, ok.stderr)
        claimed_upd = next(u for u in self.tracker()["updates"] if u["id"] == "UPD-002")
        claimed = next(t for t in claimed_upd["tasks"] if t["id"] == "UPD-002-T02")
        self.assertEqual(claimed["assignedSessionId"], "S-900")

        # Si la dependencia vuelve a estar pendiente, el claim se rechaza.
        tracker = self.tracker()
        upd = next(u for u in tracker["updates"] if u["id"] == "UPD-002")
        first = next(t for t in upd["tasks"] if t["id"] == "UPD-002-T01")
        first["status"] = "PENDIENTE"
        second = next(t for t in upd["tasks"] if t["id"] == "UPD-002-T02")
        second["status"] = "LISTO"
        second.pop("assignedSessionId", None)
        (self.root / "docs" / "IMPLEMENTATION_TRACKER.json").write_text(
            json.dumps(tracker, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        blocked = self.run_continuity(
            "claim", "UPD-002-T02", "--session", "S-901", "--agent", "test", "--no-sync"
        )
        self.assertEqual(blocked.returncode, 1, blocked.stderr)
        self.assertIn("dependencias sin completar", blocked.stderr)

    # K. La sesión nueva aparece como activa -------------------------------
    def test_k_new_session_is_registered_as_active(self) -> None:
        created = self.run_continuity(
            "new-session", "--agent", "test", "--title", "sesión activa"
        )
        self.assertEqual(created.returncode, 0, created.stderr)
        session_id = created.stdout.splitlines()[0].strip()
        session = next(s for s in self.tracker()["sessions"] if s["id"] == session_id)
        self.assertEqual(session["outcome"], "EN_PROGRESO")
        self.assertEqual(session["document"], f"docs/sessions/{session_id}.md")
        self.assertEqual(self.run_tool("sync-dashboard.py", "--quiet").returncode, 0)
        status = (self.root / "docs" / "STATUS.md").read_text(encoding="utf-8")
        self.assertIn(session_id, status)
        self.assertIn("## Sesiones activas", status)
        status_section = status.split("## Sesiones activas", 1)[1].split("##", 1)[0]
        self.assertIn(session_id, status_section)

    # L. closeSession cierra la sesión abierta -----------------------------
    def test_l_close_session_marks_outcome(self) -> None:
        created = self.run_continuity(
            "new-session", "--agent", "test", "--title", "sesión a cerrar"
        )
        self.assertEqual(created.returncode, 0, created.stderr)
        session_id = created.stdout.splitlines()[0].strip()
        plan = self.write_plan(
            "close.json",
            {
                "sessionId": session_id,
                "closeSession": True,
                "operations": [{"op": "meta.update", "set": {}}],
            },
        )
        closed = self.run_continuity("apply", plan, "--no-sync")
        self.assertEqual(closed.returncode, 0, closed.stderr)
        tracker = self.tracker()
        session = next(s for s in tracker["sessions"] if s["id"] == session_id)
        self.assertEqual(session["outcome"], "Completado")
        self.assertEqual(tracker["meta"]["lastSessionId"], session_id)


    # M. Catálogo de revisión ----------------------------------------------
    def test_m_catalog_update_links_existing_entries(self) -> None:
        plan = self.write_plan(
            "catalog.json",
            {
                "sessionId": "S-900",
                "operations": [
                    {
                        "op": "catalog.update",
                        "updateId": "UPD-002",
                        "systems": ["SYS-CONTINUIDAD"],
                        "features": ["F-CONTINUIDAD"],
                    }
                ],
            },
        )
        result = self.run_continuity("apply", plan, "--no-sync")
        self.assertEqual(result.returncode, 0, result.stderr)
        catalog = self.tracker()["reviewCatalog"]
        system = next(s for s in catalog["systems"] if s["id"] == "SYS-CONTINUIDAD")
        feature = next(f for f in catalog["features"] if f["id"] == "F-CONTINUIDAD")
        self.assertIn("UPD-002", system["updateIds"])
        self.assertIn("UPD-002", feature["updateIds"])
        # Un identificador inexistente no se inventa.
        bad = self.write_plan(
            "catalog-bad.json",
            {
                "operations": [
                    {
                        "op": "catalog.update",
                        "updateId": "UPD-002",
                        "systems": ["SYS-NO-EXISTE"],
                    }
                ]
            },
        )
        failed = self.run_continuity("apply", bad, "--no-sync")
        self.assertEqual(failed.returncode, 1, failed.stderr)


    # N. displayVersion duplicada ------------------------------------------
    def test_n_duplicate_display_version_is_rejected(self) -> None:
        tracker = self.tracker()
        tracker["updates"][1]["displayVersion"] = tracker["updates"][0]["displayVersion"]
        (self.root / "docs" / "IMPLEMENTATION_TRACKER.json").write_text(
            json.dumps(tracker, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        result = self.run_tool("sync-dashboard.py", "--quiet")
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("displayVersion duplicada", result.stderr)

    # O. Nueva actualización con versión y grupos --------------------------
    def test_o_new_update_with_display_version_and_groups(self) -> None:
        (self.root / "docs" / "updates" / "UPD-900.md").write_text(
            "# UPD-900 — Prueba de versión\n", encoding="utf-8"
        )
        task = {
            "id": "UPD-900-T01",
            "updateId": "UPD-900",
            "title": "Tarea de prueba",
            "description": "Tarea de prueba para grupos.",
            "category": "implementation",
            "priority": "P2",
            "status": "PENDIENTE",
            "countsForProgress": True,
            "dependencies": [],
            "relatedTaskIds": [],
            "decisionIds": [],
            "files": [],
            "acceptanceCriteria": ["Criterio de prueba"],
            "tests": ["Prueba de prueba"],
            "technicalNotes": "",
            "executionLog": [],
            "evidence": [],
            "createdAt": "2026-11-08T15:00:00-06:00",
            "updatedAt": "2026-11-08T15:00:00-06:00",
        }
        value = {
            "id": "UPD-900",
            "displayVersion": "0.7",
            "objective": "Probar la plantilla dinámica.",
            "name": "Actualización de prueba",
            "status": "PROPUESTA",
            "changeType": "MEJORA",
            "priority": "P2",
            "createdAt": "2026-11-08T15:00:00-06:00",
            "updatedAt": "2026-11-08T15:00:00-06:00",
            "description": "Actualización de prueba.",
            "motivation": "Probar la validación de versiones y grupos.",
            "affectedSystems": [],
            "affectedFeatures": [],
            "relatedStages": [],
            "relatedTasks": [],
            "relatedUpdates": [],
            "dependencies": [],
            "document": "docs/updates/UPD-900.md",
            "approval": {
                "required": True,
                "status": "PENDIENTE",
                "approvedBy": None,
                "approvedAt": None,
                "basis": "Prueba.",
            },
            "acceptanceCriteria": ["Criterio de prueba"],
            "tasks": [task],
            "groups": [
                {
                    "id": "G1",
                    "name": "Grupo de prueba",
                    "objective": "Agrupar la tarea.",
                    "description": "Grupo de prueba.",
                    "status": "PENDIENTE",
                    "taskIds": ["UPD-900-T01"],
                    "dependencies": [],
                    "tests": ["Prueba de grupo"],
                }
            ],
            "observations": [],
            "implementationLog": [],
            "reviewExports": [],
        }
        plan = self.write_plan(
            "new-update.json",
            {
                "sessionId": "S-900",
                "operations": [{"op": "update.add", "value": value}],
            },
        )
        result = self.run_continuity("apply", plan, "--no-sync")
        self.assertEqual(result.returncode, 0, result.stderr)
        tracker = self.tracker()
        labels = {u["id"]: u.get("displayVersion") for u in tracker["updates"]}
        self.assertEqual(labels["UPD-001"], "0.2")
        self.assertEqual(labels["UPD-005"], "0.6")
        self.assertEqual(labels["UPD-900"], "0.7")
        self.assertEqual(self.run_tool("sync-dashboard.py", "--quiet").returncode, 0)

        # Un grupo no puede referenciar una tarea inexistente.
        tracker["updates"][-1]["groups"][0]["taskIds"] = ["UPD-900-T99"]
        (self.root / "docs" / "IMPLEMENTATION_TRACKER.json").write_text(
            json.dumps(tracker, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        bad = self.run_tool("sync-dashboard.py", "--quiet")
        self.assertNotEqual(bad.returncode, 0, bad.stdout)
        self.assertIn("referencia tarea inexistente", bad.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
