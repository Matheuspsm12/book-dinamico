package com.tcia.book_dinamico_back_end.utils;

import com.tcia.book_dinamico_back_end.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.exception.ArquivoException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Valida integridade básica de arquivos no upload (RN31 / N5):
 * (i)  extensão na whitelist (RN21)
 * (ii) MIME magic-byte casa com a extensão — todos os formatos são OOXML (ZIP),
 *      então checamos o magic do ZIP {@code PK\x03\x04}
 * (iii) {@code 0 < tamanho ≤ 60 MB}
 */
@Log4j2
@Component
public class IntegridadeArquivoValidator {

    public static final long TAMANHO_MAXIMO_BYTES = 60L * 1024 * 1024;

    /** Assinatura local-file-header de qualquer arquivo ZIP/OOXML. */
    private static final byte[] ZIP_LOCAL_FILE_HEADER = {0x50, 0x4B, 0x03, 0x04};

    /**
     * Valida e retorna a extensão do arquivo.
     *
     * @throws ArquivoException com chave i18n específica em caso de violação
     */
    public ExtensaoDocumento validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoException("arquivo-invalido");
        }

        ExtensaoDocumento ext = ExtensaoDocumento.fromFilename(arquivo.getOriginalFilename())
                .orElseThrow(() -> new ArquivoException("arquivo-extensao-nao-permitida"));

        long size = arquivo.getSize();
        if (size <= 0) {
            throw new ArquivoException("arquivo-invalido");
        }
        if (size > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoException("arquivo-tamanho-excedido");
        }

        if (!confereMagicBytesZip(arquivo)) {
            throw new ArquivoException("arquivo-conteudo-incompativel");
        }

        return ext;
    }

    private boolean confereMagicBytesZip(MultipartFile arquivo) {
        try (var in = arquivo.getInputStream()) {
            byte[] head = new byte[4];
            int lidos = in.read(head);
            if (lidos != 4) return false;
            for (int i = 0; i < 4; i++) {
                if (head[i] != ZIP_LOCAL_FILE_HEADER[i]) return false;
            }
            return true;
        } catch (IOException e) {
            log.warn("Falha ao ler magic bytes de {}: {}", arquivo.getOriginalFilename(), e.getMessage());
            return false;
        }
    }
}
