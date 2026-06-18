-- Tabela documento.
-- Soft-delete via flag 'ativo' + @SQLDelete na entidade (padrão TCIA).
-- data_atualizacao é campo manual do admin (A6) — distinto de atualizado_em (auto via JPA auditing).
-- tipo/extensao com check constraints (RN21).

CREATE TABLE documento (
    id                     BIGINT       NOT NULL DEFAULT nextval('documento_seq'),
    nome                   VARCHAR(255) NOT NULL,
    descricao              TEXT         NOT NULL,
    tipo                   VARCHAR(20)  NOT NULL,
    extensao               VARCHAR(10)  NOT NULL,
    caminho_armazenamento  VARCHAR(500) NOT NULL,
    tamanho_bytes          BIGINT       NOT NULL,
    data_atualizacao       DATE         NOT NULL,
    criado_em              TIMESTAMP    NOT NULL DEFAULT NOW(),
    criado_por             BIGINT       NOT NULL,
    atualizado_em          TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_por         BIGINT       NOT NULL,
    ativo                  BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_documento PRIMARY KEY (id),
    CONSTRAINT fk_documento_criado_por     FOREIGN KEY (criado_por)     REFERENCES usuario (id),
    CONSTRAINT fk_documento_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuario (id),
    CONSTRAINT ck_documento_tipo     CHECK (tipo     IN ('POWERPOINT', 'EXCEL')),
    CONSTRAINT ck_documento_extensao CHECK (extensao IN ('XLSM', 'XLSX', 'PPTX')),
    CONSTRAINT ck_documento_tipo_extensao CHECK (
        (tipo = 'POWERPOINT' AND extensao = 'PPTX') OR
        (tipo = 'EXCEL'      AND extensao IN ('XLSM', 'XLSX'))
    ),
    CONSTRAINT ck_documento_tamanho CHECK (tamanho_bytes > 0)
);

CREATE INDEX ix_documento_ativo_atualizado_em ON documento (ativo, atualizado_em DESC);
