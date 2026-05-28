package com.tcia.book_dinamico_back_end.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload do autocadastro (US2 — RN05/RN06).
 * Senha incluída via decisão A1. Status inicial é PENDENTE — setado no service, não vem do cliente.
 */
@Data
public class UsuarioCadastroRequest {

    @NotBlank
    @Size(max = 200)
    private String nome;

    @NotBlank
    @Size(max = 200)
    private String empresa;

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    /** BCrypt limita entrada a 72 bytes; mínimo 8 pra evitar senhas triviais. */
    @NotBlank
    @Size(min = 8, max = 72)
    private String senha;

    @NotBlank
    private String justificativa;
}
