package com.tcia.book_dinamico_back_end.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload do "Quero manter meu acesso" (token recebido no e-mail de ociosidade). */
@Data
public class ManterAcessoRequest {

    @NotBlank
    private String token;
}
