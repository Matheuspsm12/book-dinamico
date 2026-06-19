CREATE TABLE dominio (
    dominio_chave        VARCHAR(60)  NOT NULL,
    dominio_valor        VARCHAR(370),
    dominio_descricao    TEXT,
    CONSTRAINT pk_dominio PRIMARY KEY (dominio_chave)
);

INSERT INTO dominio (dominio_chave, dominio_valor, dominio_descricao)
VALUES ('PROCESSAMENTO_HABILITADO', 'true', 'Liga/desliga o scheduler de processamento de documentos.');
