# Book Dinâmico – Backend

API Spring Boot para distribuição controlada de books dinâmicos da Claro/Logística, com autenticação JWT, controle de usuários/perfis/permissões e integração com PostgreSQL via JPA. Inclui documentação OpenAPI, migrações Flyway e upload de documentos com validação de integridade.

## Stack
- Java 17, Spring Boot 3.4 (Web, Data JPA, Security, Validation, Mail)
- PostgreSQL + Flyway para versionamento de banco
- MapStruct e Lombok para mapeamento e redução de boilerplate
- Springdoc OpenAPI para documentação e UI do Swagger

## Arquitetura e Pastas
- `src/main/java/com/tcia/book_dinamico_back_end/api`: controllers, requests e responses expostos pela API.
- `src/main/java/com/tcia/book_dinamico_back_end/domain`: models, services, repositories, specifications e exceptions.
- `src/main/java/com/tcia/book_dinamico_back_end/infrastructure`: segurança (JWT), configurações, mappers MapStruct e adapters.
- `src/main/java/com/tcia/book_dinamico_back_end/core`: enums, utilitários e anotações.
- `src/main/resources`: `application.yml` e migrações Flyway em `db/migration`.
- `src/test/java`: testes JUnit 5 espelhando os pacotes principais.

## Pré-requisitos
- JDK 17
- Maven Wrapper (já incluso: `./mvnw`)
- PostgreSQL 14+ com um banco criado para a aplicação

## Variáveis de Ambiente Principais
- `BOOK_APP_PORT` e `BOOK_APP_CONTEXT_PATH`: porta e prefixo do contexto (ex.: `/book_dinamico`).
- `BOOK_DB_URL`, `BOOK_DB_USERNAME`, `BOOK_DB_PASSWORD`: conexão com o Postgres.
- `BOOK_AMBIENTE`, `BOOK_APP_NAME`, `BOOK_NOME_SISTEMA`, `BOOK_URL_SITE`, `BOOK_URL_FRONT_END`.
- `BOOK_DIRETORIO`: diretório de armazenamento dos uploads — deve ficar **fora** do projeto (volume/disco externo).
- `BOOK_MAIL_HOST`, `BOOK_MAIL_PORT`, `BOOK_MAIL_USERNAME`, `BOOK_MAIL_PASSWORD`.
- `BOOK_CHAVE_JWT_SECRET`: chave usada para assinar tokens (substitua o valor default em produção).
- `BOOK_PERMITIR_LOGIN_LOCAL`: habilita/desabilita o login local (`true`/`false`).
- `BOOK_SCHEDULING`: expressão cron do agendamento de processamento (default `0 */10 * * * *` — a cada 10 min).
- `BOOK_OCIOSIDADE_SCHEDULING`: expressão cron da verificação de ociosidade de usuários (default `0 0 3 * * *` — diariamente às 3h).

## Como Rodar Localmente
1. Configure as variáveis de ambiente acima e crie o banco `book_dinamico`.
2. Execute `./mvnw spring-boot:run` para iniciar (as migrações Flyway rodam ao subir o app).
3. Build completo: `./mvnw clean package` gera o jar em `target/`. Testes isolados: `./mvnw test`.

- API: `http://localhost:8082/book_dinamico`
- Swagger: `http://localhost:8082/book_dinamico/swagger-ui.html`
