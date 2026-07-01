package com.tcia.book_dinamico_back_end.infrastructure.scheduler;

import com.tcia.book_dinamico_back_end.domain.service.DominioService;
import com.tcia.book_dinamico_back_end.domain.service.ProcessamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Log4j2
@RequiredArgsConstructor
@Component
public class ProcessamentoScheduler {

    private final ProcessamentoService service;
    private final DominioService dominioService;

    @Scheduled(cron = "0 */10 * * * *")
    public void executarProcessamento() {
        try {
            log.info("Verificando Agendamentos");
            var habilitado = dominioService.buscarPorChave("PROCESSAMENTO_HABILITADO");

            if (Objects.nonNull(habilitado.getValor()) && Boolean.parseBoolean(habilitado.getValor())) {
                service.verificarProcessamento();
            } else{
                log.info("Processamento nao habilitado");
            }
            log.info("Processos executados!");
        } catch (Exception e) {
            log.error("Erro ao executar agendamentos", e);
        }
    }
}
