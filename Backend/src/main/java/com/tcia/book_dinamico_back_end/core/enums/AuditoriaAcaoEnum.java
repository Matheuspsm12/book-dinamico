package com.tcia.book_dinamico_back_end.core.enums;

import lombok.Getter;

@Getter
public enum AuditoriaAcaoEnum {

    CRIAR_DOCUMENTO(EntidadeAuditoriaEnum.DOCUMENTO, AcaoBaseAuditoriaEnum.CRIAR),
    ALTERAR_DOCUMENTO(EntidadeAuditoriaEnum.DOCUMENTO, AcaoBaseAuditoriaEnum.ALTERAR),
    EXCLUIR_DOCUMENTO(EntidadeAuditoriaEnum.DOCUMENTO, AcaoBaseAuditoriaEnum.EXCLUIR),
    PROCESSAR_DOCUMENTO(EntidadeAuditoriaEnum.PROCESSAMENTO, AcaoBaseAuditoriaEnum.PROCESSAR);

    private final EntidadeAuditoriaEnum entidade;
    private final AcaoBaseAuditoriaEnum acao;

    AuditoriaAcaoEnum(EntidadeAuditoriaEnum entidade, AcaoBaseAuditoriaEnum acao) {
        this.entidade = entidade;
        this.acao = acao;
    }
}
