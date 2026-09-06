-- Suporta a consulta do processamento mais recente por documento:
-- findFirstByDocumentoIdOrderByDataStartDescIdDesc.
CREATE INDEX ix_processamento_documento_data_start_id
    ON processamento (documento_id, data_start DESC, id DESC);
