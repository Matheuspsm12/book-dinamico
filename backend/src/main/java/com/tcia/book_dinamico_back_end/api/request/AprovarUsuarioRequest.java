package com.tcia.book_dinamico_back_end.api.request;

import lombok.Data;

@Data
public class AprovarUsuarioRequest {

    /** Perfil a atribuir ao usuário na aprovação. Se nulo, mantém o perfil atual. */
    private Long idPerfil;
}
