package com.tcia.book_dinamico_back_end.infrastructure.event;

import com.tcia.book_dinamico_back_end.domain.event.ProcessamentoAgendadoEvent;
import com.tcia.book_dinamico_back_end.domain.service.ProcessamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessamentoAgendadoListenerTest {

    @Mock
    private ProcessamentoService processamentoService;

    @InjectMocks
    private ProcessamentoAgendadoListener listener;

    @Test
    void aoAgendarProcessamentoDeveProcessarPorId() {
        listener.aoAgendarProcessamento(new ProcessamentoAgendadoEvent(10L));

        verify(processamentoService).processarPorId(10L);
    }

}
