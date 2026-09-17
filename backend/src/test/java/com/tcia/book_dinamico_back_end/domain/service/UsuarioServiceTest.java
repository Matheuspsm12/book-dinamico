package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.api.request.LoginRequest;
import com.tcia.book_dinamico_back_end.api.request.UsuarioCadastroRequest;
import com.tcia.book_dinamico_back_end.api.response.TokenResponse;
import com.tcia.book_dinamico_back_end.api.response.UsuarioResponse;
import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
import com.tcia.book_dinamico_back_end.core.util.AuthUtils;
import com.tcia.book_dinamico_back_end.domain.exception.ErroAutenticacaoException;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Perfil;
import com.tcia.book_dinamico_back_end.domain.model.ResetSenhaToken;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.domain.repository.PerfilRepository;
import com.tcia.book_dinamico_back_end.domain.repository.ResetSenhaTokenRepository;
import com.tcia.book_dinamico_back_end.domain.repository.UsuarioRepository;
import com.tcia.book_dinamico_back_end.domain.specification.UsuarioSpecifications;
import com.tcia.book_dinamico_back_end.infrastructure.adapter.EmailAdapter;
import com.tcia.book_dinamico_back_end.infrastructure.mapper.UsuarioMapper;
import com.tcia.book_dinamico_back_end.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private UsuarioSpecifications usuarioSpecifications;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailAdapter emailAdapter;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private ResetSenhaTokenRepository resetSenhaTokenRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private AmbienteService ambienteService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UsuarioService usuarioService;

    private Perfil perfilOperador;
    private Perfil perfilAdmin;
    private Usuario usuario;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usuarioService, "urlSite", "https://book.teste.com.br");

        perfilOperador = Perfil.builder()
                .id(2L)
                .nomePerfil("OPERADOR")
                .build();

        perfilAdmin = Perfil.builder()
                .id(1L)
                .nomePerfil("ADMIN")
                .build();

        usuario = Usuario.builder()
                .id(10L)
                .nome("Maria")
                .empresa("TCIA")
                .email("maria@teste.com")
                .senha("hash")
                .justificativa("Preciso acessar")
                .status(UsuarioStatus.APROVADO)
                .ativo(true)
                .perfil(perfilOperador)
                .build();

        admin = Usuario.builder()
                .id(99L)
                .nome("Admin")
                .email("admin@teste.com")
                .status(UsuarioStatus.APROVADO)
                .ativo(true)
                .perfil(perfilAdmin)
                .build();
    }

    @Test
    void autenticarDeveRetornarTokenQuandoCredenciaisEStatusForemValidos() {
        LoginRequest request = loginRequest();
        Instant expiracao = Instant.parse("2026-09-06T15:00:00Z");

        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-correta", "hash")).thenReturn(true);
        when(jwtTokenProvider.gerarToken(usuario, httpRequest)).thenReturn("jwt-token");
        when(jwtTokenProvider.obterExpiracao("jwt-token")).thenReturn(expiracao);
        when(ambienteService.isProducao()).thenReturn(false);

        TokenResponse response = usuarioService.autenticar(request, httpRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getExpiraEm()).isEqualTo(expiracao);
        assertThat(response.getNome()).isEqualTo("Maria");
        assertThat(response.getEmail()).isEqualTo("maria@teste.com");
        assertThat(response.getRole()).isEqualTo("OPERADOR");
        assertThat(response.isProducao()).isFalse();

        verify(usuarioRepository).registrarAcesso(eq(10L), any(LocalDateTime.class));
    }

    @Test
    void autenticarDeveRejeitarSenhaIncorreta() {
        LoginRequest request = loginRequest();

        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-correta", "hash")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.autenticar(request, httpRequest))
                .isInstanceOf(ErroAutenticacaoException.class)
                .hasMessage("erro-credenciais-invalidas");

        verify(jwtTokenProvider, never()).gerarToken(any(), any());
        verify(usuarioRepository, never()).registrarAcesso(any(), any());
    }

    @Test
    void autenticarDeveRejeitarUsuarioPendente() {
        LoginRequest request = loginRequest();
        usuario.setStatus(UsuarioStatus.PENDENTE);

        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-correta", "hash")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.autenticar(request, httpRequest))
                .isInstanceOf(ErroAutenticacaoException.class)
                .hasMessage("erro-conta-pendente");

        verify(jwtTokenProvider, never()).gerarToken(any(), any());
        verify(usuarioRepository, never()).registrarAcesso(any(), any());
    }

    @Test
    void cadastrarDeveCriarUsuarioPendenteComSenhaCriptografadaEPerfilOperador() {
        UsuarioCadastroRequest request = cadastroRequest();
        Usuario novoUsuario = Usuario.builder()
                .nome("Joao")
                .empresa("TCIA")
                .email("joao@teste.com")
                .justificativa("Preciso consultar books")
                .build();
        UsuarioResponse response = UsuarioResponse.builder()
                .id(20L)
                .email("joao@teste.com")
                .status(UsuarioStatus.PENDENTE)
                .role("OPERADOR")
                .build();

        when(usuarioRepository.existsByEmail("joao@teste.com")).thenReturn(false);
        when(usuarioMapper.toEntity(request)).thenReturn(novoUsuario);
        when(passwordEncoder.encode("senha-segura")).thenReturn("senha-hash");
        when(perfilRepository.findByNomePerfil("OPERADOR")).thenReturn(Optional.of(perfilOperador));
        when(usuarioRepository.save(novoUsuario)).thenAnswer(invocation -> {
            Usuario salvo = invocation.getArgument(0);
            salvo.setId(20L);
            return salvo;
        });
        when(usuarioMapper.toResponse(novoUsuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.cadastrar(request);

        assertThat(resultado).isSameAs(response);
        assertThat(novoUsuario.getSenhaHash()).isEqualTo("senha-hash");
        assertThat(novoUsuario.getStatus()).isEqualTo(UsuarioStatus.PENDENTE);
        assertThat(novoUsuario.getPerfil()).isEqualTo(perfilOperador);

        verify(auditoriaService).salvar(any());
    }

    @Test
    void cadastrarDeveRejeitarEmailDuplicado() {
        UsuarioCadastroRequest request = cadastroRequest();

        when(usuarioRepository.existsByEmail("joao@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cadastrar(request))
                .isInstanceOf(NegocioException.class)
                .hasMessage("erro-email-duplicado");

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void aprovarDeveAlterarStatusPerfilEnviarEmailEAuditar() {
        usuario.setStatus(UsuarioStatus.PENDENTE);
        UsuarioResponse response = UsuarioResponse.builder()
                .id(10L)
                .email("maria@teste.com")
                .status(UsuarioStatus.APROVADO)
                .role("ADMIN")
                .build();

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByStatus(UsuarioStatus.APROVADO)).thenReturn(3L);
        when(perfilRepository.findById(1L)).thenReturn(Optional.of(perfilAdmin));
        when(authUtils.getUsuarioLogado()).thenReturn(admin);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.aprovar(10L, 1L);

        assertThat(resultado).isSameAs(response);
        assertThat(usuario.getStatus()).isEqualTo(UsuarioStatus.APROVADO);
        assertThat(usuario.getPerfil()).isEqualTo(perfilAdmin);
        assertThat(usuario.getAprovadoPor()).isEqualTo(admin);
        assertThat(usuario.getDecididoEm()).isNotNull();

        verify(emailAdapter).enviarAprovacao(usuario);
        verify(auditoriaService).salvar(any());
    }

    @Test
    void redefinirSenhaDeveAtualizarSenhaEMarcarTokenComoUsado() {
        ResetSenhaToken resetToken = ResetSenhaToken.builder()
                .token("token-reset")
                .usuario(usuario)
                .expiraEm(LocalDateTime.now().plusMinutes(30))
                .usado(false)
                .build();

        when(resetSenhaTokenRepository.findByToken("token-reset")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("nova-senha")).thenReturn("nova-senha-hash");

        usuarioService.redefinirSenha("token-reset", "nova-senha");

        assertThat(usuario.getSenhaHash()).isEqualTo("nova-senha-hash");
        assertThat(resetToken.getUsado()).isTrue();

        verify(usuarioRepository).save(usuario);
        verify(resetSenhaTokenRepository).save(resetToken);
    }

    @Test
    void recuperarSenhaPorEmailDeveCriarTokenEEnviarLinkQuandoUsuarioExiste() {
        when(emailAdapter.isHabilitado()).thenReturn(true);
        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(usuario));

        usuarioService.recuperarSenhaPorEmail("maria@teste.com");

        ArgumentCaptor<ResetSenhaToken> tokenCaptor = ArgumentCaptor.forClass(ResetSenhaToken.class);
        verify(resetSenhaTokenRepository).save(tokenCaptor.capture());
        ResetSenhaToken resetToken = tokenCaptor.getValue();

        assertThat(resetToken.getUsuario()).isEqualTo(usuario);
        assertThat(resetToken.getToken()).isNotBlank();
        assertThat(resetToken.getExpiraEm()).isAfter(LocalDateTime.now());
        assertThat(resetToken.getUsado()).isFalse();

        verify(emailAdapter).enviarLinkRecuperacao(eq(usuario), anyString());
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("maria@teste.com");
        request.setSenha("senha-correta");
        return request;
    }

    private UsuarioCadastroRequest cadastroRequest() {
        UsuarioCadastroRequest request = new UsuarioCadastroRequest();
        request.setNome("Joao");
        request.setEmpresa("TCIA");
        request.setEmail("joao@teste.com");
        request.setSenha("senha-segura");
        request.setJustificativa("Preciso consultar books");
        return request;
    }
}
