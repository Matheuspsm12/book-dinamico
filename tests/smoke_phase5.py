#!/usr/bin/env python3
"""
Smoke test automatizado da Phase 5 — Upload e Catálogo (US6 + US7 / RN20-RN32).

Cobre:
  1. Upload .xlsx, .xlsm, .pptx individuais → 201 + DocumentoResponse + log de upload
  2. Listar documentos → contém os enviados
  3. Buscar por id
  4. Substituição de arquivo → log de upload adicional, metadata preservada
  5. Editar só metadata (PUT /{id}) — não toca no binário
  6. Soft delete → some da listagem
  7. Lote (POST /lote) — 2 arquivos numa única requisição
  8. Extensão fora da whitelist (.txt) → 400 arquivo-extensao-nao-permitida
  9. Magic bytes inválidos (.xlsx com bytes texto) → 400 arquivo-conteudo-incompativel
 10. Download → 200 com Content-Disposition
 11. Não-admin: POST/PUT/DELETE barrados; GET listar e download permitidos
"""

from __future__ import annotations

import io
import os
import sys
import time
import zipfile
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
DIRETORIO_ARQUIVOS = os.environ["BOOK_DIRETORIO"]
BASE_URL = f"http://localhost:{APP_PORT}{APP_CONTEXT_PATH}"

TEST_PWD = "Senha@123"


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
    """Apaga uploads logs antes (FK) e documentos depois.
       Também limpa usuários de teste 'doc_*'."""
    with db_connect() as conn:
        conn.autocommit = True
        with conn.cursor() as cur:
            cur.execute(
                "DELETE FROM documento_upload_log WHERE documento_id IN "
                "(SELECT id FROM documento WHERE nome LIKE 'smoke_%')"
            )
            cur.execute("DELETE FROM documento WHERE nome LIKE 'smoke_%'")
            cur.execute("DELETE FROM usuario WHERE email LIKE 'doc_%@teste.com'")


def db_insert_user(email, status, role="USUARIO"):
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
                (f"user {email}", email, TEST_PWD, status, role),
            )
            return cur.fetchone()[0]


def db_count_uploads(documento_id):
    with db_connect() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT count(*) FROM documento_upload_log WHERE documento_id = %s", (documento_id,))
            return cur.fetchone()[0]


def db_documento(documento_id):
    with db_connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT nome, extensao, tamanho_bytes, ativo, caminho_armazenamento "
                "FROM documento WHERE id = %s", (documento_id,))
            return cur.fetchone()


# ---------------------------------------------------------------------------
# Gera arquivos OOXML-like (minimal ZIP) in-memory pra testar upload
# ---------------------------------------------------------------------------

def make_ooxml_bytes() -> bytes:
    """ZIP minimal: starts with PK\\x03\\x04, contém um único arquivo dummy."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, mode="w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("[Content_Types].xml", "<?xml version=\"1.0\"?><Types/>")
        zf.writestr("dummy.txt", "smoke test payload")
    return buf.getvalue()


OOXML_BYTES = make_ooxml_bytes()  # mesmos bytes para xlsm/xlsx/pptx no smoke


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
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return requests.request(method, f"{BASE_URL}{path}", headers=headers, timeout=30, **kw)


def upload_arquivo(token, metadata_dict, filename, content_bytes):
    files = {
        "metadata": (None, _json_part(metadata_dict), "application/json"),
        "arquivo":  (filename, content_bytes, "application/octet-stream"),
    }
    return auth_req("POST", "/api/documentos", token, files=files)


def upload_lote(token, metadata_list, arquivos_list):
    """arquivos_list = list of (filename, bytes)."""
    files = [("metadata", (None, _json_part(metadata_list), "application/json"))]
    for fn, b in arquivos_list:
        files.append(("arquivos", (fn, b, "application/octet-stream")))
    return auth_req("POST", "/api/documentos/lote", token, files=files)


def substituir_arquivo(token, doc_id, filename, content_bytes):
    files = {"arquivo": (filename, content_bytes, "application/octet-stream")}
    return auth_req("PUT", f"/api/documentos/{doc_id}/arquivo", token, files=files)


def _json_part(obj):
    """Multipart parts precisam de bytes pra Spring desserializar JSON em objeto/lista."""
    import json
    return json.dumps(obj, default=str)


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------

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

    # Cria USUARIO comum também
    db_insert_user("doc_user@teste.com", "APROVADO", "USUARIO")
    r = login("doc_user@teste.com", TEST_PWD)
    user_token = r.json()["token"] if r.status_code == 200 else None

    metadata_xlsx = {"nome": "smoke_xlsx", "descricao": "teste xlsx", "dataAtualizacao": "2026-05-27"}
    metadata_xlsm = {"nome": "smoke_xlsm", "descricao": "teste xlsm", "dataAtualizacao": "2026-05-27"}
    metadata_pptx = {"nome": "smoke_pptx", "descricao": "teste pptx", "dataAtualizacao": "2026-05-27"}

    # ------------------------------------------------------------------
    # 1. Upload individual (xlsx, xlsm, pptx)
    # ------------------------------------------------------------------
    print("\n[1] Upload .xlsx, .xlsm, .pptx")
    ids = {}
    for meta, ext in [(metadata_xlsx, "xlsx"), (metadata_xlsm, "xlsm"), (metadata_pptx, "pptx")]:
        fname = f"{meta['nome']}.{ext}"
        r = upload_arquivo(admin_token, meta, fname, OOXML_BYTES)
        t.assert_eq(f"{ext} status 201", r.status_code, 201)
        if r.status_code == 201:
            body = r.json()
            ids[ext] = body.get("id")
            t.assert_eq(f"{ext} extensao", body.get("extensao"), ext.upper())
            t.assert_eq(f"{ext} tipo", body.get("tipo"),
                        "EXCEL" if ext in ("xlsx", "xlsm") else "POWERPOINT")
            t.assert_eq(f"{ext} log de upload contado", db_count_uploads(body["id"]), 1)
        else:
            print(f"    body: {r.text}")

    # ------------------------------------------------------------------
    # 2. Listar
    # ------------------------------------------------------------------
    print("\n[2] Listar documentos")
    r = auth_req("GET", "/api/documentos", admin_token)
    t.assert_eq("status 200", r.status_code, 200)
    if r.status_code == 200:
        nomes = {d.get("nome") for d in r.json()}
        for esperado in ("smoke_xlsx", "smoke_xlsm", "smoke_pptx"):
            t.check(f"contém {esperado}", esperado in nomes)

    # ------------------------------------------------------------------
    # 3. Buscar por id
    # ------------------------------------------------------------------
    if "xlsx" in ids:
        print("\n[3] Buscar por id")
        r = auth_req("GET", f"/api/documentos/{ids['xlsx']}", admin_token)
        t.assert_eq("status 200", r.status_code, 200)
        if r.status_code == 200:
            t.assert_eq("body nome", r.json().get("nome"), "smoke_xlsx")

    # ------------------------------------------------------------------
    # 4. Substituir arquivo
    # ------------------------------------------------------------------
    if "xlsx" in ids:
        print("\n[4] Substituir arquivo do documento xlsx")
        novos_bytes = make_ooxml_bytes() + b"PADDING"  # tamanho diferente
        r = substituir_arquivo(admin_token, ids["xlsx"], "novo.xlsx", novos_bytes)
        t.assert_eq("status 200", r.status_code, 200)
        if r.status_code == 200:
            body = r.json()
            t.assert_eq("nome preservado", body.get("nome"), "smoke_xlsx")
            t.assert_eq("tamanho_bytes atualizou", body.get("tamanhoBytes"), len(novos_bytes))
            t.assert_eq("log de upload contado", db_count_uploads(ids["xlsx"]), 2)

    # ------------------------------------------------------------------
    # 5. Editar só metadata
    # ------------------------------------------------------------------
    if "xlsm" in ids:
        print("\n[5] Editar só metadata (PUT /{id})")
        r = auth_req("PUT", f"/api/documentos/{ids['xlsm']}", admin_token, json={
            "nome": "smoke_xlsm_edit",
            "descricao": "descrição atualizada",
            "dataAtualizacao": "2026-06-01",
        })
        t.assert_eq("status 200", r.status_code, 200)
        if r.status_code == 200:
            body = r.json()
            t.assert_eq("nome atualizado", body.get("nome"), "smoke_xlsm_edit")
            t.assert_eq("descricao atualizada", body.get("descricao"), "descrição atualizada")
            t.assert_eq("data atualizada", body.get("dataAtualizacao"), "2026-06-01")
            # Nenhum novo log de upload (metadata-only)
            t.assert_eq("log inalterado", db_count_uploads(ids["xlsm"]), 1)

    # ------------------------------------------------------------------
    # 6. Soft delete
    # ------------------------------------------------------------------
    if "pptx" in ids:
        print("\n[6] Soft delete do pptx")
        r = auth_req("DELETE", f"/api/documentos/{ids['pptx']}", admin_token)
        t.assert_eq("status 204", r.status_code, 204)

        row = db_documento(ids["pptx"])
        t.check("DB ativo=false", row is not None and row[3] is False)

        r = auth_req("GET", "/api/documentos", admin_token)
        if r.status_code == 200:
            nomes = {d.get("nome") for d in r.json()}
            t.check("não aparece em listar", "smoke_pptx" not in nomes)

        r = auth_req("GET", f"/api/documentos/{ids['pptx']}", admin_token)
        t.assert_eq("buscar deletado → 404", r.status_code, 404)

    # ------------------------------------------------------------------
    # 7. Lote
    # ------------------------------------------------------------------
    print("\n[7] Upload em lote (POST /lote)")
    metadatas_lote = [
        {"nome": "smoke_lote_a", "descricao": "lote a", "dataAtualizacao": "2026-05-27"},
        {"nome": "smoke_lote_b", "descricao": "lote b", "dataAtualizacao": "2026-05-27"},
    ]
    arquivos_lote = [("a.xlsx", OOXML_BYTES), ("b.pptx", OOXML_BYTES)]
    r = upload_lote(admin_token, metadatas_lote, arquivos_lote)
    t.assert_eq("status 201", r.status_code, 201)
    if r.status_code == 201:
        body = r.json()
        t.assert_eq("retorna 2", len(body), 2)
        t.assert_eq("primeiro tipo EXCEL", body[0].get("tipo"), "EXCEL")
        t.assert_eq("segundo tipo POWERPOINT", body[1].get("tipo"), "POWERPOINT")

    # ------------------------------------------------------------------
    # 8. Extensão fora da whitelist
    # ------------------------------------------------------------------
    print("\n[8] Extensão .txt fora da whitelist")
    r = upload_arquivo(admin_token,
                       {"nome": "smoke_invalid", "descricao": "x", "dataAtualizacao": "2026-05-27"},
                       "evil.txt", OOXML_BYTES)
    t.assert_eq("status 400", r.status_code, 400)
    if r.status_code == 400:
        t.assert_eq("message", r.json().get("message"), "arquivo-extensao-nao-permitida")

    # ------------------------------------------------------------------
    # 9. Magic bytes inválidos
    # ------------------------------------------------------------------
    print("\n[9] Extensão .xlsx mas conteúdo texto puro")
    r = upload_arquivo(admin_token,
                       {"nome": "smoke_fake", "descricao": "x", "dataAtualizacao": "2026-05-27"},
                       "fake.xlsx", b"this is not a zip file")
    t.assert_eq("status 400", r.status_code, 400)
    if r.status_code == 400:
        t.assert_eq("message", r.json().get("message"), "arquivo-conteudo-incompativel")

    # ------------------------------------------------------------------
    # 10. Download
    # ------------------------------------------------------------------
    if "xlsx" in ids:
        print("\n[10] Download")
        r = auth_req("GET", f"/api/documentos/{ids['xlsx']}/download", admin_token)
        t.assert_eq("status 200", r.status_code, 200)
        if r.status_code == 200:
            disp = r.headers.get("Content-Disposition", "")
            t.check("Content-Disposition attachment", "attachment" in disp.lower())
            t.check("conteúdo bate (4 bytes magic PK)", r.content[:4] == b"PK\x03\x04")

    # ------------------------------------------------------------------
    # 11. USUARIO comum: pode GET, mas POST/PUT/DELETE = 403
    # ------------------------------------------------------------------
    print("\n[11] USUARIO comum: GETs liberados, mutações barradas")
    if user_token is None:
        t.check("usuário comum logou", False)
    else:
        r = auth_req("GET", "/api/documentos", user_token)
        t.assert_eq("GET /api/documentos 200", r.status_code, 200)

        if "xlsx" in ids:
            r = auth_req("GET", f"/api/documentos/{ids['xlsx']}/download", user_token)
            t.assert_eq("download 200", r.status_code, 200)

        r = upload_arquivo(user_token,
                           {"nome": "smoke_x", "descricao": "x", "dataAtualizacao": "2026-05-27"},
                           "x.xlsx", OOXML_BYTES)
        t.assert_eq("POST upload 403", r.status_code, 403)

        if "xlsm" in ids:
            r = auth_req("PUT", f"/api/documentos/{ids['xlsm']}", user_token, json={
                "nome": "x", "descricao": "x", "dataAtualizacao": "2026-05-27"
            })
            t.assert_eq("PUT metadata 403", r.status_code, 403)

            r = auth_req("DELETE", f"/api/documentos/{ids['xlsm']}", user_token)
            t.assert_eq("DELETE 403", r.status_code, 403)

    return t.summary()


def main():
    print(f"Smoke Phase 5 — alvo: {BASE_URL}")
    print(f"DB: {DB['host']}:{DB['port']}/{DB['dbname']}")
    print(f"Diretório de arquivos: {DIRETORIO_ARQUIVOS}")

    print("\n[Setup] limpando documentos e usuários de teste anteriores…")
    db_cleanup()

    try:
        ok = run_tests()
    finally:
        print("\n[Teardown] removendo documentos e usuários de teste…")
        db_cleanup()

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
