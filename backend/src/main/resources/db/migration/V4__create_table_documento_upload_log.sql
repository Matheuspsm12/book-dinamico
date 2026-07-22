-- Log de uploads (RN32).
-- Substitui o @Auditar AOP do TCIA (decisão D5).
-- Persiste tanto uploads iniciais quanto substituições de versão (RN25).

CREATE TABLE documento_upload_log (
    id            BIGINT       NOT NULL DEFAULT nextval('documento_upload_log_seq'),
    documento_id  BIGINT       NOT NULL,
    usuario_id    BIGINT       NOT NULL,
    nome_arquivo  VARCHAR(500) NOT NULL,
    datetime      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_documento_upload_log PRIMARY KEY (id),
    CONSTRAINT fk_dul_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
    CONSTRAINT fk_dul_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuario (id)
);

CREATE INDEX ix_dul_documento_datetime ON documento_upload_log (documento_id, datetime DESC);
