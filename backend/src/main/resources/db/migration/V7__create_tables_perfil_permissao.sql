-- Base de acesso padrão TCIA (definida pelo Efren): perfil / permissao / perfil_permissao / usuario_perfil.
-- Sequences per-tabela (padrão TCIA: allocationSize=1 no @SequenceGenerator).
CREATE SEQUENCE IF NOT EXISTS perfil_seq START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS permissao_seq START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS perfil_permissao_seq START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS usuario_perfil_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS perfil (
    id           BIGINT       NOT NULL PRIMARY KEY,
    nome_perfil  VARCHAR(255) NOT NULL UNIQUE,
    descricao    TEXT,
    data_criacao TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativado      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS permissao (
    id             BIGINT       NOT NULL PRIMARY KEY,
    nome_permissao VARCHAR(255) NOT NULL UNIQUE,
    descricao      TEXT,
    data_criacao   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS perfil_permissao (
    id           BIGINT NOT NULL PRIMARY KEY,
    perfil_id    BIGINT NOT NULL REFERENCES perfil(id) ON DELETE CASCADE,
    permissao_id BIGINT NOT NULL REFERENCES permissao(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS usuario_perfil (
    id         BIGINT NOT NULL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    perfil_id  BIGINT NOT NULL REFERENCES perfil(id) ON DELETE CASCADE
);
