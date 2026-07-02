package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissaoRequest {

    @NotBlank(message = "{permissao.nome.not-blank}")
    @Size(max = 255, message = "{permissao.nome.size}")
    private String nomePermissao;

    private String descricao;
}
