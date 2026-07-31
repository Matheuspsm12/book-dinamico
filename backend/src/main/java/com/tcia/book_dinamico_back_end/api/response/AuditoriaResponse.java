package com.tcia.book_dinamico_back_end.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditoriaResponse {
    private Long id;
    private String usuario;
    private String acao;
    private String entidade;
    private Long entidadeId;
    /** Para documentos, guarda o nome do documento na hora da ação. */
    private String detalhes;
    private LocalDateTime dataHora;
}
