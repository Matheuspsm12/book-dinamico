package br.com.tcia.bookdinamico.controller.request;

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

    @NotBlank(message = "{documento.nome.not-blank}")
    @Size(max = 255, message = "{documento.nome.size}")
    private String nome;

    @NotBlank(message = "{documento.descricao.not-blank}")
    private String descricao;

    /** Data manual da última atualização, exibida no portal (A6 / RN19). */
    @NotNull(message = "{documento.data-atualizacao.not-null}")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAtualizacao;
}
