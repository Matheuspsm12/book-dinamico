package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Usuario nao pode estar vazio!")
    private String email;

    @NotBlank(message = "Senha nao pode estar vazia!")
    private String senha;
}
