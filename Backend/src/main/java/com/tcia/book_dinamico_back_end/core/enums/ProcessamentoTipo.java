package com.tcia.book_dinamico_back_end.core.enums;

import java.util.Arrays;

public enum ProcessamentoTipo {
    DOCUMENTO(1, "Documento");

    private final Integer codigo;
    private final String descricao;

    ProcessamentoTipo(Integer codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public static ProcessamentoTipo peloCodigo(Integer codigo) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.codigo.equals(codigo))
                .findFirst()
                .orElse(null);
    }
}
