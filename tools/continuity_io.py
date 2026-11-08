#!/usr/bin/env python3
"""Primitivas de escritura segura para la continuidad de GalacticIndustry.

Este módulo es deliberadamente pequeño: solo resuelve dos problemas.

1. Un bloqueo de sistema operativo (`flock`) compartido por todas las sesiones.
   El bloqueo se libera solo cuando el proceso termina o se interrumpe, así que
   nunca queda un candado permanente por una sesión abandonada.
2. Escrituras atómicas: se escribe un temporal en el mismo directorio, se
   sincroniza a disco y se sustituye el original con `os.replace`.

No es una base de datos ni un servidor. No guarda estado propio: el estado real
sigue siendo `docs/IMPLEMENTATION_TRACKER.json`.

Variables de entorno:
    GI_PROJECT_ROOT   raíz alternativa del proyecto (útil para pruebas).
    GI_LOCK_TIMEOUT   segundos de espera por defecto (30).
"""
from __future__ import annotations

import contextlib
import json
import os
import tempfile
import time
from pathlib import Path

try:  # Linux/macOS
    import fcntl
except ImportError:  # pragma: no cover - el proyecto se construye en Linux
    fcntl = None  # type: ignore[assignment]

LOCK_NAME = ".continuity.lock"
DEFAULT_TIMEOUT = 30.0


class ContinuityError(RuntimeError):
    """Error de continuidad que debe mostrarse sin traza larga."""


class LockTimeout(ContinuityError):
    """No se pudo adquirir el bloqueo dentro del plazo."""


class LockUnavailable(ContinuityError):
    """La plataforma no ofrece bloqueo de archivos."""


def project_root(default: Path | str | None = None) -> Path:
    """Devuelve la raíz del proyecto; `GI_PROJECT_ROOT` tiene prioridad."""
    env = os.environ.get("GI_PROJECT_ROOT")
    if env:
        return Path(env).expanduser().resolve()
    if default is not None:
        return Path(default).expanduser().resolve()
    return Path(__file__).resolve().parent.parent


def lock_path(root: Path | str) -> Path:
    return Path(root).resolve() / "docs" / LOCK_NAME


def timeout_from_env(default: float = DEFAULT_TIMEOUT) -> float:
    try:
        return float(os.environ.get("GI_LOCK_TIMEOUT", default))
    except (TypeError, ValueError):
        return default


@contextlib.contextmanager
def project_lock(
    root: Path | str,
    timeout: float | None = None,
    shared: bool = False,
):
    """Adquiere el bloqueo del proyecto durante un bloque corto.

    Es un `flock` real: cualquier proceso del mismo usuario puede adquirirlo, no
    pertenece a una sesión concreta y el kernel lo libera al cerrar el
    descriptor o al morir el proceso. El archivo `.continuity.lock` puede quedar
    en disco; eso no significa que siga bloqueado.
    """
    if fcntl is None:
        raise LockUnavailable(
            "Esta plataforma no ofrece fcntl.flock; usa un sistema Linux/macOS."
        )
    path = lock_path(root)
    path.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(path, os.O_RDWR | os.O_CREAT, 0o644)
    mode = fcntl.LOCK_SH if shared else fcntl.LOCK_EX
    wait = timeout_from_env() if timeout is None else float(timeout)
    deadline = time.monotonic() + max(0.0, wait)
    try:
        while True:
            try:
                fcntl.flock(fd, mode | fcntl.LOCK_NB)
                break
            except BlockingIOError:
                if time.monotonic() >= deadline:
                    raise LockTimeout(
                        f"Bloqueo ocupado más de {wait:.1f}s: {path}"
                    ) from None
                time.sleep(0.05)
        yield path
    finally:
        try:
            fcntl.flock(fd, fcntl.LOCK_UN)
        finally:
            os.close(fd)


def try_lock(root: Path | str, shared: bool = False) -> bool:
    """Intenta adquirir el bloqueo sin esperar. Devuelve True si lo consiguió.

    Por defecto pide un bloqueo exclusivo: así `lock-status` responde si un
    escritor podría entrar ahora, aunque otra sesión mantenga un bloqueo
    compartido de lectura.
    """
    try:
        with project_lock(root, timeout=0.0, shared=shared):
            return True
    except (LockTimeout, LockUnavailable):
        return False


def _fsync_directory(path: Path) -> None:
    try:
        fd = os.open(path, os.O_RDONLY)
    except OSError:
        return
    try:
        os.fsync(fd)
    except OSError:
        pass
    finally:
        os.close(fd)


def atomic_write_text(path: Path | str, text: str) -> None:
    """Escribe `text` de forma atómica y durable.

    Si el proceso muere antes de `os.replace`, el archivo original queda intacto
    y el temporal se elimina en el `finally` cuando el proceso sigue vivo.
    """
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(
        prefix=f".{target.name}.", suffix=".tmp", dir=str(target.parent)
    )
    tmp = Path(tmp_name)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(tmp, target)
        _fsync_directory(target.parent)
    finally:
        if tmp.exists():
            try:
                tmp.unlink()
            except OSError:
                pass


def atomic_write_json(path: Path | str, value: object) -> None:
    text = json.dumps(value, ensure_ascii=False, indent=2) + "\n"
    atomic_write_text(path, text)


def read_json(path: Path | str) -> dict:
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)
