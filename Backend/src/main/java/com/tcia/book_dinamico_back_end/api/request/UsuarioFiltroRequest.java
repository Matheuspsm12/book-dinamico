package com.tcia.book_dinamico_back_end.api.request;

import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
import lombok.Data;

@Data
public class UsuarioFiltroRequest {
    private UsuarioStatus status;
    private String empresa;
    private String nome;
}
