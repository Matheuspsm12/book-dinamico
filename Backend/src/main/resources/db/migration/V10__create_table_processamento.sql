CREATE SEQUENCE IF NOT EXISTS processamento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE processamento (
    id                  BIGINT       NOT NULL DEFAULT nextval('processamento_seq'),
    nome_arquivo        VARCHAR(500) NOT NULL,
    content_type        VARCHAR(150),
    data_start          TIMESTAMP    NOT NULL DEFAULT NOW(),
    data_inicio         TIMESTAMP,
    data_fim            TIMESTAMP,
    tipo_processamento  INTEGER      NOT NULL,
    executado           BOOLEAN      NOT NULL DEFAULT FALSE,
    reprocessar         BOOLEAN      NOT NULL DEFAULT FALSE,
    qtd_reprocessar     INTEGER      NOT NULL DEFAULT 0,
    qtd_reprocessado    INTEGER      NOT NULL DEFAULT 0,
    resultado           TEXT,
    resultado_amigavel  VARCHAR(200),
    parametro           VARCHAR(500),
    arquivo_a_processar VARCHAR(500),
    arquivo_processado  VARCHAR(500),
    tamanho             VARCHAR(200),
    usuario_id          BIGINT       NOT NULL,
    documento_id        BIGINT,

    CONSTRAINT pk_processamento PRIMARY KEY (id),
    CONSTRAINT fk_processamento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_processamento_documento FOREIGN KEY (documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_processamento_tipo ON processamento (tipo_processamento);
CREATE INDEX ix_processamento_data_inicio ON processamento (data_inicio DESC);
CREATE INDEX ix_processamento_documento ON processamento (documento_id);
