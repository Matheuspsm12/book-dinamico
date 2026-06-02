package com.tcia.book_dinamico_back_end.utils;

import com.tcia.book_dinamico_back_end.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.exception.ArquivoException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Valida integridade básica de arquivos no upload (RN31 / N5):
 * (i)  extensão na whitelist (RN21)
 * (ii) magic bytes do header casa com um formato aceito —
 *      <ul>
 *        <li>{@code PK\x03\x04} → OOXML / ZIP (xlsx, xlsm, pptx modernos)</li>
 *        <li>{@code D0 CF 11 E0} → CFB (Compound File Binary) — usado pelos
 *            formatos binários antigos (xls, ppt) e também por arquivos
 *            OOXML <strong>protegidos por senha</strong>, que o Office encapsula
 *            num container CFB encriptado</li>
 *      </ul>
 * (iii) {@code 0 < tamanho ≤ 60 MB}
 */
@Log4j2
@Component
public class IntegridadeArquivoValidator {

    public static final long TAMANHO_MAXIMO_BYTES = 60L * 1024 * 1024;

    /** Local-file-header de ZIP/OOXML. */
    private static final byte[] MAGIC_ZIP = {0x50, 0x4B, 0x03, 0x04};
    /** Header de Compound File Binary (Office binário antigo + OOXML criptografado). */
    private static final byte[] MAGIC_CFB = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};

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

        if (!confereMagicBytesAceito(arquivo)) {
            throw new ArquivoException("arquivo-conteudo-incompativel");
        }

        return ext;
    }

    private boolean confereMagicBytesAceito(MultipartFile arquivo) {
        try (InputStream in = arquivo.getInputStream()) {
            byte[] head = in.readNBytes(4);
            if (head.length != 4) {
                log.warn("Magic bytes: lidos < 4 para {} (lidos={})", arquivo.getOriginalFilename(), head.length);
                return false;
            }
            boolean ok = startsWith(head, MAGIC_ZIP) || startsWith(head, MAGIC_CFB);
            if (!ok) {
                log.warn("Magic bytes não reconhecidos em {}: {} {} {} {} (esperado PK\\x03\\x04 ou D0CF11E0)",
                        arquivo.getOriginalFilename(),
                        String.format("%02X", head[0]),
                        String.format("%02X", head[1]),
                        String.format("%02X", head[2]),
                        String.format("%02X", head[3]));
            }
            return ok;
        } catch (IOException e) {
            log.warn("Falha ao ler magic bytes de {}: {}", arquivo.getOriginalFilename(), e.getMessage());
            return false;
        }
    }

    private static boolean startsWith(byte[] head, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) return false;
        }
        return true;
    }
}
