# Smoke tests

Scripts Python que rodam end-to-end contra a aplicação em execução. Cada fase tem um arquivo (`smoke_phase<N>.py`) com setup/teardown próprio.

## Setup (uma vez por máquina)

```powershell
cd C:\Users\mathe\Documents\backend-upload-book
python -m venv .venv
.\.venv\Scripts\activate
pip install -r tests\requirements.txt
```

Se `python` não funcionar no terminal, instale via Microsoft Store (Python 3.12) ou direto do site oficial.

## Como rodar

1. Suba a aplicação no IntelliJ (▶ na Run Configuration).
2. Em outro terminal, com o venv ativo:
   ```powershell
   python tests\smoke_phase1.py
   ```
3. Script lê `.env`, conecta no Postgres, insere usuários `smoke_*@teste.com`, roda os cenários, remove os usuários no fim.

## Cobertura por fase

| Script | Fase | Cenários |
| --- | --- | --- |
| `smoke_phase1.py` | Phase 1 — Login | Admin seed, APROVADO, senha errada, e-mail inexistente, PENDENTE/REJEITADO/DESATIVADO, endpoint protegido com/sem token, logout, token revogado |

## Resultado

Saída humana com ✓ / ✗ por asserção e resumo no fim. Exit code 0 = tudo passou, 1 = alguma asserção falhou. Bom pra CI eventualmente.
