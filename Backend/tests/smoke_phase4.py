#!/usr/bin/env python3
"""
Smoke test automatizado da Phase 4 — Gerenciamento (US4 / RN12-RN15 / A3).

Cobre:
  1. Paginar como admin → 200, Page<UsuarioResponse> com totalElements ≥ inseridos
  2. Filtrar por status=PENDENTE → só retorna PENDENTEs
  3. Filtrar por empresa=Acme → só retorna empresas que casam
  4. Filtrar por nome (case/acento-insensitive)
  5. Paginar como USUARIO comum → 403
  6. Atualizar nome/empresa/email do usuário → persiste
  7. Atualizar email pra um já existente → 400 erro-email-duplicado
  8. Desativar usuário → status DESATIVADO + login retorna erro-conta-desativada
  9. Reativar usuário → status APROVADO + login funciona
 10. Reativar quando cap=40 cheio → 400 cap-usuarios-excedido
 11. Atualizar/ativar/desativar como não-admin → 403
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
EMAIL_PREFIX = "mgmt_"


def parse_jdbc_url(jdbc):
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


def db_insert(email, status, role="USUARIO", empresa="Acme", nome=None):
    nome = nome or f"User {email}"
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO usuario (id, nome, empresa, email, senha_hash, justificativa,
                                     status, role, criado_em, atualizado_em)
                VALUES (nextval('usuario_seq'), %s, %s, %s,
                        crypt(%s, gen_salt('bf')), 'smoke test',
                        %s, %s, NOW(), NOW())
                RETURNING id
                """,
                (nome, empresa, email, TEST_PWD, status, role),
            )
            return cur.fetchone()[0]


def db_status(email):
    with db_connect() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT status FROM usuario WHERE email = %s", (email,))
            row = cur.fetchone()
            return row[0] if row else None


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


def auth_req(method, path, token, **kw):
    headers = kw.pop("headers", {})
    headers["Authorization"] = f"Bearer {token}"
    return requests.request(method, f"{BASE_URL}{path}", headers=headers, timeout=10, **kw)


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

    print("\n[Setup] login admin")
    r = login("admin@claro.com.br", "admin")
    if r.status_code != 200:
        print(f"  ✗ admin login falhou: {r.status_code}")
        return False
    admin_token = r.json()["token"]

    # Inserir conjunto base de usuários: 2 APROVADO Acme, 2 PENDENTE Beta, 1 REJEITADO Acme
    print("\n[Setup] inserindo conjunto base")
    db_insert(EMAIL_PREFIX + "ap1@teste.com",  "APROVADO", empresa="Acme",  nome="João Silva")
    db_insert(EMAIL_PREFIX + "ap2@teste.com",  "APROVADO", empresa="Acme",  nome="Maria José")
    db_insert(EMAIL_PREFIX + "pe1@teste.com",  "PENDENTE", empresa="Beta",  nome="Carlos")
    db_insert(EMAIL_PREFIX + "pe2@teste.com",  "PENDENTE", empresa="Beta",  nome="Ana")
    db_insert(EMAIL_PREFIX + "re1@teste.com",  "REJEITADO", empresa="Acme", nome="Pedro")

    # ------------------------------------------------------------------
    # 1. Paginar como admin
    # ------------------------------------------------------------------
    print("\n[1] Paginar como admin (sem filtro)")
    r = auth_req("POST", "/api/usuarios/paginar?page=0&size=50", admin_token, json={})
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        body = r.json()
        total = body.get("totalElements", 0)
        t.check("totalElements >= 5", total >= 5, f"total={total}")
        t.check("content é lista", isinstance(body.get("content"), list))

    # ------------------------------------------------------------------
    # 2. Filtrar por status=PENDENTE
    # ------------------------------------------------------------------
    print("\n[2] Filtrar por status=PENDENTE")
    r = auth_req("POST", "/api/usuarios/paginar?page=0&size=50", admin_token,
                 json={"status": "PENDENTE"})
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        statuses = {u.get("status") for u in r.json()["content"]}
        t.check("só PENDENTE retorna", statuses == {"PENDENTE"}, f"recebido={statuses}")

    # ------------------------------------------------------------------
    # 3. Filtrar por empresa=Acme
    # ------------------------------------------------------------------
    print("\n[3] Filtrar por empresa=Acme")
    r = auth_req("POST", "/api/usuarios/paginar?page=0&size=50", admin_token,
                 json={"empresa": "Acme"})
    if r.status_code == 200:
        empresas = {u.get("empresa") for u in r.json()["content"] if u.get("empresa")}
        t.check("só Acme retorna", empresas == {"Acme"}, f"recebido={empresas}")
    else:
        t.check("filtro empresa", False, f"status {r.status_code}")

    # ------------------------------------------------------------------
    # 4. Filtrar por nome (case-insensitive)
    # ------------------------------------------------------------------
    print("\n[4] Filtrar por nome=jose (deve achar José, case-insensitive)")
    r = auth_req("POST", "/api/usuarios/paginar?page=0&size=50", admin_token,
                 json={"nome": "jose"})
    if r.status_code == 200:
        nomes = [u.get("nome") for u in r.json()["content"]]
        t.check("acha Maria José (case/acento insensitive)",
                any("José" in n for n in nomes),
                f"nomes={nomes}")

    # ------------------------------------------------------------------
    # 5. Paginar como USUARIO comum → 403
    # ------------------------------------------------------------------
    print("\n[5] Paginar como USUARIO comum → 403")
    db_insert(EMAIL_PREFIX + "comum@teste.com", "APROVADO", role="USUARIO")
    r_login = login(EMAIL_PREFIX + "comum@teste.com", TEST_PWD)
    if r_login.status_code != 200:
        t.check("login usuário comum", False, f"status {r_login.status_code}")
    else:
        usuario_token = r_login.json()["token"]
        r = auth_req("POST", "/api/usuarios/paginar?page=0&size=10", usuario_token, json={})
        t.assert_eq("status 403", r.status_code, 403)

    # ------------------------------------------------------------------
    # 6. Atualizar usuário
    # ------------------------------------------------------------------
    print("\n[6] Atualizar nome/empresa/email")
    uid = db_insert(EMAIL_PREFIX + "edit@teste.com", "APROVADO", empresa="Old")
    r = auth_req("PUT", f"/api/usuarios/{uid}", admin_token, json={
        "nome": "Novo Nome",
        "empresa": "NovaEmpresa",
        "email": EMAIL_PREFIX + "edited@teste.com",
    })
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        body = r.json()
        t.assert_eq("nome atualizado", body.get("nome"), "Novo Nome")
        t.assert_eq("empresa atualizada", body.get("empresa"), "NovaEmpresa")
        t.assert_eq("email atualizado", body.get("email"), EMAIL_PREFIX + "edited@teste.com")

    # ------------------------------------------------------------------
    # 7. Atualizar email pra um já existente → 400
    # ------------------------------------------------------------------
    print("\n[7] Atualizar email pra um já em uso")
    uid_dup = db_insert(EMAIL_PREFIX + "dup_alvo@teste.com", "APROVADO")
    r = auth_req("PUT", f"/api/usuarios/{uid_dup}", admin_token, json={
        "email": EMAIL_PREFIX + "edited@teste.com",  # já criado no teste 6
    })
    t.assert_eq("status 400", r.status_code, 400)
    if r.status_code == 400:
        t.assert_eq("message", r.json().get("message"), "erro-email-duplicado")

    # ------------------------------------------------------------------
    # 8. Desativar usuário
    # ------------------------------------------------------------------
    print("\n[8] Desativar usuário")
    uid_des = db_insert(EMAIL_PREFIX + "desativ@teste.com", "APROVADO")
    r = auth_req("POST", f"/api/usuarios/{uid_des}/desativar", admin_token)
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        t.assert_eq("body status DESATIVADO", r.json().get("status"), "DESATIVADO")
        t.assert_eq("DB status DESATIVADO", db_status(EMAIL_PREFIX + "desativ@teste.com"), "DESATIVADO")

    print("\n[8b] Login após desativar → erro-conta-desativada")
    r = login(EMAIL_PREFIX + "desativ@teste.com", TEST_PWD)
    t.assert_eq("status 403", r.status_code, 403)
    t.assert_eq("message", r.json().get("message"), "erro-conta-desativada")

    # ------------------------------------------------------------------
    # 9. Reativar usuário
    # ------------------------------------------------------------------
    print("\n[9] Reativar usuário DESATIVADO")
    r = auth_req("POST", f"/api/usuarios/{uid_des}/ativar", admin_token)
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        t.assert_eq("body status APROVADO", r.json().get("status"), "APROVADO")

    print("\n[9b] Login após reativar → 200")
    r = login(EMAIL_PREFIX + "desativ@teste.com", TEST_PWD)
    t.assert_eq("status 200", r.status_code, 200)

    # ------------------------------------------------------------------
    # 10. Reativar com cap cheio → 400
    # ------------------------------------------------------------------
    print("\n[10] Cap cheio bloqueia reativação")
    db_cleanup()  # zera o cenário
    # 40 APROVADOs + 1 DESATIVADO que queremos reativar
    for i in range(40):
        db_insert(f"{EMAIL_PREFIX}cap_{i:02d}@teste.com", "APROVADO")
    uid_dis = db_insert(EMAIL_PREFIX + "dis@teste.com", "DESATIVADO")
    t.check(f"pré-condição: 40 APROVADOs",
            db_count_aprovados() >= 40,
            f"count={db_count_aprovados()}")

    r = auth_req("POST", f"/api/usuarios/{uid_dis}/ativar", admin_token)
    t.assert_eq("status 400 (cap)", r.status_code, 400)
    if r.status_code == 400:
        t.assert_eq("message", r.json().get("message"), "cap-usuarios-excedido")

    # ------------------------------------------------------------------
    # 11. Atualizar/ativar como não-admin → 403
    # ------------------------------------------------------------------
    print("\n[11] Editar/desativar/ativar como USUARIO comum")
    db_cleanup()
    db_insert(EMAIL_PREFIX + "comum@teste.com", "APROVADO", role="USUARIO")
    uid_alvo = db_insert(EMAIL_PREFIX + "alvo@teste.com", "APROVADO")
    r_login = login(EMAIL_PREFIX + "comum@teste.com", TEST_PWD)
    if r_login.status_code != 200:
        t.check("login usuário comum", False, f"status {r_login.status_code}")
    else:
        usuario_token = r_login.json()["token"]
        r = auth_req("PUT", f"/api/usuarios/{uid_alvo}", usuario_token, json={"nome": "X"})
        t.assert_eq("PUT 403", r.status_code, 403)
        r = auth_req("POST", f"/api/usuarios/{uid_alvo}/desativar", usuario_token)
        t.assert_eq("desativar 403", r.status_code, 403)
        r = auth_req("POST", f"/api/usuarios/{uid_alvo}/ativar", usuario_token)
        t.assert_eq("ativar 403", r.status_code, 403)

    return t.summary()


def main():
    print(f"Smoke Phase 4 — alvo: {BASE_URL}")
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
