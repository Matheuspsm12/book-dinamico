package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.api.response.LogLinhaResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticoLogServiceTest {

    private final DiagnosticoLogService service = new DiagnosticoLogService();

    @Test
    void parsearEstruturaNivelLoggerEMensagem() {
        List<LogLinhaResponse> linhas = service.parsear(List.of(
                "2026-09-10 18:21:14 INFO  c.t.b.d.s.ProcessamentoService - Processamento iniciado id=7",
                "2026-09-10 18:21:15 WARN  c.t.b.d.s.ProcessamentoService - Reprocessamento agendado tentativa=1/3"));

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).getTimestamp()).isEqualTo("2026-09-10 18:21:14");
        assertThat(linhas.get(0).getNivel()).isEqualTo("INFO");
        assertThat(linhas.get(0).getLogger()).isEqualTo("c.t.b.d.s.ProcessamentoService");
        assertThat(linhas.get(0).getMensagem()).isEqualTo("Processamento iniciado id=7");
        assertThat(linhas.get(0).getStacktrace()).isNull();
        assertThat(linhas.get(1).getNivel()).isEqualTo("WARN");
    }

    @Test
    void parsearAgrupaLinhasDeContinuacaoComoStacktrace() {
        List<LogLinhaResponse> linhas = service.parsear(List.of(
                "2026-09-10 18:21:14 ERROR c.t.b.d.s.ProcessamentoService - Processamento falhou id=7",
                "java.lang.IllegalStateException: boom",
                "\tat com.tcia.Foo.bar(Foo.java:42)",
                "\tat com.tcia.Foo.baz(Foo.java:13)",
                "2026-09-10 18:21:16 INFO  c.t.b.d.s.OutroService - segue a vida"));

        assertThat(linhas).hasSize(2);
        LogLinhaResponse erro = linhas.get(0);
        assertThat(erro.getNivel()).isEqualTo("ERROR");
        assertThat(erro.getStacktrace())
                .contains("java.lang.IllegalStateException: boom")
                .contains("at com.tcia.Foo.bar(Foo.java:42)")
                .contains("at com.tcia.Foo.baz(Foo.java:13)");
        assertThat(linhas.get(1).getNivel()).isEqualTo("INFO");
        assertThat(linhas.get(1).getStacktrace()).isNull();
    }

    @Test
    void parsearMarcaComoRawLinhaForaDoPadraoAntesDeQualquerEntrada() {
        List<LogLinhaResponse> linhas = service.parsear(List.of(
                "texto solto sem formato de log",
                "2026-09-10 18:21:14 INFO  c.t.b.Main - Started BookDinamicoBackEndApplication"));

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).getNivel()).isEqualTo("RAW");
        assertThat(linhas.get(0).getMensagem()).isEqualTo("texto solto sem formato de log");
        assertThat(linhas.get(1).getNivel()).isEqualTo("INFO");
    }

    @Test
    void parsearAceitaTimestampComMilissegundos() {
        List<LogLinhaResponse> linhas = service.parsear(List.of(
                "2026-09-10 18:21:14.512 DEBUG c.t.b.Algo - com millis"));

        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).getNivel()).isEqualTo("DEBUG");
        assertThat(linhas.get(0).getTimestamp()).isEqualTo("2026-09-10 18:21:14.512");
        assertThat(linhas.get(0).getMensagem()).isEqualTo("com millis");
    }
}
