package com.tcia.book_dinamico_back_end.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoAbaResponse {
    private String nomeAba;
    private Integer qtdLinhas;
    private Integer qtdColunas;
}
