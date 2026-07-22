# Book Dinâmico

## Rodar manualmente no Windows

Abra dois terminais PowerShell.

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

O Spring carrega `backend\.env` automaticamente quando o comando roda dentro da pasta `backend`.

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

O Next usa `frontend\.env.local`.

URLs esperadas:

```text
Backend:  http://localhost:8082/book_dinamico
Frontend: http://localhost:3001
```

Este repositório é composto por **Backend (Spring Boot)** e **Frontend (Next.js)**, cada um com suas próprias variáveis de ambiente e formas de configuração.

---

## Backend – Spring Boot

### Tecnologias Utilizadas

- **Java:** 17
- **Framework:** Spring Boot
- **Banco de Dados:** PostgreSQL



Este projeto utiliza **variáveis de ambiente** para configuração da aplicação Spring Boot. Este README descreve cada variável utilizada e fornece um **exemplo de configuração para ambiente DEV**.

---

## Configurações da Aplicação (`app`)

| Variável de Ambiente | Descrição | Exemplo (DEV) |
|---------------------|-----------|---------------|
| `BOOK_AMBIENTE` | Ambiente de execução da aplicação | `DEV` |
| `BOOK_NOME_SISTEMA` | Nome do sistema | `Book Dinâmico` |
| `BOOK_URL_SITE` | URL do backend | `http://localhost:8082/book_dinamico` |
| `BOOK_URL_FRONT_END` | URL do frontend | `http://localhost:3001` |
| `BOOK_DIRETORIO` | Diretório de armazenamento dos uploads (externo ao projeto) | `/var/dados/book-dinamico-arquivos` |

> **JWT Secret**
>
> Definido via variável de ambiente (string longa e aleatória):
>
> ```
> BOOK_CHAVE_JWT_SECRET=substitua-por-string-longa-aleatoria
> ```

---

## Configurações do Servidor (`server`)

| Variável de Ambiente | Descrição | Exemplo (DEV) |
|---------------------|-----------|---------------|
| `BOOK_APP_PORT` | Porta da aplicação | `8082` |
| `BOOK_APP_CONTEXT_PATH` | Context path da aplicação | `/book_dinamico` |
| `BOOK_APP_NAME` | Nome da aplicação Spring | `book_dinamico_backend` |

---

## Configurações de Banco de Dados (`datasource`)

| Variável de Ambiente | Descrição | Exemplo (DEV) |
|---------------------|-----------|---------------|
| `BOOK_DB_URL` | URL de conexão com o PostgreSQL | `jdbc:postgresql://localhost:5432/book_dinamico` |
| `BOOK_DB_USERNAME` | Usuário do banco | `postgres` |
| `BOOK_DB_PASSWORD` | Senha do banco | `1234` |

O pool de conexões utiliza **HikariCP** com parâmetros já definidos em configuração.

---

## Configurações de E-mail (`spring.mail`)

| Variável de Ambiente | Descrição | Exemplo (DEV) |
|---------------------|-----------|---------------|
| `BOOK_EMAIL_HABILITADO` | Liga/desliga o envio de e-mail | `false` |
| `BOOK_MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `BOOK_MAIL_PORT` | Porta SMTP | `587` |
| `BOOK_MAIL_USERNAME` | Usuário do e-mail | `contato@tcia.com.br` |
| `BOOK_MAIL_PASSWORD` | Senha do e-mail | `#########` |

---

## Perfil Spring

- Flyway habilitado com `baseline-on-migrate=true` e `out-of-order=true`

---

## Frontend – Next.js

### Configurações do Frontend (`.env.local`)

O frontend utiliza apenas um arquivo `.env.local` com as variáveis abaixo:

```env
PORT=3001
NEXT_PUBLIC_API_URL=http://localhost:8082/book_dinamico
```

- `PORT`: Porta em que o frontend será executado
- `NEXT_PUBLIC_API_URL`: URL base da API do backend

> Variáveis iniciadas com `NEXT_PUBLIC_` ficam expostas no browser.

---

## Exemplo de Configuração (application.yml – DEV)

```env
BOOK_AMBIENTE=DEV
BOOK_APP_CONTEXT_PATH=/book_dinamico
BOOK_APP_NAME=book_dinamico_backend
BOOK_APP_PORT=8082

BOOK_DB_URL=jdbc:postgresql://localhost:5432/book_dinamico
BOOK_DB_USERNAME=postgres
BOOK_DB_PASSWORD=1234

BOOK_EMAIL_HABILITADO=false
BOOK_MAIL_HOST=smtp.gmail.com
BOOK_MAIL_PORT=587
BOOK_MAIL_USERNAME=contato@tcia.com.br
BOOK_MAIL_PASSWORD=#########

BOOK_NOME_SISTEMA=Book Dinâmico
BOOK_URL_FRONT_END=http://localhost:3001
BOOK_URL_SITE=http://localhost:8082/book_dinamico
BOOK_DIRETORIO=/var/dados/book-dinamico-arquivos
BOOK_CHAVE_JWT_SECRET=substitua-por-string-longa-aleatoria
```

---

## Observações

- Nunca versionar arquivos `.env` com senhas reais.
- Para produção, utilize variáveis de ambiente configuradas no servidor ou orquestrador (Docker/Kubernetes).
