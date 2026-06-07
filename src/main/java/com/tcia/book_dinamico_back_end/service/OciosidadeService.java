package com.tcia.book_dinamico_back_end.service;

import com.tcia.book_dinamico_back_end.config.PortalLinks;
import com.tcia.book_dinamico_back_end.email.EmailAdapter;
import com.tcia.book_dinamico_back_end.entity.Usuario;
import com.tcia.book_dinamico_back_end.enums.UsuarioRole;
import com.tcia.book_dinamico_back_end.enums.UsuarioStatus;
import com.tcia.book_dinamico_back_end.exception.NegocioException;
import com.tcia.book_dinamico_back_end.repository.UsuarioRepository;
import com.tcia.book_dinamico_back_end.utils.DiasUteis;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controle de ociosidade (Item 1). Job diário:
 *   1. Avisa por e-mail usuários APROVADO/USUARIO sem acesso há mais de N meses.
 *   2. Remove (soft-delete → DESATIVADO) quem não retornou em até K dias úteis após o aviso.
 *
 * "Retorno" que cancela a remoção: novo login (zera o aviso em {@code UsuarioService.autenticar})
 * ou clique no link "Quero manter meu acesso" ({@link #manterAcesso(String)}). ADMINs são isentos.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class OciosidadeService {

    @Value("${app.ociosidade.habilitado:true}")
    private boolean habilitado;

    @Value("${app.ociosidade.meses-inatividade:4}")
    private int mesesInatividade;

    @Value("${app.ociosidade.dias-uteis-prazo:2}")
    private int diasUteisPrazo;

    private final UsuarioRepository usuarioRepository;
    private final EmailAdapter emailAdapter;
    private final TokenAcaoService tokenAcaoService;
    private final PortalLinks portalLinks;

    /** Job agendado (cron em {@code app.ociosidade.cron}). */
    @Scheduled(cron = "${app.ociosidade.cron:0 0 8 * * *}")
    public void processarOciosidade() {
        if (!habilitado) {
            log.debug("Controle de ociosidade desabilitado (app.ociosidade.habilitado=false).");
            return;
        }
        // Sem e-mail ativo não há como avisar antes de remover — não progride o ciclo
        // (evita desativar usuários silenciosamente quando o SMTP está desligado).
        if (!emailAdapter.isHabilitado()) {
            log.warn("Ociosidade: e-mail desabilitado no servidor — ciclo pulado (não removemos sem avisar).");
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        List<Usuario> aprovados = usuarioRepository.findByStatus(UsuarioStatus.APROVADO);
        int avisados = 0;
        int removidos = 0;
        for (Usuario u : aprovados) {
            if (u.getRole() == UsuarioRole.ADMIN) {
                continue; // admins isentos
            }
            if (u.getAvisoOciosidadeEnviadoEm() == null) {
                if (estaOcioso(u, agora)) {
                    avisar(u);
                    avisados++;
                }
            } else if (prazoExpirado(u.getAvisoOciosidadeEnviadoEm(), agora)) {
                remover(u);
                removidos++;
            }
        }
        if (avisados > 0 || removidos > 0) {
            log.info("Ociosidade processada: {} avisados, {} removidos (de {} aprovados).",
                    avisados, removidos, aprovados.size());
        }
    }

    private boolean estaOcioso(Usuario u, LocalDateTime agora) {
        LocalDateTime referencia = u.getUltimoAcesso() != null ? u.getUltimoAcesso()
                : (u.getDecididoEm() != null ? u.getDecididoEm() : u.getCriadoEm());
        return referencia != null && referencia.isBefore(agora.minusMonths(mesesInatividade));
    }

    private boolean prazoExpirado(LocalDateTime avisoEm, LocalDateTime agora) {
        return !agora.isBefore(DiasUteis.somar(avisoEm, diasUteisPrazo));
    }

    private void avisar(Usuario u) {
        String token = tokenAcaoService.gerar(
                u.getId(), TokenAcaoService.Proposito.MANTER_ACESSO, Duration.ofDays(7));
        u.setAvisoOciosidadeEnviadoEm(LocalDateTime.now());
        usuarioRepository.save(u);
        emailAdapter.enviarAvisoOciosidade(u, portalLinks.manterAcesso(token), mesesInatividade, diasUteisPrazo);
        log.info("Aviso de ociosidade enviado: usuario id={}", u.getId());
    }

    private void remover(Usuario u) {
        u.setStatus(UsuarioStatus.DESATIVADO);
        usuarioRepository.save(u);
        emailAdapter.enviarRemocaoOciosidade(u);
        log.info("Usuário removido por ociosidade (DESATIVADO): id={}", u.getId());
    }

    /**
     * "Quero manter meu acesso" — confirma interesse via token do e-mail.
     * Zera o aviso e renova o último acesso. Se o acesso já tiver sido removido,
     * o link não reativa (é preciso novo cadastro).
     */
    @Transactional
    public void manterAcesso(String token) {
        Long usuarioId = tokenAcaoService.validar(token, TokenAcaoService.Proposito.MANTER_ACESSO)
                .orElseThrow(() -> new NegocioException("token-invalido"));
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NegocioException("token-invalido"));
        if (u.getStatus() != UsuarioStatus.APROVADO) {
            throw new NegocioException("ociosidade-acesso-ja-removido");
        }
        u.setUltimoAcesso(LocalDateTime.now());
        u.setAvisoOciosidadeEnviadoEm(null);
        usuarioRepository.save(u);
        tokenAcaoService.consumir(token);
        log.info("Acesso mantido via link: usuario id={}", usuarioId);
    }
}
