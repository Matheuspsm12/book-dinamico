package br.com.tcia.bookdinamico.controller.request;

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

    /** BCrypt limita entrada a 72 bytes; mínimo 8 pra evitar senhas triviais. */
    @NotBlank(message = "{usuario.senha.not-blank}")
    @Size(min = 8, max = 72, message = "{usuario.senha.size}")
    private String senha;

    @NotBlank(message = "{usuario.justificativa.not-blank}")
    private String justificativa;
}
