package com.tcia.book_dinamico_back_end.service;

import com.tcia.book_dinamico_back_end.exception.ArquivoException;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Persiste binários de documentos no filesystem em {@code ${app.diretorio}} (D7).
 * Convenção de nome: {@code documento_{id}_{yyyyMMddHHmmss}.{ext}} — não colide entre versões.
 */
@Log4j2
@Service
public class ArquivoStorageService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${app.diretorio}")
    private String diretorioBase;

    private Path raiz;

    @PostConstruct
    public void init() {
        this.raiz = Paths.get(diretorioBase).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
            log.info("Diretório de arquivos: {}", raiz);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar diretório de arquivos: " + raiz, e);
        }
    }

    /**
     * Grava o arquivo no filesystem e devolve o caminho absoluto.
     *
     * @param documentoId id do documento (usado no nome do arquivo)
     * @param extensao    extensão em lowercase (xlsm/xlsx/pptx)
     * @param arquivo     binário recebido
     */
    public String gravar(Long documentoId, String extensao, MultipartFile arquivo) {
        String nome = "documento_%d_%s.%s".formatted(
                documentoId,
                LocalDateTime.now().format(TIMESTAMP_FMT),
                extensao.toLowerCase());
        Path destino = raiz.resolve(nome);
        try {
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            log.info("Arquivo gravado: {} ({} bytes)", destino, arquivo.getSize());
        } catch (IOException e) {
            throw new ArquivoException("erro-processamento-arquivo", e);
        }
        return destino.toString();
    }

    /** Apaga arquivo físico anterior — usado em substituição (RN25/RN26). */
    public void deletarSeExistir(String caminho) {
        if (caminho == null || caminho.isBlank()) return;
        try {
            Path p = Paths.get(caminho);
            if (Files.deleteIfExists(p)) {
                log.info("Arquivo antigo removido: {}", p);
            }
        } catch (IOException e) {
            log.warn("Falha ao remover arquivo antigo {}: {}", caminho, e.getMessage());
        }
    }

    public Resource ler(String caminho) {
        try {
            Path p = Paths.get(caminho);
            Resource r = new UrlResource(p.toUri());
            if (!r.exists() || !r.isReadable()) {
                throw new ArquivoException("erro-processamento-arquivo");
            }
            return r;
        } catch (MalformedURLException e) {
            throw new ArquivoException("erro-processamento-arquivo", e);
        }
    }
}
