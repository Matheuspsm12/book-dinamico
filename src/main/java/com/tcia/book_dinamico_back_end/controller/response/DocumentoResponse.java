package com.tcia.book_dinamico_back_end.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcia.book_dinamico_back_end.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.enums.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentoResponse {
    private Long id;
    private String nome;
    private String descricao;
    private TipoDocumento tipo;
    private ExtensaoDocumento extensao;
    private Long tamanhoBytes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAtualizacao;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private Long criadoPorId;
    private Long atualizadoPorId;
    private Boolean ativo;
    // Nunca exposto: caminhoArmazenamento (caminho interno do filesystem).
}
