package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "{usuario.email.not-blank}")
    @Email(message = "{usuario.email.invalido}")
    private String email;

    @NotBlank(message = "{usuario.senha.not-blank}")
    private String senha;
}
