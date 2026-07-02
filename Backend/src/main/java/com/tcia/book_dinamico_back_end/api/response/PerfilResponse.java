package com.tcia.book_dinamico_back_end.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfilResponse {
    private Long id;
    private String nomePerfil;
    private String descricao;
    private Boolean ativado;
    private LocalDateTime dataCriacao;
    private List<PermissaoResponse> permissoes;
}
