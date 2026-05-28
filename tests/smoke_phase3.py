#!/usr/bin/env python3
"""
Smoke test automatizado da Phase 3 — Aprovação (US3 / RN09-RN11).

Cobre:
  1. Admin aprova usuário PENDENTE → 200, status APROVADO, aprovado_por + decidido_em populados
  2. Login pós-aprovação funciona (regressão Phase 1)
  3. Admin rejeita usuário PENDENTE → 200, status REJEITADO
  4. Aprovar usuário já APROVADO → 400 erro-decisao-invalida-status-nao-pendente
  5. Aprovar usuário inexistente → 404
  6. Aprovar como não-admin → 403
  7. Cap de 40: com 40 APROVADOs já no banco, aprovação #41 → 400 cap-usuarios-excedido
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

PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env")

DB_URL = os.environ["BOOK_DB_URL"]
DB_USER = os.environ["BOOK_DB_USERNAME"]
DB_PASS = os.environ["BOOK_DB_PASSWORD"]
APP_PORT = os.environ.get("BOOK_APP_PORT", "8082")
APP_CONTEXT_PATH = os.environ.get("BOOK_APP_CONTEXT_PATH", "/book_dinamico")
BASE_URL = f"http://localhost:{APP_PORT}{APP_CONTEXT_PATH}"

TEST_PWD = "Senha@123"
EMAIL_PREFIX = "approval_"  # todos os usuários de teste começam com isso


def parse_jdbc_url(jdbc: str) -> dict:
    parsed = urlparse(jdbc.replace("jdbc:", ""))
    return {"host": parsed.hostname, "port": parsed.port or 5432, "dbname": parsed.path.lstrip("/")}


DB = parse_jdbc_url(DB_URL)


def db_connect():
    return psycopg2.connect(
        host=DB["host"], port=DB["port"], dbname=DB["dbname"],
        user=DB_USER, password=DB_PASS,
        client_encoding="UTF8", options="-c lc_messages=C",
    )


def db_cleanup():
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute("DELETE FROM usuario WHERE email LIKE %s", (EMAIL_PREFIX + "%",))


def db_insert_user(email, status, role="USUARIO", nome=None):
    nome = nome or f"User {email}"
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO usuario (id, nome, empresa, email, senha_hash, justificativa,
                                     status, role, criado_em, atualizado_em)
                VALUES (nextval('usuario_seq'), %s, 'Acme', %s,
                        crypt(%s, gen_salt('bf')), 'smoke test',
                        %s, %s, NOW(), NOW())
                RETURNING id
                """,
                (nome, email, TEST_PWD, status, role),
            )
            return cur.fetchone()[0]


def db_fetch(email):
    with db_connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id, status, role, aprovado_por, decidido_em FROM usuario WHERE email = %s",
                (email,),
            )
            return cur.fetchone()


def db_count_aprovados():
    with db_connect() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT count(*) FROM usuario WHERE status='APROVADO'")
            return cur.fetchone()[0]


def wait_for_app(timeout_s=30):
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


def login(email, senha):
    return requests.post(f"{BASE_URL}/autenticacao/login", json={"email": email, "senha": senha}, timeout=10)


def aprovar(token, user_id):
    return requests.post(
        f"{BASE_URL}/api/usuarios/{user_id}/aprovar",
        headers={"Authorization": f"Bearer {token}"}, timeout=10,
    )


def rejeitar(token, user_id):
    return requests.post(
        f"{BASE_URL}/api/usuarios/{user_id}/rejeitar",
        headers={"Authorization": f"Bearer {token}"}, timeout=10,
    )


class TestRunner:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.failures = []

    def check(self, name, condition, detail=""):
        if condition:
            print(f"  ✓ {name}")
            self.passed += 1
        else:
            print(f"  ✗ {name}{(' — ' + detail) if detail else ''}")
            self.failed += 1
            self.failures.append(name)

    def assert_eq(self, name, actual, expected):
        self.check(name, actual == expected, f"esperado {expected!r}, recebido {actual!r}")

    def summary(self):
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


def run_tests():
    t = TestRunner()

    print("\n[0] Aguardando aplicação subir…")
    if not wait_for_app():
        print("  ✗ aplicação não respondeu em 30s")
        return False
    print("  ✓ aplicação respondendo")

    # Login admin para obter token
    print("\n[Setup] login admin")
    r = login("admin@claro.com.br", "admin")
    if r.status_code != 200:
        print(f"  ✗ admin login falhou: {r.status_code} {r.text}")
        return False
    admin_token = r.json()["token"]
    print("  ✓ admin token obtido")

    # ------------------------------------------------------------------
    # 1. Admin aprova PENDENTE
    # ------------------------------------------------------------------
    print("\n[1] Admin aprova usuário PENDENTE")
    uid = db_insert_user(EMAIL_PREFIX + "ok@teste.com", "PENDENTE")
    r = aprovar(admin_token, uid)
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        body = r.json()
        t.assert_eq("body status APROVADO", body.get("status"), "APROVADO")
        t.check("body decididoEm presente", bool(body.get("decididoEm")))
        t.check("body aprovadoPorId presente", body.get("aprovadoPorId") is not None)

        row = db_fetch(EMAIL_PREFIX + "ok@teste.com")
        _, status_db, _, aprovado_por_db, decidido_em_db = row
        t.assert_eq("DB status APROVADO", status_db, "APROVADO")
        t.check("DB aprovado_por populado", aprovado_por_db is not None)
        t.check("DB decidido_em populado", decidido_em_db is not None)

    # ------------------------------------------------------------------
    # 2. Login pós-aprovação funciona
    # ------------------------------------------------------------------
    print("\n[2] Login pós-aprovação")
    r = login(EMAIL_PREFIX + "ok@teste.com", TEST_PWD)
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        t.assert_eq("role USUARIO", r.json().get("role"), "USUARIO")

    # ------------------------------------------------------------------
    # 3. Admin rejeita PENDENTE
    # ------------------------------------------------------------------
    print("\n[3] Admin rejeita usuário PENDENTE")
    uid = db_insert_user(EMAIL_PREFIX + "rej@teste.com", "PENDENTE")
    r = rejeitar(admin_token, uid)
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        t.assert_eq("body status REJEITADO", r.json().get("status"), "REJEITADO")

    # ------------------------------------------------------------------
    # 4. Aprovar usuário já APROVADO → 400
    # ------------------------------------------------------------------
    print("\n[4] Aprovar já-APROVADO")
    uid = db_insert_user(EMAIL_PREFIX + "ja_aprovado@teste.com", "APROVADO")
    r = aprovar(admin_token, uid)
    t.assert_eq("status 400", r.status_code, 400)
    if r.status_code == 400:
        t.assert_eq("message", r.json().get("message"), "erro-decisao-invalida-status-nao-pendente")

    # ------------------------------------------------------------------
    # 5. Aprovar inexistente → 404
    # ------------------------------------------------------------------
    print("\n[5] Aprovar id inexistente")
    r = aprovar(admin_token, 99999999)
    t.assert_eq("status 404", r.status_code, 404)

    # ------------------------------------------------------------------
    # 6. Aprovar como não-admin → 403
    # ------------------------------------------------------------------
    print("\n[6] Aprovar como USUARIO comum")
    db_insert_user(EMAIL_PREFIX + "usuario_comum@teste.com", "APROVADO", role="USUARIO")
    r_login = login(EMAIL_PREFIX + "usuario_comum@teste.com", TEST_PWD)
    if r_login.status_code != 200:
        t.check("login usuário comum", False, f"status {r_login.status_code}")
    else:
        usuario_token = r_login.json()["token"]
        uid_target = db_insert_user(EMAIL_PREFIX + "alvo_nao_admin@teste.com", "PENDENTE")
        r = aprovar(usuario_token, uid_target)
        t.assert_eq("status 403", r.status_code, 403)

    # ------------------------------------------------------------------
    # 7. Cap de 40: 41ª aprovação rejeitada
    # ------------------------------------------------------------------
    print("\n[7] Cap de 40 APROVADOs")
    # Limpa para começar do zero e ter contagem previsível
    db_cleanup()
    # Insere 40 usuários já APROVADOS (preenche o cap)
    for i in range(40):
        db_insert_user(f"{EMAIL_PREFIX}cap_{i:02d}@teste.com", "APROVADO")
    # +1 PENDENTE pra tentar aprovar
    uid_41 = db_insert_user(EMAIL_PREFIX + "cap_pend@teste.com", "PENDENTE")

    aprovados = db_count_aprovados()
    t.check(f"pré-condição: {aprovados} APROVADOs (esperado ≥40)", aprovados >= 40)

    r = aprovar(admin_token, uid_41)
    t.assert_eq("status 400 (cap)", r.status_code, 400)
    if r.status_code == 400:
        t.assert_eq("message", r.json().get("message"), "cap-usuarios-excedido")

    return t.summary()


def main():
    print(f"Smoke Phase 3 — alvo: {BASE_URL}")
    print(f"DB: {DB['host']}:{DB['port']}/{DB['dbname']}")

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
