package com.tcia.book_dinamico_back_end.api.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ProcessamentoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nomeArquivo;
    private String tipoProcessamento;
    private String nomeProcessamento;
    private String resultadoAmigavel;
    private String tamanho;
    private String usuario;
    private Long documentoId;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataStart;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataInicio;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataFim;
}
