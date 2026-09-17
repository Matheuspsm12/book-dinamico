package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.core.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoResultado;
import com.tcia.book_dinamico_back_end.core.enums.TipoDocumento;
import com.tcia.book_dinamico_back_end.core.util.UsuarioLogadoUtil;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Documento;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.domain.repository.ProcessamentoRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessamentoServiceTest {

    @Mock
    private ProcessamentoRepository processamentoRepository;

    @Mock
    private ExtracaoConteudoService extracaoConteudoService;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private UsuarioLogadoUtil usuarioLogadoUtil;

    private SimpleMeterRegistry meterRegistry;
    private ProcessamentoService processamentoService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        processamentoService = new ProcessamentoService(
                processamentoRepository,
                extracaoConteudoService,
                auditoriaService,
                usuarioLogadoUtil,
                meterRegistry);
    }

    @Test
    void processarImediatoDeveRegistrarMetricasDeSucesso() {
        Processamento processamento = processamento();
        when(processamentoRepository.save(any(Processamento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioLogadoUtil.getEmailUsuarioLogado()).thenReturn("admin@teste.com");

        processamentoService.processarImediato(processamento);

        assertThat(processamento.getExecutado()).isTrue();
        assertThat(processamento.getResultado()).isEqualTo(ProcessamentoResultado.SUCESSO.name());
        assertThat(meterRegistry.counter("book.processamento.total", "resultado", "SUCESSO").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer("book.processamento.duracao", "resultado", "SUCESSO").count()).isEqualTo(1);

        verify(extracaoConteudoService).extrair(processamento);
        verify(auditoriaService).salvar(any());
    }

    @Test
    void processarImediatoDeveRegistrarMetricasDeErro() {
        Processamento processamento = processamento();
        processamento.setQtdReprocessar(3);
        processamento.setQtdReprocessado(3);
        doThrow(new NegocioException("arquivo-invalido")).when(extracaoConteudoService).extrair(processamento);
        when(processamentoRepository.save(any(Processamento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioLogadoUtil.getEmailUsuarioLogado()).thenReturn("admin@teste.com");

        processamentoService.processarImediato(processamento);

        assertThat(processamento.getExecutado()).isTrue();
        assertThat(processamento.getResultado()).isEqualTo(ProcessamentoResultado.ERRO.name());
        assertThat(processamento.getResultadoAmigavel()).isEqualTo("arquivo-invalido");
        assertThat(meterRegistry.counter("book.processamento.total", "resultado", "ERRO").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer("book.processamento.duracao", "resultado", "ERRO").count()).isEqualTo(1);

        verify(auditoriaService).salvar(any());
    }

    @Test
    void buscarUltimoPorDocumentoDeveRetornarProcessamentoMaisRecente() {
        Processamento processamento = processamento();
        when(processamentoRepository.findFirstByDocumentoIdOrderByDataStartDescIdDesc(1L))
                .thenReturn(Optional.of(processamento));

        Processamento resultado = processamentoService.buscarUltimoPorDocumento(1L);

        assertThat(resultado).isSameAs(processamento);
    }

    @Test
    void buscarUltimoPorDocumentoDeveFalharQuandoNaoExistirProcessamento() {
        when(processamentoRepository.findFirstByDocumentoIdOrderByDataStartDescIdDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> processamentoService.buscarUltimoPorDocumento(1L))
                .isInstanceOf(NegocioException.class)
                .hasMessage("processamento-nao-encontrado");
    }

    private Processamento processamento() {
        Documento documento = Documento.builder()
                .id(1L)
                .nome("Book Comercial")
                .descricao("Book mensal")
                .tipo(TipoDocumento.EXCEL)
                .extensao(ExtensaoDocumento.XLSX)
                .caminhoArmazenamento("C:\\uploads\\book.xlsx")
                .tamanhoBytes(1024L)
                .dataAtualizacao(LocalDate.of(2026, 9, 1))
                .ativo(true)
                .build();

        Usuario usuario = Usuario.builder()
                .id(10L)
                .email("admin@teste.com")
                .build();

        return Processamento.builder()
                .id(99L)
                .nomeArquivo("book.xlsx")
                .arquivoAProcessar("C:\\uploads\\book.xlsx")
                .executado(false)
                .reprocessar(false)
                .qtdReprocessar(0)
                .qtdReprocessado(0)
                .usuario(usuario)
                .documento(documento)
                .build();
    }
}
