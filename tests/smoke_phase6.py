#!/usr/bin/env python3
"""
Smoke test automatizado da Phase 6 — Endpoints públicos novos.

Cobre os ajustes de acesso (rotas públicas, sem token):
  1. POST /autenticacao/esqueci-senha  → 204 mesmo p/ e-mail inexistente (não vaza existência)
  2. POST /autenticacao/esqueci-senha  → 204 p/ usuário APROVADO
  3. POST /autenticacao/esqueci-senha  → 400 p/ e-mail com formato inválido (validação)
  4. POST /autenticacao/redefinir-senha → 400 + token-invalido p/ token forjado
  5. POST /autenticacao/redefinir-senha → 400 p/ senha curta (validação)
  6. POST /autenticacao/manter-acesso   → 400 + token-invalido p/ token forjado

Observação: o caminho feliz de redefinir/manter exige um token assinado pelo servidor
(secret do app), inviável de forjar aqui — então validamos rejeição + roteamento público.
O envio real de e-mail depende de BOOK_EMAIL_HABILITADO; o status HTTP é 204 de qualquer forma.

Setup/teardown via psycopg2 (insere e remove usuário de teste).
Lê credenciais do .env na raiz do projeto.

Uso:
    python -m venv .venv && source .venv/bin/activate   # (Windows: .venv\\Scripts\\activate)
    pip install -r tests/requirements.txt
    # Suba a aplicação primeiro!
    python tests/smoke_phase6.py
"""

from __future__ import annotations

import os
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

import psycopg2
import requests
from dotenv import load_dotenv

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env")

DB_URL = os.environ["BOOK_DB_URL"]
DB_USER = os.environ["BOOK_DB_USERNAME"]
DB_PASS = os.environ["BOOK_DB_PASSWORD"]
APP_PORT = os.environ.get("BOOK_APP_PORT", "8082")
APP_CONTEXT_PATH = os.environ.get("BOOK_APP_CONTEXT_PATH", "/book_dinamico")
BASE_URL = f"http://localhost:{APP_PORT}{APP_CONTEXT_PATH}"


def parse_jdbc_url(jdbc: str) -> dict:
    parsed = urlparse(jdbc.replace("jdbc:", ""))
    return {
        "host": parsed.hostname,
        "port": parsed.port or 5432,
        "dbname": parsed.path.lstrip("/"),
    }


DB_CONN_INFO = parse_jdbc_url(DB_URL)

TEST_PASSWORD = "Senha@123"
SMOKE_EMAIL = "smoke_reset@teste.com"
TEST_EMAILS = [SMOKE_EMAIL]


# ---------------------------------------------------------------------------
# DB helpers
# ---------------------------------------------------------------------------

def db_connect():
    return psycopg2.connect(
        host=DB_CONN_INFO["host"],
        port=DB_CONN_INFO["port"],
        dbname=DB_CONN_INFO["dbname"],
        user=DB_USER,
        password=DB_PASS,
        client_encoding="UTF8",
        options="-c lc_messages=C",
    )


def db_setup():
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute("DELETE FROM usuario WHERE email = ANY(%s)", (TEST_EMAILS,))
            cur.execute(
                """
                INSERT INTO usuario (
                    id, nome, empresa, email, senha_hash, justificativa,
                    status, role, criado_em, atualizado_em
                ) VALUES (
                    nextval('usuario_seq'), 'Smoke Reset', 'Acme', %s,
                    crypt(%s, gen_salt('bf')), 'Smoke test',
                    'APROVADO', 'USUARIO', NOW(), NOW()
                )
                """,
                (SMOKE_EMAIL, TEST_PASSWORD),
            )


def db_teardown():
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute("DELETE FROM usuario WHERE email = ANY(%s)", (TEST_EMAILS,))


# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------

def wait_for_app(timeout_s: int = 30) -> bool:
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        try:
            r = requests.get(f"{BASE_URL}/v3/api-docs", timeout=2)
            if r.status_code < 500:
                return True
        except requests.exceptions.RequestException:
            pass
        time.sleep(1)
    return False


def esqueci_senha(email: str) -> requests.Response:
    return requests.post(f"{BASE_URL}/autenticacao/esqueci-senha", json={"email": email}, timeout=10)


def redefinir_senha(token: str, nova_senha: str) -> requests.Response:
    return requests.post(
        f"{BASE_URL}/autenticacao/redefinir-senha",
        json={"token": token, "novaSenha": nova_senha},
        timeout=10,
    )


def manter_acesso(token: str) -> requests.Response:
    return requests.post(f"{BASE_URL}/autenticacao/manter-acesso", json={"token": token}, timeout=10)


# ---------------------------------------------------------------------------
# Test runner
# ---------------------------------------------------------------------------

class TestRunner:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.failures: list[str] = []

    def check(self, name: str, condition: bool, detail: str = ""):
        if condition:
            print(f"  ✓ {name}")
            self.passed += 1
        else:
            print(f"  ✗ {name}{(' — ' + detail) if detail else ''}")
            self.failed += 1
            self.failures.append(name)

    def assert_eq(self, name: str, actual, expected):
        self.check(name, actual == expected, f"esperado {expected!r}, recebido {actual!r}")

    def summary(self) -> bool:
        total = self.passed + self.failed
        print()
        print("=" * 64)
        print(f"Resultado: {self.passed}/{total} passou")
        if self.failures:
            print("Testes que falharam:")
            for f in self.failures:
                print(f"  - {f}")
        else:
            print("Todos os testes passaram.")
        return self.failed == 0


FORGED_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwIn0.assinatura-invalida"


def run_tests() -> bool:
    t = TestRunner()

    print("\n[0] Aguardando aplicação subir…")
    if not wait_for_app():
        print("  ✗ aplicação não respondeu em 30s")
        return False
    print("  ✓ aplicação respondendo")

    print("\n[1] esqueci-senha — e-mail inexistente (não vaza existência)")
    r = esqueci_senha("naoexiste_xyz@teste.com")
    t.assert_eq("status", r.status_code, 204)

    print("\n[2] esqueci-senha — usuário APROVADO")
    r = esqueci_senha(SMOKE_EMAIL)
    t.assert_eq("status", r.status_code, 204)

    print("\n[3] esqueci-senha — e-mail inválido (validação)")
    r = esqueci_senha("isto-nao-e-email")
    t.assert_eq("status", r.status_code, 400)

    print("\n[4] redefinir-senha — token forjado")
    r = redefinir_senha(FORGED_TOKEN, "NovaSenha@123")
    t.assert_eq("status", r.status_code, 400)
    try:
        t.assert_eq("message", r.json().get("message"), "token-invalido")
    except ValueError:
        t.check("JSON válido", False, f"corpo não-JSON: {r.text}")

    print("\n[5] redefinir-senha — senha curta (validação)")
    r = redefinir_senha(FORGED_TOKEN, "123")
    t.assert_eq("status", r.status_code, 400)

    print("\n[6] manter-acesso — token forjado")
    r = manter_acesso(FORGED_TOKEN)
    t.assert_eq("status", r.status_code, 400)
    try:
        t.assert_eq("message", r.json().get("message"), "token-invalido")
    except ValueError:
        t.check("JSON válido", False, f"corpo não-JSON: {r.text}")

    return t.summary()


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------

def main():
    print(f"Smoke Phase 6 — alvo: {BASE_URL}")
    print(f"DB: {DB_CONN_INFO['host']}:{DB_CONN_INFO['port']}/{DB_CONN_INFO['dbname']}")

    print("\n[Setup] inserindo usuário de teste…")
    db_setup()

    try:
        ok = run_tests()
    finally:
        print("\n[Teardown] removendo usuário de teste…")
        db_teardown()

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
