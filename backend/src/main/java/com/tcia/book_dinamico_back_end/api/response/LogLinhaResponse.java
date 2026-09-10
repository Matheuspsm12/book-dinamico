package com.tcia.book_dinamico_back_end.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Uma entrada do log da aplicação já estruturada a partir da linha de texto.
 * Linhas de continuação (stack trace, causas) são agrupadas em {@link #stacktrace}
 * na entrada que as originou.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogLinhaResponse {

    /** Carimbo de data/hora exatamente como aparece no arquivo (ex.: {@code 2026-09-10 18:21:14}). */
    private String timestamp;

    /** {@code TRACE|DEBUG|INFO|WARN|ERROR}, ou {@code RAW} para linhas fora do padrão. */
    private String nivel;

    /** Nome abreviado do logger (ex.: {@code c.t.b.d.s.ProcessamentoService}). */
    private String logger;

    /** Mensagem da linha. */
    private String mensagem;

    /** Linhas de continuação juntadas por {@code \n}, ou {@code null} se não houver. */
    private String stacktrace;
}
