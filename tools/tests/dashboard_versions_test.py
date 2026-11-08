#!/usr/bin/env python3
"""Prueba de renderizado del panel por versiones.

Delega en `tools/tests/dashboard_versions_test.js`, que ejecuta dashboard.js
en un DOM mínimo de Node y comprueba selector, línea de tiempo, plantilla
dinámica, progreso por versión y estados de actualización.

Uso:
    python3 tools/tests/dashboard_versions_test.py
"""
from __future__ import annotations

import shutil
import subprocess
import unittest
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[2]


class DashboardVersionsTest(unittest.TestCase):
    def test_dashboard_versions_render(self) -> None:
        node = shutil.which("node")
        if not node:
            self.skipTest("Node no está instalado; no se puede ejecutar la prueba de renderizado.")
        result = subprocess.run(
            [node, str(PROJECT / "tools" / "tests" / "dashboard_versions_test.js")],
            cwd=str(PROJECT),
            capture_output=True,
            text=True,
            timeout=60,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
