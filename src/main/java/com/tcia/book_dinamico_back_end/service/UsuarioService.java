package com.tcia.book_dinamico_back_end.service;

import com.tcia.book_dinamico_back_end.controller.mapper.UsuarioMapper;
import com.tcia.book_dinamico_back_end.controller.request.LoginRequest;
import com.tcia.book_dinamico_back_end.controller.request.UsuarioCadastroRequest;
import com.tcia.book_dinamico_back_end.controller.request.UsuarioEdicaoRequest;
import com.tcia.book_dinamico_back_end.controller.request.UsuarioFiltroRequest;
import com.tcia.book_dinamico_back_end.controller.response.TokenResponse;
import com.tcia.book_dinamico_back_end.controller.response.UsuarioResponse;
import com.tcia.book_dinamico_back_end.email.EmailAdapter;
import com.tcia.book_dinamico_back_end.entity.Usuario;
import com.tcia.book_dinamico_back_end.repository.PerfilRepository;
import com.tcia.book_dinamico_back_end.enums.UsuarioStatus;
import com.tcia.book_dinamico_back_end.exception.ErroAutenticacaoException;
import com.tcia.book_dinamico_back_end.exception.NegocioException;
import com.tcia.book_dinamico_back_end.security.ResourceNotFoundException;
import com.tcia.book_dinamico_back_end.jwt.JwtTokenProvider;
import com.tcia.book_dinamico_back_end.repository.UsuarioRepository;
import com.tcia.book_dinamico_back_end.repository.specification.UsuarioSpecifications;
import com.tcia.book_dinamico_back_end.utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** Cap de usuários APROVADO simultaneamente (RN15 / A10 / N2). */
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

    // ------------------------------------------------------------------
    // Autenticação (Phase 1 — US1)
    // ------------------------------------------------------------------

    public TokenResponse autenticar(LoginRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ErroAutenticacaoException("erro-credenciais-invalidas"));

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            log.warn("Senha incorreta para {}", request.getEmail());
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }

        validarStatusParaLogin(usuario.getStatus());

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

    // ------------------------------------------------------------------
    // Autocadastro (Phase 2 — US2)
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Aprovação / rejeição (Phase 3 — US3 / RN09-RN11)
    // ------------------------------------------------------------------

    @Transactional
    public UsuarioResponse aprovar(Long usuarioId) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);
        validarStatusPendente(usuario);
        validarCapAprovados();

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
            // não deveria acontecer: endpoint exige hasRole(ADMIN), mas defesa em profundidade
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }
        return admin;
    }

    // ------------------------------------------------------------------
    // Reset de senha por e-mail (usuário logado solicita nova senha)
    // ------------------------------------------------------------------

    /**
     * Gera uma senha temporária aleatória para o usuário autenticado, persiste
     * o hash e dispara e-mail com a senha em texto plano. Idempotente.
     */
    @Transactional
    public void resetarMinhaSenhaPorEmail() {
        Usuario usuario = authUtils.getUsuarioLogado();
        if (usuario == null) {
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }
        // Sem e-mail ativo a nova senha ficaria perdida e o usuário fora do sistema.
        if (!emailAdapter.isHabilitado()) {
            throw new NegocioException("email-desabilitado");
        }
        String senhaTemp = gerarSenhaTemporaria();
        usuario.setSenhaHash(passwordEncoder.encode(senhaTemp));
        usuarioRepository.save(usuario);
        emailAdapter.enviarSenhaTemporaria(usuario, senhaTemp);
        log.info("Senha temporária gerada e enviada para usuário id={}", usuario.getId());
    }

    private static String gerarSenhaTemporaria() {
        // 10 chars: letras maiúsculas + minúsculas + dígitos (sem ambíguos 0/O/1/l/I)
        final String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(alfabeto.charAt(rnd.nextInt(alfabeto.length())));
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Gerenciamento (Phase 4 — US4 / RN12-RN15 / A3)
    // ------------------------------------------------------------------

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
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário atualizado: id={} email={}", salvo.getId(), salvo.getEmail());
        return usuarioMapper.toResponse(salvo);
    }

    @Transactional
    public UsuarioResponse desativar(Long usuarioId) {
        Usuario usuario = buscarPorIdOuFalhar(usuarioId);
        if (usuario.getStatus() == UsuarioStatus.DESATIVADO) {
            throw new NegocioException("erro-usuario-ja-desativado");
        }
        usuario.setStatus(UsuarioStatus.DESATIVADO);
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
        // Reativação reentra no cap de 40 APROVADOs.
        validarCapAprovados();
        usuario.setStatus(UsuarioStatus.APROVADO);
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário reativado: id={} email={}", salvo.getId(), salvo.getEmail());
        return usuarioMapper.toResponse(salvo);
    }
}
