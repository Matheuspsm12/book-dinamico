package com.tcia.book_dinamico_back_end.core.util;

import com.tcia.book_dinamico_back_end.core.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.domain.exception.ArquivoException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Log4j2
@Component
public class IntegridadeArquivoValidator {

    public static final long TAMANHO_MAXIMO_BYTES = 60L * 1024 * 1024;

    private static final byte[] MAGIC_ZIP = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] MAGIC_CFB = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};

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
