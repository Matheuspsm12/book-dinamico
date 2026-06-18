#!/usr/bin/env python3
"""
Smoke test automatizado da Phase 1 — Autenticação.

Cobre:
  1. Login feliz (admin seed)
  2. Login feliz (usuário APROVADO via setup)
  3. Senha errada     → 403, erro-credenciais-invalidas
  4. E-mail inexistente → 403, erro-credenciais-invalidas
  5. Conta PENDENTE   → 403, erro-conta-pendente
  6. Conta REJEITADO  → 403, erro-conta-rejeitada
  7. Conta DESATIVADO → 403, erro-conta-desativada
  8. GET protegido sem token → 403
  9. GET protegido com token (rota inexistente) → 404 (NoResourceFoundException)
 10. Logout revoga jti
 11. Token revogado não autentica mais

Setup/teardown via psycopg2 (insere e remove usuários de teste).
Lê credenciais do .env na raiz do projeto.

Uso:
    cd C:\\Users\\mathe\\Documents\\backend-upload-book
    python -m venv .venv
    .venv\\Scripts\\activate
    pip install -r tests\\requirements.txt
    # Suba a aplicação no IntelliJ primeiro!
    python tests\\smoke_phase1.py
"""

from __future__ import annotations

import os
import sys
import time
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse

# Força UTF-8 no stdout/stderr — o terminal Windows default (CP1252) explode
# ao tentar imprimir ✓ / ✗ / … usados pelo relatório.
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
    """jdbc:postgresql://host:port/db -> dict pra psycopg2.connect."""
    parsed = urlparse(jdbc.replace("jdbc:", ""))
    return {
        "host": parsed.hostname,
        "port": parsed.port or 5432,
        "dbname": parsed.path.lstrip("/"),
    }


DB_CONN_INFO = parse_jdbc_url(DB_URL)

# Usuários de teste — todos com mesma senha pra simplificar
TEST_PASSWORD = "Senha@123"
TEST_USERS = [
    ("smoke_aprovado@teste.com",   "Smoke Aprovado",   "APROVADO",   "USUARIO"),
    ("smoke_pendente@teste.com",   "Smoke Pendente",   "PENDENTE",   "USUARIO"),
    ("smoke_rejeitado@teste.com",  "Smoke Rejeitado",  "REJEITADO",  "USUARIO"),
    ("smoke_desativado@teste.com", "Smoke Desativado", "DESATIVADO", "USUARIO"),
]
TEST_EMAILS = [u[0] for u in TEST_USERS]


# ---------------------------------------------------------------------------
# DB helpers
# ---------------------------------------------------------------------------

def db_connect():
    # client_encoding=UTF8 + lc_messages=C evita UnicodeDecodeError quando
    # o servidor Postgres roda com locale pt-BR (default em instalação Windows brasileira)
    # e manda mensagens de status em CP1252.
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
    """Remove qualquer resíduo e insere os usuários de teste com BCrypt via pgcrypto."""
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute("DELETE FROM usuario WHERE email = ANY(%s)", (TEST_EMAILS,))
            for email, nome, status, role in TEST_USERS:
                cur.execute(
                    """
                    INSERT INTO usuario (
                        id, nome, empresa, email, senha_hash, justificativa,
                        status, role, criado_em, atualizado_em
                    ) VALUES (
                        nextval('usuario_seq'), %s, 'Acme', %s,
                        crypt(%s, gen_salt('bf')), 'Smoke test',
                        %s, %s, NOW(), NOW()
                    )
                    """,
                    (nome, email, TEST_PASSWORD, status, role),
                )


def db_teardown():
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute("DELETE FROM usuario WHERE email = ANY(%s)", (TEST_EMAILS,))


def db_assert_bcrypt_matches(email: str, raw_password: str) -> bool:
    """pgcrypto verifica nativamente se a senha bate com o hash armazenado."""
    with db_connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT (senha_hash = crypt(%s, senha_hash)) FROM usuario WHERE email = %s",
                (raw_password, email),
            )
            row = cur.fetchone()
            return bool(row and row[0])


# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------

def wait_for_app(timeout_s: int = 30):
    """Pinga /v3/api-docs até voltar algo (qualquer status indica que o servidor está vivo)."""
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


def login(email: str, senha: str) -> requests.Response:
    return requests.post(
        f"{BASE_URL}/autenticacao/login",
        json={"email": email, "senha": senha},
        timeout=10,
    )


def auth_get(path: str, token: Optional[str] = None) -> requests.Response:
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    return requests.get(f"{BASE_URL}{path}", headers=headers, timeout=10)


def logout(token: str) -> requests.Response:
    return requests.post(
        f"{BASE_URL}/autenticacao/logout",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )


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


def run_tests() -> bool:
    t = TestRunner()

    # 0. App vivo
    print("\n[0] Aguardando aplicação subir…")
    if not wait_for_app():
        print("  ✗ aplicação não respondeu em 30s")
        return False
    print("  ✓ aplicação respondendo")

    # Pré-condição: pgcrypto matches em todos os usuários inseridos
    print("\n[Pré-check] Verificando hashes via pgcrypto…")
    for email, *_ in TEST_USERS:
        t.check(
            f"hash bate em {email}",
            db_assert_bcrypt_matches(email, TEST_PASSWORD),
        )

    # 1. Admin seedado
    print("\n[1] Login admin seed")
    r = login("admin@claro.com.br", "admin")
    t.assert_eq("admin status", r.status_code, 200)
    admin_token = None
    if r.status_code == 200:
        body = r.json()
        t.assert_eq("admin role", body.get("role"), "ADMIN")
        t.assert_eq("admin nome", body.get("nome"), "Administrador")
        t.check("admin tem token", bool(body.get("token")))
        admin_token = body.get("token")
    else:
        print(f"    body: {r.text}")

    # 2. Login aprovado dinâmico
    print("\n[2] Login usuário APROVADO")
    r = login("smoke_aprovado@teste.com", TEST_PASSWORD)
    t.assert_eq("aprovado status", r.status_code, 200)
    if r.status_code == 200:
        t.assert_eq("aprovado role", r.json().get("role"), "USUARIO")
    else:
        print(f"    body: {r.text}")

    # 3. Cenários de erro (A7 mensagens distintas)
    cenarios = [
        ("[3a] senha errada",        "admin@claro.com.br",         "senha-errada", "erro-credenciais-invalidas"),
        ("[3b] e-mail inexistente",  "naoexiste@teste.com",        TEST_PASSWORD,  "erro-credenciais-invalidas"),
        ("[3c] conta PENDENTE",      "smoke_pendente@teste.com",   TEST_PASSWORD,  "erro-conta-pendente"),
        ("[3d] conta REJEITADO",     "smoke_rejeitado@teste.com",  TEST_PASSWORD,  "erro-conta-rejeitada"),
        ("[3e] conta DESATIVADO",    "smoke_desativado@teste.com", TEST_PASSWORD,  "erro-conta-desativada"),
    ]
    for label, email, senha, msg_esperada in cenarios:
        print(f"\n{label}")
        r = login(email, senha)
        t.assert_eq(f"{label} status", r.status_code, 403)
        try:
            t.assert_eq(f"{label} message", r.json().get("message"), msg_esperada)
        except ValueError:
            t.check(f"{label} JSON válido", False, f"corpo não-JSON: {r.text}")

    # 4. Endpoint protegido
    print("\n[4a] GET protegido sem token")
    r = auth_get("/api/qualquercoisa")
    t.assert_eq("sem-token status", r.status_code, 403)

    print("\n[4b] GET endpoint inexistente com token válido (passa pelo filtro)")
    if admin_token:
        r = auth_get("/api/qualquercoisa", token=admin_token)
        t.assert_eq("com-token status", r.status_code, 404)
    else:
        t.check("com-token", False, "sem admin_token disponível")

    # 5. Logout + token revogado
    print("\n[5a] Logout revoga jti")
    if admin_token:
        r = logout(admin_token)
        t.assert_eq("logout status", r.status_code, 200)

        print("\n[5b] Token revogado não autentica mais")
        r = auth_get("/api/qualquercoisa", token=admin_token)
        # Token revogado → JwtFilter não autentica → cai em authenticated() barrado
        t.assert_eq("revogado status", r.status_code, 403)
    else:
        t.check("logout", False, "sem admin_token disponível")

    return t.summary()


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------

def main():
    print(f"Smoke Phase 1 — alvo: {BASE_URL}")
    print(f"DB: {DB_CONN_INFO['host']}:{DB_CONN_INFO['port']}/{DB_CONN_INFO['dbname']}")

    print("\n[Setup] inserindo usuários de teste…")
    db_setup()

    try:
        ok = run_tests()
    finally:
        print("\n[Teardown] removendo usuários de teste…")
        db_teardown()

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
