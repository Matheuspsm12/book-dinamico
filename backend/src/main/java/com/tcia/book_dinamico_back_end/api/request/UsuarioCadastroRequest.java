package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioCadastroRequest {

    @NotBlank(message = "{usuario.nome.not-blank}")
    @Size(max = 200, message = "{usuario.nome.size}")
    private String nome;

    @NotBlank(message = "{usuario.empresa.not-blank}")
    @Size(max = 200, message = "{usuario.empresa.size}")
    private String empresa;

    @NotBlank(message = "{usuario.email.not-blank}")
    @Email(message = "{usuario.email.invalido}")
    @Size(max = 200, message = "{usuario.email.size}")
    private String email;

    @NotBlank(message = "{usuario.senha.not-blank}")
    @Size(min = 8, max = 72, message = "{usuario.senha.size}")
    private String senha;

    @NotBlank(message = "{usuario.justificativa.not-blank}")
    private String justificativa;
}
