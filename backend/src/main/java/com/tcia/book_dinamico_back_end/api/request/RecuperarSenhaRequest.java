package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecuperarSenhaRequest {

    @NotBlank(message = "{email.not-blank}")
    @Email(message = "{email.invalido}")
    private String email;
}
