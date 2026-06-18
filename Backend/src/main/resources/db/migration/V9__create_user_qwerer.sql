UPDATE usuario
SET nome = 'Qwerer',
    empresa = 'TCIA',
    email = 'qwerer',
    senha_hash = crypt('qwerer', gen_salt('bf')),
    justificativa = 'Usuario base do sistema.',
    status = 'APROVADO',
    id_perfil = (SELECT id FROM perfil WHERE nome_perfil = 'ADMIN'),
    atualizado_em = NOW()
WHERE email = 'admin@claro.com.br';

INSERT INTO usuario (
    id, nome, empresa, email, senha_hash, justificativa, status, id_perfil, criado_em, atualizado_em
)
SELECT nextval('usuario_seq'), 'Qwerer', 'TCIA', 'qwerer', crypt('qwerer', gen_salt('bf')),
       'Usuario base do sistema.', 'APROVADO', p.id, NOW(), NOW()
FROM perfil p
WHERE p.nome_perfil = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'qwerer');

DELETE FROM usuario_perfil
WHERE usuario_id IN (SELECT id FROM usuario WHERE email = 'qwerer');

INSERT INTO usuario_perfil (id, usuario_id, perfil_id)
SELECT nextval('usuario_perfil_seq'), u.id, p.id
FROM usuario u
JOIN perfil p ON p.nome_perfil = 'ADMIN'
WHERE u.email = 'qwerer';

SELECT setval('usuario_seq', (SELECT COALESCE(MAX(id), 1) FROM usuario), true);
