# Deploy — Render.com (free tier)

Stack de 3 serviços orquestrada via `render.yaml`:

| Serviço | Tipo | Plano | URL prevista |
| --- | --- | --- | --- |
| `book-dinamico-db` | Postgres | free (1 GB, expira em 90 dias) | interno |
| `book-dinamico-backend` | Docker web | free (sleep 15 min) | `https://book-dinamico-backend.onrender.com` |
| `book-dinamico-frontend` | Docker web | free (sleep 15 min) | `https://book-dinamico-frontend.onrender.com` |

## Limitações conhecidas do free tier

- **Sleep após 15 min de inatividade** — cold start ~30 s no próximo request.
- **Postgres expira em 90 dias** — recriar a database e re-deploy.
- **Sem disco persistente** — uploads de arquivos (Phase 5) ficam em `/tmp` e
  **são perdidos a cada deploy ou restart do backend**. O catálogo no Postgres
  mantém as linhas, mas o binário some. Para persistir uploads, opções:
  1. Migrar `ArquivoStorageService` pra armazenar `BYTEA` na própria tabela `documento`
  2. Migrar pra object storage (Cloudflare R2 / S3)
  3. Upgrade Render pra plano com disco

## Pré-requisitos no seu lado

1. Conta no Render (gratuita, sem cartão): https://render.com/register
2. Conta no GitHub
3. Git instalado localmente (já tem)

## Passos

### 1. Criar o repo no GitHub

A árvore atual já está pronta como monorepo. Crie um repo vazio no GitHub e empurre:

```powershell
cd C:\Users\mathe\Documents\backend-upload-book
git add .
git commit -m "deploy: dockerize + render blueprint"
git branch -M main
git remote add origin https://github.com/<SEU_USUARIO>/book-dinamico.git
git push -u origin main
```

> Se quiser que eu use o `gh` CLI pra criar o repo e empurrar, me dê um PAT
> (Personal Access Token) do GitHub com escopo `repo`.

### 2. Conectar no Render

1. Em https://dashboard.render.com → **New → Blueprint**.
2. **Connect a repository** → autorize seu GitHub → escolha o repo recém-criado.
3. Render vai detectar o `render.yaml` e mostrar os 3 serviços que vai criar.
4. **Apply** — Render provisiona Postgres + builda os 2 Dockerfiles e sobe os serviços.

Tempo total esperado de primeiro deploy: ~8–12 min (build do backend Maven demora).

### 3. Acompanhar o build

- Logs dos 2 web services no dashboard do Render.
- O backend deve mostrar `Started BookDinamicoBackEndApplication` e Flyway aplicando V1–V6.
- O frontend deve mostrar `Ready in Xms` do Next.

### 4. Teste o ar

Abra `https://book-dinamico-frontend.onrender.com/login` e logue:

```
admin@claro.com.br / admin
```

(Mesma seed da V5 que roda em qualquer ambiente — local ou cloud.)

### 5. Configurações pós-deploy

#### SMTP de verdade (Phase 3 — notificações de aprovação/rejeição)

No dashboard do Render → serviço `book-dinamico-backend` → **Environment**:

```
BOOK_EMAIL_HABILITADO=true
BOOK_MAIL_HOST=smtp.gmail.com         (ou outro provider)
BOOK_MAIL_PORT=587
BOOK_MAIL_USERNAME=seu@email.com
BOOK_MAIL_PASSWORD=<app password>     (Gmail exige app password, não a senha real)
```

Save → o serviço reinicia automaticamente.

#### CORS pra um domínio próprio

Se você apontar um domínio customizado pro frontend (ex.: `book.suaempresa.com`):

```
BOOK_URL_FRONT_END=https://book.suaempresa.com,https://book-dinamico-frontend.onrender.com
```

(Aceita lista separada por vírgula — o `SecurityConfig` já desfaz por essa lógica.)

## Rodar localmente igual à produção

```powershell
cd C:\Users\mathe\Documents\backend-upload-book
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend:  http://localhost:8082/book_dinamico
- Postgres: localhost **5433** (porta diferente pra não bater com Postgres local)

Para parar: `docker compose down` (ou `-v` pra zerar volumes).

## Troubleshooting

**Backend crashou em OOM no Render?**
- Free tier dá 512 MB de RAM. `render.yaml` já configura `JAVA_OPTS=-Xmx400m -Xms128m`.
- Se ainda assim travar, reduzir Hikari pool em `application.yml` (`maximum-pool-size: 5`).

**Frontend builda mas dá 502 ao acessar?**
- Confirme que `Dockerfile` tem `ENV HOSTNAME=0.0.0.0` (Render exige bind em 0.0.0.0).
- Confirme que o Next está em modo standalone (já configurado em `next.config.ts`).

**CORS errado em produção?**
- `BOOK_URL_FRONT_END` no backend tem que conter a URL exata do frontend.
- Se rodar em mais de um domínio, lista separada por vírgula.

**Migration falhou?**
- Render mostra logs do Flyway no build. Se uma V já foi aplicada num DB que
  precisou ser recriado, faça `Reset DB` no dashboard do Postgres.

## Arquivos relevantes

- `backend/Dockerfile` — backend (multi-stage Maven 3.9 → JRE 17)
- `frontend/Dockerfile` — frontend (multi-stage Node 20 → standalone)
- `docker-compose.yml` — desenvolvimento local (3 containers)
- `render.yaml` — blueprint do deploy no Render
- `.dockerignore` (em ambos) — exclui o que não vai pra imagem
- `src/main/resources/application.yml` — fallbacks de env var pra cloud e local
