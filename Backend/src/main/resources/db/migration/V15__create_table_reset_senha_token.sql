CREATE SEQUENCE IF NOT EXISTS reset_senha_token_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE reset_senha_token (
    id          BIGINT       NOT NULL DEFAULT nextval('reset_senha_token_seq'),
    token       VARCHAR(255) NOT NULL UNIQUE,
    usuario_id  BIGINT       NOT NULL,
    expira_em   TIMESTAMP    NOT NULL,
    usado       BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_reset_senha_token PRIMARY KEY (id),
    CONSTRAINT fk_reset_senha_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX idx_reset_senha_token_token ON reset_senha_token (token);
