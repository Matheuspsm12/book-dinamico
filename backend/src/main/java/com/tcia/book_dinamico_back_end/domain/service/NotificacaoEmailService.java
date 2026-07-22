package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.domain.repository.UsuarioRepository;
import com.tcia.book_dinamico_back_end.infrastructure.adapter.EmailAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificacaoEmailService {

    private final UsuarioRepository usuarioRepository;
    private final EmailAdapter emailAdapter;

    public void notificarNovaPublicacao() {
        if (!emailAdapter.isHabilitado()) {
            log.info("[notificacao] e-mail desabilitado; nova publicação não notificada");
            return;
        }
        List<Usuario> aprovados = usuarioRepository.findByStatus(UsuarioStatus.APROVADO);
        aprovados.forEach(emailAdapter::enviarNovaPublicacao);
        log.info("Notificação de nova publicação enviada para {} usuário(s) aprovado(s)", aprovados.size());
    }
}
