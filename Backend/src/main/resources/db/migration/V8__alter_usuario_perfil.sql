-- Substitui o enum role da usuario pela base de perfil (padrão TCIA / Efren).

-- 1) Perfis base do Book Dinâmico
INSERT INTO perfil (id, nome_perfil, descricao, data_criacao, ativado)
VALUES (nextval('perfil_seq'), 'ADMIN', 'Administrador do portal', CURRENT_TIMESTAMP, TRUE),
       (nextval('perfil_seq'), 'USUARIO', 'Usuário do portal', CURRENT_TIMESTAMP, TRUE);

-- 2) Coluna id_perfil na usuario
ALTER TABLE usuario ADD COLUMN id_perfil BIGINT;

-- 3) Backfill a partir do enum role atual
UPDATE usuario SET id_perfil = (SELECT id FROM perfil WHERE nome_perfil = 'ADMIN')
 WHERE role = 'ADMIN';
UPDATE usuario SET id_perfil = (SELECT id FROM perfil WHERE nome_perfil = 'USUARIO')
 WHERE role <> 'ADMIN' OR role IS NULL;

-- 4) Restrições
ALTER TABLE usuario ALTER COLUMN id_perfil SET NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_perfil FOREIGN KEY (id_perfil) REFERENCES perfil(id);

-- 5) Espelha em usuario_perfil (base do Efren)
INSERT INTO usuario_perfil (id, usuario_id, perfil_id)
SELECT nextval('usuario_perfil_seq'), u.id, u.id_perfil FROM usuario u;

-- 6) Remove o enum role
ALTER TABLE usuario DROP COLUMN role;
