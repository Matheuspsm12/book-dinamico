package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissaoRequest {

    @NotBlank(message = "Nome da permissão não pode estar vazio!")
    @Size(max = 255, message = "Nome da permissão deve ter no máximo {max} caracteres!")
    private String nomePermissao;

    private String descricao;
}
