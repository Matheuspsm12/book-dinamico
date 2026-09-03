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
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.binary.XSSFBSheetHandler;
import org.apache.poi.xssf.eventusermodel.XSSFBReader;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

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

        try {
            if (documento.getExtensao() == ExtensaoDocumento.PPTX) {
                try (InputStream in = new FileInputStream(caminho)) {
                    extrairApresentacao(documento, in);
                }
            } else if (documento.getExtensao() == ExtensaoDocumento.XLSB) {
                extrairPlanilhaBinaria(documento, caminho);
            } else {
                try (InputStream in = new FileInputStream(caminho)) {
                    extrairPlanilha(documento, in);
                }
            }
        } catch (EncryptedDocumentException e) {
            throw new NegocioException("Arquivo protegido por senha.");
        } catch (IOException e) {
            throw new ArquivoException("Falha ao ler o arquivo para extracao.", e);
        } catch (OpenXML4JException e) {
            throw new ArquivoException("Falha ao abrir o arquivo para extracao.", e);
        }
    }

    private void extrairPlanilha(Documento documento, InputStream in) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            log.info("Extraido de '{}': {} aba(s)", documento.getNome(), workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                int linhas = sheet.getPhysicalNumberOfRows() > 0 ? sheet.getLastRowNum() + 1 : 0;
                Row cabecalho = sheet.getRow(sheet.getFirstRowNum());
                int colunas = cabecalho != null ? Math.max(cabecalho.getLastCellNum(), 0) : 0;

                log.info("  {}: {} linhas x {} colunas", sheet.getSheetName(), linhas, colunas);
                documentoAbaRepository.save(DocumentoAba.builder()
                        .documentoId(documento.getId())
                        .nomeAba(sheet.getSheetName())
                        .qtdLinhas(linhas)
                        .qtdColunas(colunas)
                        .build());
            }
        }
    }

    private void extrairPlanilhaBinaria(Documento documento, String caminho)
            throws IOException, OpenXML4JException {

        int abas = 0;
        try (OPCPackage pkg = OPCPackage.open(Path.of(caminho).toFile())) {
            XSSFBReader reader = new XSSFBReader(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

            while (sheets.hasNext()) {
                abas++;
                try (InputStream sheetStream = sheets.next()) {
                    SheetStats stats = new SheetStats();
                    XSSFBSheetHandler handler = new XSSFBSheetHandler(
                            sheetStream,
                            reader.getXSSFBStylesTable(),
                            null,
                            reader.getSharedStringsTable(),
                            stats,
                            new DataFormatter(),
                            false);
                    handler.parse();

                    log.info("  {}: {} linhas x {} colunas", sheets.getSheetName(), stats.getLinhas(), stats.getColunas());
                    documentoAbaRepository.save(DocumentoAba.builder()
                            .documentoId(documento.getId())
                            .nomeAba(sheets.getSheetName())
                            .qtdLinhas(stats.getLinhas())
                            .qtdColunas(stats.getColunas())
                            .build());
                }
            }
        }
        log.info("Extraido de '{}': {} aba(s)", documento.getNome(), abas);
    }

    private void extrairApresentacao(Documento documento, InputStream in) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(in)) {
            int slides = ppt.getSlides().size();
            log.info("Extraido de '{}': {} slide(s)", documento.getNome(), slides);
            documentoAbaRepository.save(DocumentoAba.builder()
                    .documentoId(documento.getId())
                    .nomeAba("Apresentacao")
                    .qtdLinhas(slides)
                    .qtdColunas(0)
                    .build());
        }
    }

    private static class SheetStats implements XSSFSheetXMLHandler.SheetContentsHandler {

        private int linhas;
        private int colunas;
        private int colunasLinhaAtual;

        @Override
        public void startRow(int rowNum) {
            colunasLinhaAtual = 0;
        }

        @Override
        public void endRow(int rowNum) {
            linhas = Math.max(linhas, rowNum + 1);
            colunas = Math.max(colunas, colunasLinhaAtual);
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            colunasLinhaAtual = Math.max(colunasLinhaAtual, coluna(cellReference) + 1);
        }

        int getLinhas() {
            return linhas;
        }

        int getColunas() {
            return colunas;
        }

        private static int coluna(String cellReference) {
            if (cellReference == null || cellReference.isBlank()) {
                return 0;
            }

            int coluna = 0;
            for (int i = 0; i < cellReference.length(); i++) {
                char c = cellReference.charAt(i);
                if (!Character.isLetter(c)) {
                    break;
                }
                coluna = coluna * 26 + (Character.toUpperCase(c) - 'A' + 1);
            }
            return Math.max(coluna - 1, 0);
        }
    }
}
