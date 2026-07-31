package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.infrastructure.mapper.UsuarioMapper;
import com.tcia.book_dinamico_back_end.api.request.LoginRequest;
import com.tcia.book_dinamico_back_end.api.request.UsuarioCadastroRequest;
import com.tcia.book_dinamico_back_end.api.request.UsuarioEdicaoRequest;
import com.tcia.book_dinamico_back_end.api.request.UsuarioFiltroRequest;
import com.tcia.book_dinamico_back_end.api.response.OciosidadeResultadoResponse;
import com.tcia.book_dinamico_back_end.api.response.TokenResponse;
import com.tcia.book_dinamico_back_end.api.response.UsuarioResponse;
import com.tcia.book_dinamico_back_end.infrastructure.adapter.EmailAdapter;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.domain.model.ResetSenhaToken;
import com.tcia.book_dinamico_back_end.domain.repository.PerfilRepository;
import com.tcia.book_dinamico_back_end.domain.repository.ResetSenhaTokenRepository;
import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
import com.tcia.book_dinamico_back_end.domain.exception.ErroAutenticacaoException;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.exception.ResourceNotFoundException;
import com.tcia.book_dinamico_back_end.infrastructure.security.JwtTokenProvider;
import com.tcia.book_dinamico_back_end.domain.repository.UsuarioRepository;
import com.tcia.book_dinamico_back_end.domain.specification.UsuarioSpecifications;
import com.tcia.book_dinamico_back_end.core.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class UsuarioService {

    public static final long CAP_USUARIOS_APROVADOS = 40L;

    private static final String PERFIL_USUARIO = "USUARIO";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioSpecifications usuarioSpecifications;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailAdapter emailAdapter;
    private final AuthUtils authUtils;
    private final ResetSenhaTokenRepository resetSenhaTokenRepository;

    @Value("${app.url-site}")
    private String urlSite;

    @Transactional
    public TokenResponse autenticar(LoginRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ErroAutenticacaoException("erro-credenciais-invalidas"));

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            log.warn("Senha incorreta para {}", request.getEmail());
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }

        validarStatusParaLogin(usuario.getStatus());

        usuarioRepository.registrarAcesso(usuario.getId(), LocalDateTime.now());

        String token = jwtTokenProvider.gerarToken(usuario, httpRequest);
        log.info("Login bem-sucedido: {}", usuario.getEmail());

        return TokenResponse.builder()
                .token(token)
                .expiraEm(jwtTokenProvider.obterExpiracao(token))
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getPerfil() != null ? usuario.getPerfil().getNomePerfil() : null)
                .build();
    }

    private void validarStatusParaLogin(UsuarioStatus status) {
        switch (status) {
            case PENDENTE   -> throw new ErroAutenticacaoException("erro-conta-pendente");
            case REJEITADO  -> throw new ErroAutenticacaoException("erro-conta-rejeitada");
            case DESATIVADO -> throw new ErroAutenticacaoException("erro-conta-desativada");
            case APROVADO   -> { /* ok */ }
        }
    }

    @Transactional
    public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
        validarEmailUnico(request.getEmail());

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.setStatus(UsuarioStatus.PENDENTE);
        usuario.setPerfil(perfilRepository.findByNomePerfil(PERFIL_USUARIO)
                .orElseThrow(() -> new NegocioException("erro-inesperado")));

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Novo cadastro PENDENTE: id={} email={}", salvo.getId(), salvo.getEmail());

        return usuarioMapper.toResponse(salvo);
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("erro-email-duplicado");
        }
    }

    @Transactional
    public UsuarioResponse aprovar(Long usuarioId, Long idPerfil) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);
        validarStatusPendente(usuario);
        validarCapAprovados();

        if (idPerfil != null) {
            usuario.setPerfil(buscarPerfilOuFalhar(idPerfil));
        }

        usuario.setStatus(UsuarioStatus.APROVADO);
        usuario.setAprovadoPor(adminLogado());
        usuario.setDecididoEm(LocalDateTime.now());

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário aprovado: id={} email={} aprovadoPor={}",
                salvo.getId(), salvo.getEmail(),
                salvo.getAprovadoPor() != null ? salvo.getAprovadoPor().getId() : null);

        emailAdapter.enviarAprovacao(salvo);
        return usuarioMapper.toResponse(salvo);
    }

    @Transactional
    public UsuarioResponse rejeitar(Long usuarioId) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);
        validarStatusPendente(usuario);

        usuario.setStatus(UsuarioStatus.REJEITADO);
        usuario.setAprovadoPor(adminLogado());
        usuario.setDecididoEm(LocalDateTime.now());

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário rejeitado: id={} email={}", salvo.getId(), salvo.getEmail());

        emailAdapter.enviarRejeicao(salvo);
        return usuarioMapper.toResponse(salvo);
    }

    private Usuario buscarPorIdOuFalhar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }

    private void validarStatusPendente(Usuario usuario) {
        if (usuario.getStatus() != UsuarioStatus.PENDENTE) {
            throw new NegocioException("erro-decisao-invalida-status-nao-pendente");
        }
    }

    private void validarCapAprovados() {
        if (usuarioRepository.countByStatus(UsuarioStatus.APROVADO) >= CAP_USUARIOS_APROVADOS) {
            throw new NegocioException("cap-usuarios-excedido");
        }
    }

    private Usuario adminLogado() {
        Usuario admin = authUtils.getUsuarioLogado();
        if (admin == null) {
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }
        return admin;
    }

    @Transactional
    public void resetarMinhaSenhaPorEmail() {
        Usuario usuario = authUtils.getUsuarioLogado();
        if (usuario == null) {
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }
        if (!emailAdapter.isHabilitado()) {
            throw new NegocioException("email-desabilitado");
        }
        String senhaTemp = gerarSenhaTemporaria();
        usuario.setSenhaHash(passwordEncoder.encode(senhaTemp));
        usuarioRepository.save(usuario);
        emailAdapter.enviarSenhaTemporaria(usuario, senhaTemp);
        log.info("Senha temporária gerada e enviada para usuário id={}", usuario.getId());
    }

    private static final int RESET_TOKEN_VALIDADE_HORAS = 1;

    @Transactional
    public void recuperarSenhaPorEmail(String email) {
        if (!emailAdapter.isHabilitado()) {
            throw new NegocioException("email-desabilitado");
        }
        usuarioRepository.findByEmail(email).ifPresentOrElse(usuario -> {
            String token = UUID.randomUUID().toString();
            ResetSenhaToken resetToken = ResetSenhaToken.builder()
                    .token(token)
                    .usuario(usuario)
                    .expiraEm(LocalDateTime.now().plusHours(RESET_TOKEN_VALIDADE_HORAS))
                    .usado(false)
                    .build();
            resetSenhaTokenRepository.save(resetToken);

            String link = urlSite.split(",")[0].trim() + "/redefinir-senha?token=" + token;
            emailAdapter.enviarLinkRecuperacao(usuario, link);
            log.info("Link de redefinição de senha enviado para usuário id={}", usuario.getId());
        }, () -> log.info("Recuperação de senha solicitada para e-mail inexistente: {}", email));
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        ResetSenhaToken resetToken = resetSenhaTokenRepository.findByToken(token)
                .orElseThrow(() -> new NegocioException("token-invalido"));

        if (!resetToken.isValido()) {
            throw new NegocioException("token-invalido");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        resetSenhaTokenRepository.save(resetToken);
        log.info("Senha redefinida via token para usuário id={}", usuario.getId());
    }

    private static final int OCIOSIDADE_MESES = 4;
    private static final int OCIOSIDADE_PRAZO_DIAS_UTEIS = 2;

    @Transactional
    public OciosidadeResultadoResponse processarOciosidade() {
        if (!emailAdapter.isHabilitado()) {
            log.info("[ociosidade] e-mail desabilitado; verificação ignorada");
            return OciosidadeResultadoResponse.builder()
                    .emailDesabilitado(true)
                    .notificados(List.of())
                    .desativados(List.of())
                    .build();
        }
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limiteOciosidade = agora.minusMonths(OCIOSIDADE_MESES);
        List<Usuario> aprovados = usuarioRepository.findByStatus(UsuarioStatus.APROVADO);

        List<String> notificados = new java.util.ArrayList<>();
        List<String> desativados = new java.util.ArrayList<>();

        for (Usuario usuario : aprovados) {
            LocalDateTime acesso = usuario.getUltimoAcesso() != null
                    ? usuario.getUltimoAcesso()
                    : usuario.getCriadoEm();

            boolean ocioso = acesso != null && acesso.isBefore(limiteOciosidade);
            if (!ocioso) {
                continue;
            }

            if (usuario.getOciosidadeNotificadoEm() == null) {
                usuario.setOciosidadeNotificadoEm(agora);
                usuarioRepository.save(usuario);
                emailAdapter.enviarOciosidade(usuario);
                notificados.add(usuario.getEmail());
                log.info("Ociosidade notificada: id={} email={}", usuario.getId(), usuario.getEmail());
            } else if (agora.isAfter(adicionarDiasUteis(usuario.getOciosidadeNotificadoEm(), OCIOSIDADE_PRAZO_DIAS_UTEIS))) {
                usuario.setStatus(UsuarioStatus.DESATIVADO);
                usuario.setAtivo(false);
                usuarioRepository.save(usuario);
                desativados.add(usuario.getEmail());
                log.info("Usuário desativado por ociosidade: id={} email={}", usuario.getId(), usuario.getEmail());
            }
        }

        return OciosidadeResultadoResponse.builder()
                .emailDesabilitado(false)
                .notificados(notificados)
                .desativados(desativados)
                .build();
    }

    /**
     * Encena a ociosidade de um usuário APROVADO para permitir demonstrar o fluxo sem
     * esperar os 4 meses reais: retrocede o último acesso e, opcionalmente, a data de
     * notificação (para que a próxima verificação já desative).
     *
     * @param mesesInativos          há quantos meses o usuário "não acessa" (default 5)
     * @param notificadoHaDias       se informado (>0), marca como já notificado há N dias
     *                               corridos, encenando a etapa de desativação
     */
    @Transactional
    public UsuarioResponse simularOciosidade(Long usuarioId, Integer mesesInativos, Integer notificadoHaDias) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);
        if (usuario.getStatus() != UsuarioStatus.APROVADO) {
            throw new NegocioException("erro-ociosidade-usuario-nao-aprovado");
        }
        int meses = (mesesInativos != null && mesesInativos > 0) ? mesesInativos : OCIOSIDADE_MESES + 1;
        LocalDateTime agora = LocalDateTime.now();

        usuario.setUltimoAcesso(agora.minusMonths(meses));
        if (notificadoHaDias != null && notificadoHaDias > 0) {
            usuario.setOciosidadeNotificadoEm(agora.minusDays(notificadoHaDias));
        } else {
            usuario.setOciosidadeNotificadoEm(null);
        }

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Ociosidade simulada: id={} email={} mesesInativos={} notificadoHaDias={}",
                salvo.getId(), salvo.getEmail(), meses, notificadoHaDias);
        return usuarioMapper.toResponse(salvo);
    }

    private static LocalDateTime adicionarDiasUteis(LocalDateTime inicio, int diasUteis) {
        LocalDateTime data = inicio;
        int adicionados = 0;
        while (adicionados < diasUteis) {
            data = data.plusDays(1);
            DayOfWeek dia = data.getDayOfWeek();
            if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
                adicionados++;
            }
        }
        return data;
    }

    private static String gerarSenhaTemporaria() {
        final String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(alfabeto.charAt(rnd.nextInt(alfabeto.length())));
        return sb.toString();
    }

    public Page<UsuarioResponse> paginar(UsuarioFiltroRequest filtro, Pageable pageable) {
        Specification<Usuario> spec = usuarioSpecifications.comFiltros(filtro);
        return usuarioRepository.findAll(spec, pageable).map(usuarioMapper::toResponse);
    }

    @Transactional
    public UsuarioResponse atualizar(Long usuarioId, UsuarioEdicaoRequest request) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(usuario.getEmail())
                && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new NegocioException("erro-email-duplicado");
        }

        usuarioMapper.atualizar(usuario, request);

        if (request.getIdPerfil() != null) {
            usuario.setPerfil(buscarPerfilOuFalhar(request.getIdPerfil()));
        }

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário atualizado: id={} email={} perfil={}",
                salvo.getId(), salvo.getEmail(),
                salvo.getPerfil() != null ? salvo.getPerfil().getNomePerfil() : null);
        return usuarioMapper.toResponse(salvo);
    }

    private com.tcia.book_dinamico_back_end.domain.model.Perfil buscarPerfilOuFalhar(Long idPerfil) {
        return perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado: " + idPerfil));
    }

    @Transactional
    public UsuarioResponse desativar(Long usuarioId) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);
        if (usuario.getStatus() == UsuarioStatus.DESATIVADO) {
            throw new NegocioException("erro-usuario-ja-desativado");
        }
        usuario.setStatus(UsuarioStatus.DESATIVADO);
        usuario.setAtivo(false);
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário desativado: id={} email={}", salvo.getId(), salvo.getEmail());
        return usuarioMapper.toResponse(salvo);
    }

    @Transactional
    public UsuarioResponse ativar(Long usuarioId) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);
        if (usuario.getStatus() != UsuarioStatus.DESATIVADO) {
            throw new NegocioException("erro-usuario-nao-desativado");
        }
        validarCapAprovados();
        usuario.setStatus(UsuarioStatus.APROVADO);
        usuario.setAtivo(true);
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário reativado: id={} email={}", salvo.getId(), salvo.getEmail());
        return usuarioMapper.toResponse(salvo);
    }
}
