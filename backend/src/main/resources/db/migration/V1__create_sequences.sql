-- Sequências per-tabela. Padrão TCIA: allocationSize=1 no @SequenceGenerator.
-- (Sem CREATE EXTENSION: o usuário do banco em homolog/prod não é superuser.
--  Seeds usam hash BCrypt pré-computado; busca acento-insensível usa translate().)

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
