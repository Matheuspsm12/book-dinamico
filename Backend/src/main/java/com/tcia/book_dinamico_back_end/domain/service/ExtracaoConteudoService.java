package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.core.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.domain.exception.ArquivoException;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Documento;
import com.tcia.book_dinamico_back_end.domain.model.DocumentoAba;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoAbaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Log4j2
@Service
@RequiredArgsConstructor
public class ExtracaoConteudoService {

    private final DocumentoAbaRepository documentoAbaRepository;

    public void extrair(Processamento processamento) {
        Documento documento = processamento.getDocumento();
        if (documento == null) {
            throw new NegocioException("Processamento sem documento associado.");
        }

        String caminho = processamento.getArquivoAProcessar();
        documentoAbaRepository.deleteByDocumentoId(documento.getId());

        try (InputStream in = new FileInputStream(caminho)) {
            if (documento.getExtensao() == ExtensaoDocumento.PPTX) {
                extrairApresentacao(documento, in);
            } else {
                extrairPlanilha(documento, in);
            }
        } catch (EncryptedDocumentException e) {
            throw new NegocioException("Arquivo protegido por senha.");
        } catch (IOException e) {
            throw new ArquivoException("Falha ao ler o arquivo para extracao.", e);
        }
    }

    private void extrairPlanilha(Documento documento, InputStream in) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                int linhas = sheet.getPhysicalNumberOfRows() > 0 ? sheet.getLastRowNum() + 1 : 0;
                Row cabecalho = sheet.getRow(sheet.getFirstRowNum());
                int colunas = cabecalho != null ? Math.max(cabecalho.getLastCellNum(), 0) : 0;

                documentoAbaRepository.save(DocumentoAba.builder()
                        .documentoId(documento.getId())
                        .nomeAba(sheet.getSheetName())
                        .qtdLinhas(linhas)
                        .qtdColunas(colunas)
                        .build());
            }
        }
    }

    private void extrairApresentacao(Documento documento, InputStream in) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(in)) {
            documentoAbaRepository.save(DocumentoAba.builder()
                    .documentoId(documento.getId())
                    .nomeAba("Apresentacao")
                    .qtdLinhas(ppt.getSlides().size())
                    .qtdColunas(0)
                    .build());
        }
    }
}
