CREATE SEQUENCE IF NOT EXISTS auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE auditoria (
    id           BIGINT       NOT NULL DEFAULT nextval('auditoria_seq'),
    usuario      VARCHAR(255) NOT NULL,
    acao         VARCHAR(255) NOT NULL,
    entidade     VARCHAR(255),
    entidade_id  BIGINT,
    detalhes     TEXT,
    data_hora    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_auditoria PRIMARY KEY (id)
);
