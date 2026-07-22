package com.tcia.book_dinamico_back_end.core.enums;

import java.util.Locale;
import java.util.Optional;

public enum ExtensaoDocumento {
    XLSM(TipoDocumento.EXCEL),
    XLSX(TipoDocumento.EXCEL),
    PPTX(TipoDocumento.POWERPOINT);

    private final TipoDocumento tipo;

    ExtensaoDocumento(TipoDocumento tipo) {
        this.tipo = tipo;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public static Optional<ExtensaoDocumento> fromString(String raw) {
        if (raw == null) return Optional.empty();
        String normalizada = raw.toUpperCase(Locale.ROOT).trim();
        if (normalizada.startsWith(".")) {
            normalizada = normalizada.substring(1);
        }
        try {
            return Optional.of(ExtensaoDocumento.valueOf(normalizada));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<ExtensaoDocumento> fromFilename(String filename) {
        if (filename == null) return Optional.empty();
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return Optional.empty();
        return fromString(filename.substring(dot + 1));
    }
}
