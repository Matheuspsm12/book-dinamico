package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioEdicaoRequest {

    @Size(max = 200, message = "Nome deve ter no máximo {max} caracteres!")
    private String nome;

    @Size(max = 200, message = "Empresa deve ter no máximo {max} caracteres!")
    private String empresa;

    @Email(message = "E-mail inválido!")
    @Size(max = 200, message = "E-mail deve ter no máximo {max} caracteres!")
    private String email;
}
