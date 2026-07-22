package com.tcia.book_dinamico_back_end.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcia.book_dinamico_back_end.core.annotation.Auditar;
import com.tcia.book_dinamico_back_end.core.util.UsuarioLogadoUtil;
import com.tcia.book_dinamico_back_end.domain.model.Auditoria;
import com.tcia.book_dinamico_back_end.domain.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Log4j2
@RequiredArgsConstructor
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;
    private final UsuarioLogadoUtil usuarioLogadoUtil;

    @AfterReturning(value = "@annotation(auditar)", returning = "retorno")
    public void auditar(JoinPoint joinPoint, Auditar auditar, Object retorno) {
        try {
            Auditoria logEntry = new Auditoria();
            logEntry.setUsuario(usuarioLogadoUtil.getEmailUsuarioLogado());
            logEntry.setAcao(auditar.acao().getAcao().name());
            logEntry.setEntidade(auditar.acao().getEntidade().name());

            if (retorno != null) {
                try {
                    Object idValue = retorno.getClass().getMethod("getId").invoke(retorno);
                    if (idValue instanceof Long id) {
                        logEntry.setEntidadeId(id);
                    }
                } catch (Exception ignored) {
                }
            }

            if (auditar.detalhes()) {
                logEntry.setDetalhes(serializarDetalhes(joinPoint));
            }

            auditoriaService.salvar(logEntry);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria: {}", e.getMessage(), e);
        }
    }

    private String serializarDetalhes(JoinPoint joinPoint) {
        try {
            return objectMapper.writeValueAsString(joinPoint.getArgs());
        } catch (JsonProcessingException e) {
            return "Nao foi possivel serializar detalhes da acao.";
        }
    }
}
