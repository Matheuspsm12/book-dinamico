package com.tcia.book_dinamico_back_end.core.enums;

public enum ProcessamentoResultado {
    AGENDADO("Agendado"),
    SUCESSO("Sucesso"),
    ERRO("Erro"),
    REPROCESSAMENTO_AGENDADO("Reprocessamento agendado");

    private final String descricao;

    ProcessamentoResultado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
