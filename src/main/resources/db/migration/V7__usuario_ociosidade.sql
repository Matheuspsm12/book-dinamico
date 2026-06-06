-- Controle de ociosidade de usuários (Item 1).
-- ultimo_acesso: atualizado a cada login bem-sucedido; base pra detectar inatividade (4 meses).
-- aviso_ociosidade_enviado_em: marca quando o e-mail de aviso saiu (base do prazo de 2 dias úteis).
-- Backfill: ultimo_acesso = NOW() pra base existente (evita falso-ocioso em massa no 1º deploy).

ALTER TABLE usuario ADD COLUMN ultimo_acesso               TIMESTAMP NULL;
ALTER TABLE usuario ADD COLUMN aviso_ociosidade_enviado_em TIMESTAMP NULL;

UPDATE usuario SET ultimo_acesso = NOW() WHERE ultimo_acesso IS NULL;

CREATE INDEX ix_usuario_ociosidade ON usuario (status, ultimo_acesso);
