package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.api.request.DocumentoMetadataRequest;
import com.tcia.book_dinamico_back_end.api.response.DocumentoResponse;
import com.tcia.book_dinamico_back_end.core.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.core.enums.TipoDocumento;
import com.tcia.book_dinamico_back_end.core.util.AuthUtils;
import com.tcia.book_dinamico_back_end.core.util.IntegridadeArquivoValidator;
import com.tcia.book_dinamico_back_end.domain.event.ProcessamentoAgendadoEvent;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.exception.ResourceNotFoundException;
import com.tcia.book_dinamico_back_end.domain.model.Documento;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoAbaRepository;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoRepository;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoUploadLogRepository;
import com.tcia.book_dinamico_back_end.infrastructure.mapper.DocumentoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private DocumentoAbaRepository documentoAbaRepository;

    @Mock
    private DocumentoUploadLogRepository uploadLogRepository;

    @Mock
    private DocumentoMapper documentoMapper;

    @Mock
    private ArquivoStorageService storage;

    @Mock
    private IntegridadeArquivoValidator integridadeValidator;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private ProcessamentoService processamentoService;

    @Mock
    private NotificacaoEmailService notificacaoEmailService;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DocumentoService documentoService;

    private SimpleMeterRegistry meterRegistry;
    private Usuario admin;
    private DocumentoMetadataRequest metadata;
    private MockMultipartFile arquivo;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        documentoService = new DocumentoService(
                documentoRepository,
                documentoAbaRepository,
                uploadLogRepository,
                documentoMapper,
                storage,
                integridadeValidator,
                authUtils,
                processamentoService,
                notificacaoEmailService,
                auditoriaService,
                eventPublisher,
                meterRegistry);

        admin = Usuario.builder()
                .id(10L)
                .nome("Admin")
                .email("admin@teste.com")
                .build();

        metadata = new DocumentoMetadataRequest();
        metadata.setNome("Book Comercial");
        metadata.setDescricao("Book mensal");
        metadata.setDataAtualizacao(LocalDate.of(2026, 9, 1));

        arquivo = new MockMultipartFile(
                "arquivo",
                "book.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});
    }

    @Test
    void criarDeveSalvarArquivoRegistrarProcessamentoAuditoriaENotificar() {
        DocumentoResponse response = DocumentoResponse.builder()
                .id(1L)
                .nome("Book Comercial")
                .extensao(ExtensaoDocumento.XLSX)
                .tipo(TipoDocumento.EXCEL)
                .build();

        when(authUtils.getUsuarioLogado()).thenReturn(admin);
        when(integridadeValidator.validar(arquivo)).thenReturn(ExtensaoDocumento.XLSX);
        when(documentoRepository.somarTamanhoBytesAtivos()).thenReturn(0L);
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> {
            Documento documento = invocation.getArgument(0);
            if (documento.getId() == null) {
                documento.setId(1L);
            }
            return documento;
        });
        when(storage.gravar(eq(1L), eq("XLSX"), eq(arquivo))).thenReturn("C:\\uploads\\documento_1.xlsx");
        when(processamentoService.registrarFila(any(Documento.class), eq(admin), eq("book.xlsx"), eq(arquivo.getContentType())))
                .thenReturn(Processamento.builder().id(99L).build());
        when(documentoMapper.toResponse(any(Documento.class))).thenReturn(response);

        DocumentoResponse resultado = documentoService.criar(metadata, arquivo);

        assertThat(resultado).isSameAs(response);

        ArgumentCaptor<Documento> documentoCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository, times(2)).save(documentoCaptor.capture());
        Documento documentoInicial = documentoCaptor.getAllValues().get(0);
        Documento documentoFinal = documentoCaptor.getAllValues().get(1);
        assertThat(documentoInicial.getNome()).isEqualTo("Book Comercial");
        assertThat(documentoInicial.getExtensao()).isEqualTo(ExtensaoDocumento.XLSX);
        assertThat(documentoInicial.getTipo()).isEqualTo(TipoDocumento.EXCEL);
        assertThat(documentoInicial.getTamanhoBytes()).isEqualTo(3L);
        assertThat(documentoInicial.getCriadoPor()).isEqualTo(admin);
        assertThat(documentoFinal.getCaminhoArmazenamento()).isEqualTo("C:\\uploads\\documento_1.xlsx");

        verify(storage).gravar(1L, "XLSX", arquivo);
        verify(uploadLogRepository).save(any());
        ArgumentCaptor<ProcessamentoAgendadoEvent> eventCaptor = ArgumentCaptor.forClass(ProcessamentoAgendadoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().processamentoId()).isEqualTo(99L);
        verify(processamentoService, never()).processarImediato(any(Processamento.class));
        verify(auditoriaService).salvar(any());
        verify(notificacaoEmailService).notificarNovaPublicacao();
        assertThat(meterRegistry.counter("book.documento.upload.total", "operacao", "CRIAR", "resultado", "SUCESSO").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.timer("book.documento.upload.duracao", "operacao", "CRIAR", "resultado", "SUCESSO").count())
                .isEqualTo(1);
    }

    @Test
    void criarDeveBloquearQuandoLimiteDeArmazenamentoForExcedido() {
        long doisGb = 2L * 1024 * 1024 * 1024;

        when(authUtils.getUsuarioLogado()).thenReturn(admin);
        when(integridadeValidator.validar(arquivo)).thenReturn(ExtensaoDocumento.XLSX);
        when(documentoRepository.somarTamanhoBytesAtivos()).thenReturn(doisGb);

        assertThatThrownBy(() -> documentoService.criar(metadata, arquivo))
                .isInstanceOf(NegocioException.class)
                .hasMessage("erro-limite-armazenamento");

        verify(documentoRepository, never()).save(any());
        verify(storage, never()).gravar(any(), any(), any());
        verify(processamentoService, never()).registrarFila(any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(notificacaoEmailService, never()).notificarNovaPublicacao();
        assertThat(meterRegistry.counter("book.documento.upload.total", "operacao", "CRIAR", "resultado", "ERRO").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.timer("book.documento.upload.duracao", "operacao", "CRIAR", "resultado", "ERRO").count())
                .isEqualTo(1);
    }

    @Test
    void criarLoteDeveRejeitarQuantidadesDivergentes() {
        assertThatThrownBy(() -> documentoService.criarLote(List.of(metadata), List.of()))
                .isInstanceOf(NegocioException.class)
                .hasMessage("erro-lote-quantidades-divergentes");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void substituirArquivoDeveAtualizarBinarioMetadadosEProcessar() {
        Documento documento = documentoExistente();
        DocumentoResponse response = DocumentoResponse.builder()
                .id(1L)
                .nome("Book Atualizado")
                .extensao(ExtensaoDocumento.PPTX)
                .tipo(TipoDocumento.POWERPOINT)
                .build();

        when(authUtils.getUsuarioLogado()).thenReturn(admin);
        when(documentoRepository.findById(1L)).thenReturn(Optional.of(documento));
        when(integridadeValidator.validar(arquivo)).thenReturn(ExtensaoDocumento.PPTX);
        when(documentoRepository.somarTamanhoBytesAtivos()).thenReturn(100L);
        when(storage.gravar(1L, "PPTX", arquivo)).thenReturn("C:\\uploads\\documento_1_novo.pptx");
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processamentoService.registrarFila(any(Documento.class), eq(admin), eq("book.xlsx"), eq(arquivo.getContentType())))
                .thenReturn(Processamento.builder().id(100L).build());
        when(documentoMapper.toResponse(any(Documento.class))).thenReturn(response);

        DocumentoResponse resultado = documentoService.substituirArquivo(
                1L,
                arquivo,
                "Book Atualizado",
                "2026-09-05");

        assertThat(resultado).isSameAs(response);
        assertThat(documento.getNome()).isEqualTo("Book Atualizado");
        assertThat(documento.getDataAtualizacao()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(documento.getCaminhoArmazenamento()).isEqualTo("C:\\uploads\\documento_1_novo.pptx");
        assertThat(documento.getExtensao()).isEqualTo(ExtensaoDocumento.PPTX);
        assertThat(documento.getTipo()).isEqualTo(TipoDocumento.POWERPOINT);
        assertThat(documento.getAtualizadoPor()).isEqualTo(admin);

        verify(storage).deletarSeExistir("C:\\uploads\\documento_1_antigo.xlsx");
        verify(uploadLogRepository).save(any());
        ArgumentCaptor<ProcessamentoAgendadoEvent> eventCaptor = ArgumentCaptor.forClass(ProcessamentoAgendadoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().processamentoId()).isEqualTo(100L);
        verify(processamentoService, never()).processarImediato(any(Processamento.class));
        verify(auditoriaService).salvar(any());
        verify(notificacaoEmailService).notificarNovaPublicacao();
        assertThat(meterRegistry.counter("book.documento.upload.total", "operacao", "SUBSTITUIR", "resultado", "SUCESSO").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.timer("book.documento.upload.duracao", "operacao", "SUBSTITUIR", "resultado", "SUCESSO").count())
                .isEqualTo(1);
    }

    @Test
    void deletarDeveRemoverRelacionamentosArquivoEAuditar() {
        Documento documento = documentoExistente();

        when(authUtils.getUsuarioLogado()).thenReturn(admin);
        when(documentoRepository.findById(1L)).thenReturn(Optional.of(documento));

        documentoService.deletar(1L);

        verify(auditoriaService).salvar(any());
        verify(processamentoService).removerPorDocumento(1L);
        verify(documentoAbaRepository).deleteByDocumentoId(1L);
        verify(documentoRepository).delete(documento);
        verify(storage).deletarSeExistir("C:\\uploads\\documento_1_antigo.xlsx");
    }

    @Test
    void buscarOuFalharDeveLancarErroQuandoDocumentoNaoExisteOuEstaInativo() {
        when(documentoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.buscarOuFalhar(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Documento não encontrado: 1");

        Documento inativo = documentoExistente();
        inativo.setAtivo(false);
        when(documentoRepository.findById(2L)).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> documentoService.buscarOuFalhar(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Documento não encontrado: 2");
    }

    private Documento documentoExistente() {
        return Documento.builder()
                .id(1L)
                .nome("Book Comercial")
                .descricao("Book mensal")
                .tipo(TipoDocumento.EXCEL)
                .extensao(ExtensaoDocumento.XLSX)
                .caminhoArmazenamento("C:\\uploads\\documento_1_antigo.xlsx")
                .tamanhoBytes(10L)
                .dataAtualizacao(LocalDate.of(2026, 9, 1))
                .criadoPor(admin)
                .atualizadoPor(admin)
                .ativo(true)
                .build();
    }
}
