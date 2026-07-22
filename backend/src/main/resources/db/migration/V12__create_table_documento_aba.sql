CREATE SEQUENCE IF NOT EXISTS documento_aba_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE documento_aba (
    id            BIGINT       NOT NULL DEFAULT nextval('documento_aba_seq'),
    documento_id  BIGINT       NOT NULL,
    nome_aba      VARCHAR(255) NOT NULL,
    qtd_linhas    INTEGER      NOT NULL DEFAULT 0,
    qtd_colunas   INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT pk_documento_aba PRIMARY KEY (id),
    CONSTRAINT fk_documento_aba_documento FOREIGN KEY (documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_documento_aba_documento ON documento_aba (documento_id);
