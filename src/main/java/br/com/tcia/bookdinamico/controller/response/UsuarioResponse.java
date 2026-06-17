package br.com.tcia.bookdinamico.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import br.com.tcia.bookdinamico.enums.UsuarioRole;
import br.com.tcia.bookdinamico.enums.UsuarioStatus;
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
    private UsuarioRole role;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime decididoEm;
    private Long aprovadoPorId;
}
