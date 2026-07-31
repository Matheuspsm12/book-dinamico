package com.tcia.book_dinamico_back_end.api.request;

import lombok.Data;

@Data
public class SimularOciosidadeRequest {

    /** Há quantos meses o usuário "não acessa". Nulo usa o default (limite + 1). */
    private Integer mesesInativos;

    /**
     * Se informado (>0), marca o usuário como já notificado há N dias corridos,
     * encenando a etapa de desativação na próxima verificação.
     */
    private Integer notificadoHaDias;
}
