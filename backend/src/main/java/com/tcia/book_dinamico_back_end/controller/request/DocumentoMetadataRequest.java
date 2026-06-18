package com.tcia.book_dinamico_back_end.controller.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentoMetadataRequest {

    @NotBlank(message = "{documento.nome.not-blank}")
    @Size(max = 255, message = "{documento.nome.size}")
    private String nome;

    @NotBlank(message = "{documento.descricao.not-blank}")
    private String descricao;

    @NotNull(message = "{documento.data-atualizacao.not-null}")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAtualizacao;
}
