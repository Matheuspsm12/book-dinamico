# Claro Book Dinâmico

Portal para distribuição controlada de _books_ dinâmicos (planilhas e apresentações) da Claro/Logística.
Usuários se cadastram, um administrador aprova o acesso, e os aprovados visualizam e baixam os documentos
publicados. O administrador faz o upload, a substituição de versões e a curadoria dos arquivos.

Monorepo com **backend Spring Boot** (raiz) e **frontend Next.js** (`reversa-claro-book-frontend/`),
seguindo o padrão dos projetos `reversa-claro-devolucao` da TCIA.

---

## Arquitetura

```
backend-upload-book/
├── src/main/java/com/tcia/book_dinamico_back_end/   # Backend Spring Boot
│   ├── config/        # SecurityConfig, JwtFilter, AppConfig
│   ├── controller/    # REST + request/response/mapper (MapStruct)
│   ├── service/       # Regras de negócio
│   ├── repository/    # Spring Data JPA + specifications
│   ├── entity/        # Entidades JPA
│   ├── jwt/ security/ # Emissão/validação de JWT, UserDetails
│   ├── exception/     # Exceções de domínio + GlobalExceptionHandler
│   └── email/ enums/ utils/ annotations/
├── src/main/resources/
│   ├── db/migration/  # Flyway (V1..V6)
│   ├── application.yml application-dev.yml
│   └── ValidationMessages / messages / errors .properties  # i18n pt-BR
├── reversa-claro-book-frontend/    # Frontend Next.js (ver README próprio)
├── docker-compose.yml  Dockerfile  render.yaml
└── .env.example
```

## Stack

**Backend** — Java 17 · Spring Boot 3.4 · PostgreSQL · Flyway · Spring Security + JWT (auth0 `java-jwt`,
HMAC256, expiry 30 min) · MapStruct · springdoc-openapi (Swagger) · Lombok.

**Frontend** — Next.js 16 · React 19 · TypeScript · Tailwind CSS v3 (shadcn) · axios + interceptors ·
`@tanstack/react-query` · `sonner`.

## Funcionalidades

- **Autocadastro** (status inicial `PENDENTE`) e **login** com mensagens distintas por status
  (`PENDENTE` / `REJEITADO` / `DESATIVADO` / credenciais inválidas).
- **Aprovação/rejeição** de cadastros pelo admin, com e-mail de notificação e _cap_ de **40 usuários aprovados**.
- **Gerenciamento de usuários** (listar/filtrar/editar/ativar/desativar).
- **Upload de documentos** (`.xlsm`, `.xlsx`, `.pptx`) com validação de extensão, _magic bytes_ e tamanho (≤ 60 MB),
  substituição de versão, edição de metadados, _soft delete_ e log de upload.
- **Catálogo e download** para usuários autenticados.

---

## Rodando localmente

> Pré-requisitos: **JDK 17**, **Node 20+**, **PostgreSQL** e **Maven** (ou Docker).

### 1. Banco de dados

Crie o banco `book_dinamico` no PostgreSQL local. O Flyway aplica as migrations (`V1..V6`) no boot,
incluindo o seed do admin.

### 2. Variáveis de ambiente

Copie `.env.example` para `.env` e ajuste (URL/usuário/senha do banco, `BOOK_DIRETORIO`, `BOOK_JWT_SECRET`,
SMTP). **Nunca** commite o `.env` — ele já está no `.gitignore`.

### 3. Backend

```bash
mvn clean package -DskipTests
java -jar target/book_dinamico_backend-0.0.1-SNAPSHOT.jar
```

- API: `http://localhost:8082/book_dinamico`
- Swagger: `http://localhost:8082/book_dinamico/swagger-ui.html`

### 4. Frontend

```bash
cd reversa-claro-book-frontend
npm install
npm run dev      # http://localhost:3000
```

O frontend consome a API em `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8082/book_dinamico`).

### Docker (alternativa)

```bash
docker compose up --build
```

Sobe Postgres + backend + frontend já configurados.

### Admin inicial (seed)

| E-mail | Senha | Papel |
| --- | --- | --- |
| `admin@claro.com.br` | `admin` | `ADMIN` |

> Credencial de desenvolvimento — **trocar antes de produção**.

---

## Branches

- `master` — estável.
- `develop` — integração contínua do desenvolvimento.
