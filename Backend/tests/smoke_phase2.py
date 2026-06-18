#!/usr/bin/env python3
"""
Smoke test automatizado da Phase 2 — Autocadastro (US2).

Cobre:
  1. Cadastro feliz → 201, status PENDENTE, role USUARIO
  2. E-mail duplicado → 400, erro-email-duplicado
  3. Campos obrigatórios faltando → 400 (validação)
  4. Senha muito curta (< 8) → 400 (validação)
  5. E-mail malformado → 400 (validação)
  6. Integração com Phase 1: cadastrado vira PENDENTE → login retorna erro-conta-pendente
  7. Não permite vazar role/status (cliente tenta enviar role=ADMIN; backend ignora)

Setup: limpa usuários `cadastro_*@teste.com` antes; teardown remove no fim.
"""

from __future__ import annotations

import os
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

# Força UTF-8 no stdout/stderr
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

import psycopg2
import requests
from dotenv import load_dotenv

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
TEST_EMAILS = [
    "cadastro_ok@teste.com",
    "cadastro_dup@teste.com",
    "cadastro_role_attack@teste.com",
]


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


def db_cleanup():
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute("DELETE FROM usuario WHERE email = ANY(%s)", (TEST_EMAILS,))


def db_fetch(email: str):
    with db_connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id, email, status, role, length(senha_hash), LEFT(senha_hash, 4) "
                "FROM usuario WHERE email = %s",
                (email,),
            )
            return cur.fetchone()


def wait_for_app(timeout_s: int = 30):
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


def post_cadastro(payload):
    return requests.post(f"{BASE_URL}/api/usuarios/cadastro", json=payload, timeout=10)


def post_login(email, senha):
    return requests.post(
        f"{BASE_URL}/autenticacao/login", json={"email": email, "senha": senha}, timeout=10
    )


def run_tests() -> bool:
    t = TestRunner()

    print("\n[0] Aguardando aplicação subir…")
    if not wait_for_app():
        print("  ✗ aplicação não respondeu em 30s")
        return False
    print("  ✓ aplicação respondendo")

    # 1. Cadastro feliz
    print("\n[1] Cadastro feliz")
    payload_ok = {
        "nome": "Fulano Teste",
        "empresa": "Acme",
        "email": "cadastro_ok@teste.com",
        "senha": "SenhaForte123",
        "justificativa": "Preciso acessar o portal pra revisar o book.",
    }
    r = post_cadastro(payload_ok)
    t.assert_eq("status 201", r.status_code, 201)
    if r.status_code == 201:
        body = r.json()
        t.assert_eq("status retornado PENDENTE", body.get("status"), "PENDENTE")
        t.assert_eq("role retornado USUARIO", body.get("role"), "USUARIO")
        t.check("id presente", bool(body.get("id")))
        t.check("senha não vazada na resposta", "senha" not in body and "senhaHash" not in body)

        # Verifica no banco
        row = db_fetch("cadastro_ok@teste.com")
        t.check("registro persistido", row is not None)
        if row:
            _, _, status_db, role_db, hash_len, hash_prefix = row
            t.assert_eq("DB status PENDENTE", status_db, "PENDENTE")
            t.assert_eq("DB role USUARIO", role_db, "USUARIO")
            t.assert_eq("DB hash len 60", hash_len, 60)
            t.assert_eq("DB hash prefix BCrypt", hash_prefix, "$2a$")
    else:
        print(f"    body: {r.text}")

    # 2. E-mail duplicado
    print("\n[2] E-mail duplicado")
    r = post_cadastro(payload_ok)
    t.assert_eq("status 400", r.status_code, 400)
    if r.status_code == 400:
        t.assert_eq("message", r.json().get("message"), "erro-email-duplicado")

    # 3. Campos obrigatórios
    print("\n[3] Cadastro sem campos obrigatórios")
    r = post_cadastro({"email": "outro@teste.com"})
    t.assert_eq("status 400", r.status_code, 400)

    # 4. Senha curta
    print("\n[4] Senha < 8 caracteres")
    payload_curta = {**payload_ok, "email": "outra@teste.com", "senha": "abc"}
    r = post_cadastro(payload_curta)
    t.assert_eq("status 400", r.status_code, 400)

    # 5. E-mail malformado
    print("\n[5] E-mail malformado")
    payload_email = {**payload_ok, "email": "nao-eh-email"}
    r = post_cadastro(payload_email)
    t.assert_eq("status 400", r.status_code, 400)

    # 6. Integração Phase 1: PENDENTE não loga
    print("\n[6] Cadastrado tenta logar imediatamente → erro-conta-pendente")
    r = post_login("cadastro_ok@teste.com", "SenhaForte123")
    t.assert_eq("status 403", r.status_code, 403)
    if r.status_code == 403:
        t.assert_eq("message", r.json().get("message"), "erro-conta-pendente")

    # 7. Cliente tentando setar role=ADMIN → backend ignora
    print("\n[7] Cliente injeta role=ADMIN no payload → backend força USUARIO")
    payload_attack = {
        **payload_ok,
        "email": "cadastro_role_attack@teste.com",
        "role": "ADMIN",         # campo extra, ignorado pelo DTO
        "status": "APROVADO",    # idem
    }
    r = post_cadastro(payload_attack)
    t.assert_eq("status 201", r.status_code, 201)
    if r.status_code == 201:
        body = r.json()
        t.assert_eq("role forçado USUARIO", body.get("role"), "USUARIO")
        t.assert_eq("status forçado PENDENTE", body.get("status"), "PENDENTE")

    return t.summary()


def main():
    print(f"Smoke Phase 2 — alvo: {BASE_URL}")
    print(f"DB: {DB_CONN_INFO['host']}:{DB_CONN_INFO['port']}/{DB_CONN_INFO['dbname']}")

    print("\n[Setup] limpando usuários de teste anteriores…")
    db_cleanup()

    try:
        ok = run_tests()
    finally:
        print("\n[Teardown] removendo usuários de teste…")
        db_cleanup()

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
