-- Tabela usuario.
-- Sem coluna ativo / sem @SQLDelete: status=DESATIVADO já cobre soft-delete (decisão de schema).
-- aprovado_por: self-FK para o admin que decidiu (RN09).
-- Cap de 40 APROVADO é enforced pelo serviço, não por constraint (N2 + A10).

CREATE TABLE usuario (
    id              BIGINT       NOT NULL DEFAULT nextval('usuario_seq'),
    nome            VARCHAR(200) NOT NULL,
    empresa         VARCHAR(200) NOT NULL,
    email           VARCHAR(200) NOT NULL,
    senha_hash      VARCHAR(255) NOT NULL,
    justificativa   TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    aprovado_por    BIGINT       NULL,
    decidido_em     TIMESTAMP    NULL,

    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_aprovado_por FOREIGN KEY (aprovado_por) REFERENCES usuario (id),
    CONSTRAINT ck_usuario_status CHECK (status IN ('PENDENTE', 'APROVADO', 'REJEITADO', 'DESATIVADO')),
    CONSTRAINT ck_usuario_role   CHECK (role   IN ('ADMIN', 'USUARIO'))
);

CREATE INDEX ix_usuario_status_empresa ON usuario (status, empresa);
