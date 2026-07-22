package com.tcia.book_dinamico_back_end.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioResponse {
    private Long id;
    private String nome;
    private String empresa;
    private String email;
    private String justificativa;
    private UsuarioStatus status;
    private String role;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime decididoEm;
    private Long aprovadoPorId;
}
