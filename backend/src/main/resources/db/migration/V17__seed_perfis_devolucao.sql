-- Perfis "de negócio" espelhando o portal Devolução (Segunda Mudança).
-- ADMIN e USUARIO permanecem: ADMIN gate a segurança (hasRole('ADMIN')) e o
-- login/guards do front; USUARIO é o placeholder do autocadastro até o admin
-- escolher o perfil real na aprovação.
INSERT INTO perfil (id, nome_perfil, descricao, data_criacao, ativado)
SELECT nextval('perfil_seq'), nome, descricao, CURRENT_TIMESTAMP, TRUE
FROM (VALUES
    ('Vendedor',        'Vendedor'),
    ('Gerente de Loja', 'Gerente de Loja'),
    ('Gerente Regional','Gerente Regional'),
    ('Gerente Nacional','Gerente Nacional'),
    ('Logística',       'Logística'),
    ('TCIA',            'TCIA')
) AS novos(nome, descricao)
WHERE NOT EXISTS (SELECT 1 FROM perfil p WHERE p.nome_perfil = novos.nome);
