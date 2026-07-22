package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PerfilRequest {

    @NotBlank(message = "{perfil.nome.not-blank}")
    @Size(max = 255, message = "{perfil.nome.size}")
    private String nomePerfil;

    private String descricao;

    private List<Long> permissoesIds = new ArrayList<>();
}
