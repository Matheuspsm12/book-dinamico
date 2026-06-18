package com.tcia.book_dinamico_back_end.controller.request;

import com.tcia.book_dinamico_back_end.enums.UsuarioStatus;
import lombok.Data;

@Data
public class UsuarioFiltroRequest {
    private UsuarioStatus status;
    private String empresa;
    private String nome;
}
