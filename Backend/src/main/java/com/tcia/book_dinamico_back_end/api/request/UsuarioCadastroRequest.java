package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioCadastroRequest {

    @NotBlank(message = "Nome não pode estar vazio!")
    @Size(max = 200, message = "Nome deve ter no máximo {max} caracteres!")
    private String nome;

    @NotBlank(message = "Empresa não pode estar vazia!")
    @Size(max = 200, message = "Empresa deve ter no máximo {max} caracteres!")
    private String empresa;

    @NotBlank(message = "E-mail não pode estar vazio!")
    @Email(message = "E-mail inválido!")
    @Size(max = 200, message = "E-mail deve ter no máximo {max} caracteres!")
    private String email;

    @NotBlank(message = "Senha não pode estar vazia!")
    @Size(min = 8, max = 72, message = "Senha deve ter entre {min} e {max} caracteres!")
    private String senha;

    @NotBlank(message = "Justificativa não pode estar vazia!")
    private String justificativa;
}
