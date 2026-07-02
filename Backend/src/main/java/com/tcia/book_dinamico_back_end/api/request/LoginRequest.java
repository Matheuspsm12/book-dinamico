package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "{login.usuario.not-blank}")
    private String email;

    @NotBlank(message = "{login.senha.not-blank}")
    private String senha;
}
