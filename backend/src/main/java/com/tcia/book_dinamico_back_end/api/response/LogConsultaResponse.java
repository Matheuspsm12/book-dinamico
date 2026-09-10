package com.tcia.book_dinamico_back_end.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado do tail do log da aplicação para a tela de Diagnóstico.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogConsultaResponse {

    /** {@code false} em ambientes sem appender de arquivo (nesse caso {@link #linhas} vem vazia). */
    private boolean logEmArquivoAtivo;

    /** Texto explicativo quando {@link #logEmArquivoAtivo} é {@code false}. */
    private String mensagemInativo;

    /** Nome do arquivo efetivamente lido. */
    private String arquivo;

    /** Arquivos de log disponíveis no diretório (ativo primeiro, depois os rotacionados). */
    private List<String> arquivosDisponiveis;

    /** Momento em que o tail foi gerado. */
    private LocalDateTime geradoEm;

    /** Quantidade de entradas retornadas em {@link #linhas} (após filtros). */
    private int totalLinhas;

    /** Entradas do log, das mais antigas para as mais recentes. */
    private List<LogLinhaResponse> linhas;
}
