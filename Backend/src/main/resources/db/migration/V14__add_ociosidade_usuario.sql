ALTER TABLE usuario ADD COLUMN ultimo_acesso TIMESTAMP;
ALTER TABLE usuario ADD COLUMN ociosidade_notificado_em TIMESTAMP;

UPDATE usuario SET ultimo_acesso = criado_em WHERE ultimo_acesso IS NULL;
