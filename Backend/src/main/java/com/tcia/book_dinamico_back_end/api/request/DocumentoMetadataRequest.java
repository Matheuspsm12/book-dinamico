package com.tcia.book_dinamico_back_end.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentoMetadataRequest {

    @NotBlank(message = "Nome do documento não pode estar vazio!")
    @Size(max = 255, message = "Nome do documento deve ter no máximo {max} caracteres!")
    private String nome;

    @NotBlank(message = "Descrição não pode estar vazia!")
    private String descricao;

    @NotNull(message = "Data de atualização não pode ser nula!")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAtualizacao;
}
