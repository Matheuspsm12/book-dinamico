package com.tcia.book_dinamico_back_end.infrastructure.event;

import com.tcia.book_dinamico_back_end.domain.event.ProcessamentoAgendadoEvent;
import com.tcia.book_dinamico_back_end.domain.service.ProcessamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Component
@RequiredArgsConstructor
public class ProcessamentoAgendadoListener {

    private final ProcessamentoService processamentoService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAgendarProcessamento(ProcessamentoAgendadoEvent event) {
        try {
            processamentoService.processarPorId(event.processamentoId());
        } catch (Exception e) {
            log.error("Erro ao iniciar processamento assíncrono id={}", event.processamentoId(), e);
        }
    }
}
