package com.tcia.book_dinamico_back_end.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload do "Esqueci minha senha" público (informa o e-mail cadastrado). */
@Data
public class EsqueciSenhaRequest {

    @NotBlank
    @Email
    private String email;
}
