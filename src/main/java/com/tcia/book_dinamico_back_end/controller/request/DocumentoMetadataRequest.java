package com.tcia.book_dinamico_back_end.controller.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Metadata por arquivo no upload (US7 / RN24 / N6).
 * Usado tanto no upload inicial quanto em PUT /api/documentos/{id} (apenas metadata).
 */
@Data
public class DocumentoMetadataRequest {

    @NotBlank
    @Size(max = 255)
    private String nome;

    @NotBlank
    private String descricao;

    /** Data manual da última atualização, exibida no portal (A6 / RN19). */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAtualizacao;
}
