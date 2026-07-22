-- Cria pgcrypto para uso em V5 (seed admin com BCrypt nativo do Postgres).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Sequências per-tabela. Padrão TCIA: allocationSize=1 no @SequenceGenerator.

CREATE SEQUENCE IF NOT EXISTS usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS documento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS documento_upload_log_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
