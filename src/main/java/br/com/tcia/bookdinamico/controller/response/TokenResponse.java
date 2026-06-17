package br.com.tcia.bookdinamico.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import br.com.tcia.bookdinamico.enums.UsuarioRole;
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
    private UsuarioRole role;
}
