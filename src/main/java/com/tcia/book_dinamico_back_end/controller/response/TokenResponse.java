package com.tcia.book_dinamico_back_end.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcia.book_dinamico_back_end.enums.UsuarioRole;
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
