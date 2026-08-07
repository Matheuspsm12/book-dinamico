-- Amplia as extensões de Excel aceitas: além de XLSX/XLSM, passam a valer
-- XLSB, XLTX e XLTM (todos do tipo EXCEL). PPTX (POWERPOINT) permanece.
ALTER TABLE documento DROP CONSTRAINT IF EXISTS ck_documento_extensao;
ALTER TABLE documento DROP CONSTRAINT IF EXISTS ck_documento_tipo_extensao;

ALTER TABLE documento
    ADD CONSTRAINT ck_documento_extensao
    CHECK (extensao IN ('XLSX', 'XLSM', 'XLSB', 'XLTX', 'XLTM', 'PPTX'));

ALTER TABLE documento
    ADD CONSTRAINT ck_documento_tipo_extensao CHECK (
        (tipo = 'POWERPOINT' AND extensao = 'PPTX') OR
        (tipo = 'EXCEL'      AND extensao IN ('XLSX', 'XLSM', 'XLSB', 'XLTX', 'XLTM'))
    );
