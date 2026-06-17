#!/usr/bin/env python3
"""
Launcher: carrega .env e sobe a aplicação via mvn spring-boot:run.
Útil pra reiniciar a aplicação fora do IntelliJ quando precisar de uma
recompilação completa antes de rodar os smoke tests.
"""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

from dotenv import dotenv_values

PROJECT_ROOT = Path(__file__).resolve().parent.parent
MVN_CANDIDATES = [
    r"C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\plugins\maven\lib\maven3\bin\mvn.cmd",
]


def find_mvn() -> str:
    for candidate in MVN_CANDIDATES:
        if Path(candidate).exists():
            return candidate
    raise SystemExit("Maven não encontrado nos paths conhecidos. Edite MVN_CANDIDATES em tests/run_app.py.")


def main() -> int:
    env = {**os.environ, **dotenv_values(PROJECT_ROOT / ".env")}
    mvn = find_mvn()
    print(f"Usando Maven: {mvn}", flush=True)
    print(f"Working dir: {PROJECT_ROOT}", flush=True)
    proc = subprocess.run(
        [mvn, "spring-boot:run", "-q"],
        cwd=PROJECT_ROOT,
        env=env,
    )
    return proc.returncode


if __name__ == "__main__":
    sys.exit(main())
