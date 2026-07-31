-- Passa a existir apenas 3 perfis: ADMIN, OPERADOR e LOGISTICA.
-- USUARIO (default do autocadastro) é renomeado para OPERADOR; "Logística" vira "LOGISTICA".
-- Os perfis de negócio criados na V17 (Vendedor, Gerentes, TCIA) são descontinuados.
UPDATE perfil SET nome_perfil = 'OPERADOR', descricao = 'Operador do portal'
 WHERE nome_perfil = 'USUARIO';

UPDATE perfil SET nome_perfil = 'LOGISTICA', descricao = 'Logística'
 WHERE nome_perfil = 'Logística';

-- Reassocia usuários que estavam nos perfis descontinuados para OPERADOR
-- (evita violação da FK usuario.id_perfil ao remover os perfis).
UPDATE usuario SET id_perfil = (SELECT id FROM perfil WHERE nome_perfil = 'OPERADOR')
 WHERE id_perfil IN (SELECT id FROM perfil
       WHERE nome_perfil IN ('Vendedor', 'Gerente de Loja', 'Gerente Regional', 'Gerente Nacional', 'TCIA'));

-- Remove os perfis descontinuados; perfil_permissao e usuario_perfil caem via ON DELETE CASCADE.
DELETE FROM perfil
 WHERE nome_perfil IN ('Vendedor', 'Gerente de Loja', 'Gerente Regional', 'Gerente Nacional', 'TCIA');
