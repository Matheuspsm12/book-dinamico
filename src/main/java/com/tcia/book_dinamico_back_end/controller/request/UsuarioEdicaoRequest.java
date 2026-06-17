package com.tcia.book_dinamico_back_end.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Edição admin de usuário (US4 / RN14 / A3).
 * Campos permitidos: nome, empresa, email. NÃO edita role/senha/status.
 * Todos opcionais — só os campos enviados são alterados.
 */
@Data
public class UsuarioEdicaoRequest {

    @Size(max = 200, message = "{usuario.nome.size}")
    private String nome;

    @Size(max = 200, message = "{usuario.empresa.size}")
    private String empresa;

    @Email(message = "{usuario.email.invalido}")
    @Size(max = 200, message = "{usuario.email.size}")
    private String email;
}
