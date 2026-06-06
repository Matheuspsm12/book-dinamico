# Ajustes no Portal Claro Book — design

- **Data:** 2026-06-06
- **Branch:** `feat/ajustes-portal-book`
- **Status:** aprovado pelo solicitante

Quatro ajustes de experiência e gestão de acessos no portal (backend Spring Boot +
frontend Next.js 16). Convenções deste repositório (Java/Spring, App Router) — os
padrões de outros projetos não se aplicam aqui.

## Decisões travadas

1. Ociosidade — "retorno" que cancela a remoção = **login OU clique no link** do e-mail.
2. Ociosidade — remoção = **soft-delete** (status `DESATIVADO`, reversível).
3. Esqueci a senha — **link de redefinição com token** + página própria (não senha temporária).
4. Nome — substituir "Book Dinâmico" por **"Book"** nos textos user-facing.

## Infra reaproveitada

- `EmailAdapter` já é `@Async` com gate `app.email.habilitado`.
- `@EnableScheduling` e `@EnableAsync` já ativos na aplicação.
- Soft-delete de usuário (`UsuarioStatus.DESATIVADO`) e cap de 40 aprovados (`UsuarioService`).
- `RevogacaoTokenService` (cache Caffeine) para single-use de tokens.
- `JwtTokenProvider` usa `com.auth0.jwt` (HMAC256, `app.jwt-secret`).

---

## Item 1 — Controle de ociosidade

### Dados (migration `V7`)
- `usuario.ultimo_acesso TIMESTAMP` — atualizado a cada login bem-sucedido.
- `usuario.aviso_ociosidade_enviado_em TIMESTAMP NULL` — marca quando saiu o aviso (base do prazo).
- Backfill: `ultimo_acesso = now()` para linhas existentes (evita falso-ocioso em massa no 1º deploy).

### Fluxo
- `UsuarioService.autenticar`: ao logar, `ultimoAcesso = now()` e `avisoOciosidadeEnviadoEm = null`.
- `OciosidadeService` + job `@Scheduled(cron = ${app.ociosidade.cron})`, diário:
  1. **Avisar:** `status=APROVADO`, `role=USUARIO`, `ultimoAcesso < now − N meses`, `aviso IS NULL`
     → `EmailAdapter.enviarAvisoOciosidade(usuario, link)` + grava `aviso = now()`.
  2. **Remover:** `aviso IS NOT NULL` e `now ≥ aviso + 2 dias úteis` → `status = DESATIVADO`
     (opcional: `enviarRemocaoOciosidade`). Login/clique já teriam zerado `aviso`.
- **Manter acesso:** link no e-mail → página `/manter-acesso?token=` → `POST /autenticacao/manter-acesso {token}`
  → zera `aviso` e seta `ultimoAcesso = now()`. Token de ação `purpose=MANTER_ACESSO`.
- **ADMIN isento.** Dias úteis = pula sáb/dom (feriados fora de escopo) via helper testável.

### Config (`application.yml`)
```
app.ociosidade.habilitado=${BOOK_OCIOSIDADE_HABILITADO:true}
app.ociosidade.meses-inatividade=${BOOK_OCIOSIDADE_MESES:4}
app.ociosidade.dias-uteis-prazo=${BOOK_OCIOSIDADE_DIAS_UTEIS:2}
app.ociosidade.cron=${BOOK_OCIOSIDADE_CRON:0 0 8 * * *}
```

---

## Item 2 — Notificação de novas publicações

- Após sucesso em `DocumentoService.criar`, `criarLote` e `substituirArquivo`
  (nova versão = "atualização", citada no pedido) → e-mail assíncrono a **todos `APROVADO`**.
- **Não** dispara em `atualizarMetadados` nem `deletar`.
- `criarLote` = **1 e-mail por ação** (não N). Texto genérico fornecido no pedido.
- `UsuarioRepository.findByStatus(APROVADO)`; `EmailAdapter.enviarNovaPublicacao(usuario)`.
- Falha de e-mail não quebra o upload (`@Async` + gate).

---

## Item 3 — "Esqueci minha senha" (público)

- Backend (rotas públicas em `SecurityConfig`):
  - `POST /autenticacao/esqueci-senha {email}` → **sempre 200/204** (não vaza existência).
    Se usuário existe + `APROVADO` + e-mail habilitado → token `purpose=RESET_SENHA` (exp 30 min)
    → `EmailAdapter.enviarLinkRedefinicaoSenha(usuario, link)` com `${app.url-front-end}/redefinir-senha?token=`.
  - `POST /autenticacao/redefinir-senha {token, novaSenha}` → valida token (assinatura+purpose+exp+single-use)
    → troca `senhaHash` → revoga o token (single-use).
- Frontend (grupo `(auth)`):
  - Link "Esqueci minha senha" na tela de login.
  - Página `/esqueci-senha` (form e-mail → mensagem neutra "se existir, enviamos instruções").
  - Página `/redefinir-senha?token=` (form nova senha + confirmação → sucesso → voltar ao login).
- Reset autenticado atual (menu do usuário) **permanece**.

---

## Item 4 — Renomear "Book Dinâmico" → "Book"

Trocar nos textos **user-facing**:
- Frontend: `app/layout.tsx` (título/descrição), `dashboard/page.tsx`, `upload-book/page.tsx`,
  `components/Sidebar.tsx` (nav + marca).
- Backend: `EmailAdapter` (assuntos/corpos), `messages.properties`, default `nome-sistema` no `application.yml`.

**Não** renomear identificadores de código (package/classe/repositório Git) — churn sem ganho.
Exemplos de nome de arquivo em `utils.ts` permanecem (são nomes reais de arquivo, não a marca).

---

## Transversais

- **Tokens de ação** (`TokenAcaoService`): mesmos algoritmo/secret do `JwtTokenProvider`, claim `purpose`,
  validação por purpose, single-use via `RevogacaoTokenService`. Stateless (sem tabela).
- **SecurityConfig:** liberar `POST /autenticacao/{esqueci-senha,redefinir-senha,manter-acesso}`.
- **AuthContext (frontend):** incluir as rotas públicas novas no guard (senão redireciona ao login).
- **Next 16:** ler docs locais (`node_modules/next/dist/docs/`) antes das páginas que leem `?token=`
  (Suspense/`useSearchParams`).
- **Testes:** smoke (fase 6) dos endpoints públicos novos + unit do cálculo de dias úteis.

## Fora de escopo

- Calendário de feriados no cálculo de dias úteis.
- Status dedicado "removido por ociosidade" (reusa `DESATIVADO`).
- Renomear package/classe/repositório.
