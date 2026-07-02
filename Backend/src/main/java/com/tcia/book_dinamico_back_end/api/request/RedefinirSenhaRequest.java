package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RedefinirSenhaRequest {

    @NotBlank(message = "{redefinir.token.not-blank}")
    private String token;

    @NotBlank(message = "{usuario.senha.not-blank}")
    @Size(min = 8, max = 72, message = "{usuario.senha.size}")
    private String novaSenha;
}
