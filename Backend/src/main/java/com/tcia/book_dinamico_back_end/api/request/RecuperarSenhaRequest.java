package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecuperarSenhaRequest {

    @NotBlank(message = "E-mail nao pode estar vazio!")
    @Email(message = "E-mail invalido!")
    private String email;
}
