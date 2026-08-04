-- Passam a existir apenas 2 perfis: ADMIN e OPERADOR. LOGISTICA é descontinuado.
-- Usuários que estiverem em LOGISTICA são reassociados para OPERADOR antes da remoção
-- (evita violação da FK usuario.id_perfil).
UPDATE usuario SET id_perfil = (SELECT id FROM perfil WHERE nome_perfil = 'OPERADOR')
 WHERE id_perfil IN (SELECT id FROM perfil WHERE nome_perfil = 'LOGISTICA');

DELETE FROM perfil WHERE nome_perfil = 'LOGISTICA';
