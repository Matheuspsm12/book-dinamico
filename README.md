# Claro Book Dinâmico

Portal para distribuição controlada de _books_ dinâmicos (planilhas e apresentações) da Claro/Logística.
Usuários se cadastram, um administrador aprova o acesso, e os aprovados visualizam e baixam os documentos
publicados. O administrador faz o upload, a substituição de versões e a curadoria dos arquivos.

Monorepo com **backend Spring Boot** (`Backend/`) e **frontend Next.js** (`Frontend/`),
seguindo o padrão dos projetos `reversa-claro-devolucao` da TCIA.

---

## Arquitetura

```
backend-upload-book/
├── Backend/                                         # Backend Spring Boot
│   ├── src/main/java/com/tcia/book_dinamico_back_end/
│   │   ├── api/             # controller / request / response (camada de entrada)
│   │   ├── core/            # annotation / enums / util (transversal)
│   │   ├── domain/          # model / repository / service / specification / exception
│   │   └── infrastructure/  # config / security / mapper / adapter
│   ├── src/test/java/...    # JUnit 5
│   ├── src/main/resources/
│   │   ├── db/migration/    # Flyway (V1..V8)
│   │   ├── application.yaml application-dev.yaml
│   │   └── ValidationMessages / messages / errors .properties  # i18n pt-BR
│   ├── mvnw mvnw.cmd .mvn/
│   └── README.md
└── Frontend/            # Frontend Next.js (ver README próprio)
```

## Stack

**Backend** — Java 17 · Spring Boot 3.4 · PostgreSQL · Flyway · Spring Security + JWT (auth0 `java-jwt`,
HMAC256, expiry 30 min) · MapStruct · springdoc-openapi (Swagger) · Lombok.

**Frontend** — Next.js 15 · React 18 · TypeScript · Tailwind CSS v3 (shadcn) · axios + interceptors ·
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

> Pré-requisitos: **JDK 17**, **Node 20+**, **PostgreSQL** e **Maven**.

### 1. Banco de dados

Crie o banco `book_dinamico` no PostgreSQL local. O Flyway aplica as migrations (`V1..V6`) no boot,
incluindo o seed do admin.

### 2. Variáveis de ambiente

Configure as variáveis de ambiente (URL/usuário/senha do banco, `BOOK_DIRETORIO`, `BOOK_JWT_SECRET`,
SMTP). A lista completa está em [`Backend/README.md`](Backend/README.md).

### 3. Backend

```bash
cd Backend
mvn clean package -DskipTests
java -jar target/book_dinamico_backend-0.0.1-SNAPSHOT.jar
```

- API: `http://localhost:8082/book_dinamico`
- Swagger: `http://localhost:8082/book_dinamico/swagger-ui.html`

### 4. Frontend

```bash
cd Frontend
npm install
npm run dev      # http://localhost:3000
```

O frontend consome a API em `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8082/book_dinamico`).

### Admin inicial (seed)

| E-mail | Senha | Papel |
| --- | --- | --- |
| `admin@claro.com.br` | `admin` | `ADMIN` |

> Credencial de desenvolvimento — **trocar antes de produção**.

---

## Branches

- `master` — estável.
- `develop` — integração contínua do desenvolvimento.
