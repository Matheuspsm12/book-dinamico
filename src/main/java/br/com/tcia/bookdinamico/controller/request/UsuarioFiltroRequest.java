package br.com.tcia.bookdinamico.controller.request;

import br.com.tcia.bookdinamico.enums.UsuarioStatus;
import lombok.Data;

/**
 * Filtro de paginação para US4 / RN13. Todos os campos opcionais.
 */
@Data
public class UsuarioFiltroRequest {
    private UsuarioStatus status;
    private String empresa;
    private String nome;
}
