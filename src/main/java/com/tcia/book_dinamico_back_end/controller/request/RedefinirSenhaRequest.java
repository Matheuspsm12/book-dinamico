package com.tcia.book_dinamico_back_end.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload da redefinição de senha via token recebido por e-mail. */
@Data
public class RedefinirSenhaRequest {

    @NotBlank
    private String token;

    /** BCrypt limita entrada a 72 bytes; mínimo 8 pra evitar senhas triviais. */
    @NotBlank
    @Size(min = 8, max = 72)
    private String novaSenha;
}
