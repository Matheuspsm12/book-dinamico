package br.com.tcia.bookdinamico.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {
    private String token;
    private Instant expiraEm;
    private String nome;
    private String email;
    /** Nome do perfil (ADMIN/USUARIO) — mantém o campo "role" para o frontend. */
    private String role;
}
