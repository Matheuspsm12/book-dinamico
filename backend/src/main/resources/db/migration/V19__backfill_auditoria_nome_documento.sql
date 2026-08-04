-- Limpa o histórico antigo de documentos: antes, o aspecto @Auditar gravava em
-- "detalhes" os argumentos serializados do método (ex.: [6] ou
-- [7,{"nome":"Lição","descricao":...}]) ou nulo. Agora "detalhes" guarda só o
-- nome do documento. Este backfill normaliza os registros antigos.

-- 1) Registros de edição antigos traziam o JSON com o nome embutido: extrai o nome.
UPDATE auditoria
   SET detalhes = (regexp_match(detalhes, '"nome"\s*:\s*"([^"]*)"'))[1]
 WHERE entidade = 'DOCUMENTO'
   AND detalhes LIKE '[%'
   AND detalhes LIKE '%"nome"%';

-- 2) Registros nulos ou só com o id (ex.: [6]): usa o nome atual do documento,
--    quando ele ainda existe.
UPDATE auditoria a
   SET detalhes = d.nome
  FROM documento d
 WHERE a.entidade = 'DOCUMENTO'
   AND a.entidade_id = d.id
   AND (a.detalhes IS NULL OR (a.detalhes LIKE '[%' AND a.detalhes NOT LIKE '%"nome"%'));
