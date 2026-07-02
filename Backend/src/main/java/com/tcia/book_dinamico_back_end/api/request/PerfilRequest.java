package com.tcia.book_dinamico_back_end.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PerfilRequest {

    @NotBlank(message = "Nome do perfil não pode estar vazio!")
    @Size(max = 255, message = "Nome do perfil deve ter no máximo {max} caracteres!")
    private String nomePerfil;

    private String descricao;

    private List<Long> permissoesIds = new ArrayList<>();
}
