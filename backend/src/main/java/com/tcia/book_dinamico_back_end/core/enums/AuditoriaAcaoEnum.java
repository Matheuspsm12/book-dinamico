package com.tcia.book_dinamico_back_end.core.enums;

import lombok.Getter;

@Getter
public enum AuditoriaAcaoEnum {

    CRIAR_DOCUMENTO(EntidadeAuditoriaEnum.DOCUMENTO, AcaoBaseAuditoriaEnum.CRIAR),
    ALTERAR_DOCUMENTO(EntidadeAuditoriaEnum.DOCUMENTO, AcaoBaseAuditoriaEnum.ALTERAR),
    EXCLUIR_DOCUMENTO(EntidadeAuditoriaEnum.DOCUMENTO, AcaoBaseAuditoriaEnum.EXCLUIR),
    PROCESSAR_DOCUMENTO(EntidadeAuditoriaEnum.PROCESSAMENTO, AcaoBaseAuditoriaEnum.PROCESSAR),
    CRIAR_PERFIL(EntidadeAuditoriaEnum.PERFIL, AcaoBaseAuditoriaEnum.CRIAR),
    ALTERAR_PERFIL(EntidadeAuditoriaEnum.PERFIL, AcaoBaseAuditoriaEnum.ALTERAR),
    EXCLUIR_PERFIL(EntidadeAuditoriaEnum.PERFIL, AcaoBaseAuditoriaEnum.EXCLUIR),
    CRIAR_PERMISSAO(EntidadeAuditoriaEnum.PERMISSAO, AcaoBaseAuditoriaEnum.CRIAR),
    ALTERAR_PERMISSAO(EntidadeAuditoriaEnum.PERMISSAO, AcaoBaseAuditoriaEnum.ALTERAR),
    EXCLUIR_PERMISSAO(EntidadeAuditoriaEnum.PERMISSAO, AcaoBaseAuditoriaEnum.EXCLUIR);

    private final EntidadeAuditoriaEnum entidade;
    private final AcaoBaseAuditoriaEnum acao;

    AuditoriaAcaoEnum(EntidadeAuditoriaEnum entidade, AcaoBaseAuditoriaEnum acao) {
        this.entidade = entidade;
        this.acao = acao;
    }
}
