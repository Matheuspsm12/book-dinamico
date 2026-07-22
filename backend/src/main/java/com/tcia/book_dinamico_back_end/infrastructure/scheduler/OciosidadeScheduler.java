package com.tcia.book_dinamico_back_end.infrastructure.scheduler;

import com.tcia.book_dinamico_back_end.domain.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component
public class OciosidadeScheduler {

    private final UsuarioService usuarioService;

    @Scheduled(cron = "${scheduling.ociosidade.cron}")
    public void verificarOciosidade() {
        try {
            log.info("Verificando ociosidade de usuários");
            usuarioService.processarOciosidade();
            log.info("Verificação de ociosidade concluída");
        } catch (Exception e) {
            log.error("Erro ao verificar ociosidade de usuários", e);
        }
    }
}
