package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.infrastructure.mapper.DocumentoMapper;
import com.tcia.book_dinamico_back_end.api.request.DocumentoMetadataRequest;
import com.tcia.book_dinamico_back_end.api.response.DocumentoResponse;
import com.tcia.book_dinamico_back_end.domain.event.ProcessamentoAgendadoEvent;
import com.tcia.book_dinamico_back_end.domain.model.Documento;
import com.tcia.book_dinamico_back_end.domain.model.DocumentoUploadLog;
import com.tcia.book_dinamico_back_end.domain.model.Auditoria;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.core.enums.AuditoriaAcaoEnum;
import com.tcia.book_dinamico_back_end.core.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.domain.exception.ArquivoException;
import com.tcia.book_dinamico_back_end.domain.exception.ErroAutenticacaoException;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.exception.ResourceNotFoundException;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoAbaRepository;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoRepository;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoUploadLogRepository;
import com.tcia.book_dinamico_back_end.core.util.AuthUtils;
import com.tcia.book_dinamico_back_end.core.util.IntegridadeArquivoValidator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoAbaRepository documentoAbaRepository;
    private final DocumentoUploadLogRepository uploadLogRepository;
    private final DocumentoMapper documentoMapper;
    private final ArquivoStorageService storage;
    private final IntegridadeArquivoValidator integridadeValidator;
    private final AuthUtils authUtils;
    private final ProcessamentoService processamentoService;
    private final NotificacaoEmailService notificacaoEmailService;
    private final AuditoriaService auditoriaService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    /** Teto total de armazenamento agregado dos documentos ativos (2 GB). */
    private static final long MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024;

    public List<DocumentoResponse> listar() {
        return documentoMapper.toResponseList(
                documentoRepository.findByAtivoTrueOrderByAtualizadoEmDesc());
    }

    public DocumentoResponse buscar(Long id) {
        return documentoMapper.toResponse(buscarOuFalhar(id));
    }

    public Resource baixar(Long id) {
        Documento doc = buscarOuFalhar(id);
        if (!Boolean.TRUE.equals(doc.getAtivo())) {
            throw new ResourceNotFoundException("Documento não encontrado: " + id);
        }
        return storage.ler(doc.getCaminhoArmazenamento());
    }

    public Documento buscarOuFalhar(Long id) {
        return documentoRepository.findById(id)
                .filter(d -> Boolean.TRUE.equals(d.getAtivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + id));
    }

    @Transactional
    public DocumentoResponse criar(DocumentoMetadataRequest metadata, MultipartFile arquivo) {
        long inicioMs = System.currentTimeMillis();
        String nomeArquivo = arquivo.getOriginalFilename();
        Long documentoId = null;
        ExtensaoDocumento ext = null;
        log.info("Upload iniciado operacao=CRIAR arquivo={} tamanhoBytes={}", nomeArquivo, arquivo.getSize());

        try {
            Usuario admin = adminLogado();
            ext = integridadeValidator.validar(arquivo);
            validarLimiteArmazenamento(arquivo.getSize(), 0L);

            Documento doc = Documento.builder()
                    .nome(metadata.getNome())
                    .descricao(metadata.getDescricao())
                    .dataAtualizacao(metadata.getDataAtualizacao())
                    .tipo(ext.getTipo())
                    .extensao(ext)
                    .tamanhoBytes(arquivo.getSize())
                    .caminhoArmazenamento("__placeholder__")
                    .criadoPor(admin)
                    .atualizadoPor(admin)
                    .ativo(true)
                    .build();

            Documento salvo = documentoRepository.save(doc);
            documentoId = salvo.getId();
            String caminho = storage.gravar(salvo.getId(), ext.name(), arquivo);
            salvo.setCaminhoArmazenamento(caminho);
            salvo = documentoRepository.save(salvo);

            registrarUploadLog(salvo, admin, arquivo.getOriginalFilename());
            var processamento = processamentoService.registrarFila(salvo, admin, arquivo.getOriginalFilename(), arquivo.getContentType());
            eventPublisher.publishEvent(new ProcessamentoAgendadoEvent(processamento.getId()));

            log.info("Documento criado: id={} nome={} tipo={} ext={} tamanho={}",
                    salvo.getId(), salvo.getNome(), salvo.getTipo(), salvo.getExtensao(), salvo.getTamanhoBytes());

            auditar(AuditoriaAcaoEnum.CRIAR_DOCUMENTO, salvo, admin);
            notificacaoEmailService.notificarNovaPublicacao();

            long duracaoMs = duracaoMs(inicioMs);
            registrarMetricasUpload("CRIAR", "SUCESSO", duracaoMs);
            log.info("Upload concluido operacao=CRIAR documentoId={} arquivo={} extensao={} tamanhoBytes={} duracaoMs={}",
                    documentoId, nomeArquivo, ext, arquivo.getSize(), duracaoMs);
            return documentoMapper.toResponse(salvo);
        } catch (RuntimeException e) {
            long duracaoMs = duracaoMs(inicioMs);
            registrarMetricasUpload("CRIAR", "ERRO", duracaoMs);
            log.error("Upload falhou operacao=CRIAR documentoId={} arquivo={} extensao={} tamanhoBytes={} duracaoMs={}",
                    documentoId, nomeArquivo, ext, arquivo.getSize(), duracaoMs, e);
            throw e;
        }
    }

    @Transactional
    public List<DocumentoResponse> criarLote(List<DocumentoMetadataRequest> metadatas, List<MultipartFile> arquivos) {
        if (metadatas == null || arquivos == null || metadatas.size() != arquivos.size() || metadatas.isEmpty()) {
            throw new NegocioException("erro-lote-quantidades-divergentes");
        }
        List<DocumentoResponse> respostas = new ArrayList<>(metadatas.size());
        for (int i = 0; i < metadatas.size(); i++) {
            respostas.add(criar(metadatas.get(i), arquivos.get(i)));
        }
        return respostas;
    }

    @Transactional
    public DocumentoResponse substituirArquivo(Long id, MultipartFile arquivo, String nome, String dataAtualizacao) {
        long inicioMs = System.currentTimeMillis();
        String nomeArquivo = arquivo.getOriginalFilename();
        log.info("Upload iniciado operacao=SUBSTITUIR documentoId={} arquivo={} tamanhoBytes={}",
                id, nomeArquivo, arquivo.getSize());

        try {
            Usuario admin = adminLogado();
            Documento doc = buscarOuFalhar(id);
            ExtensaoDocumento novaExt = integridadeValidator.validar(arquivo);
            // Ao substituir, o tamanho atual do próprio documento é liberado.
            validarLimiteArmazenamento(arquivo.getSize(), doc.getTamanhoBytes());

            String caminhoAntigo = doc.getCaminhoArmazenamento();
            String caminhoNovo = storage.gravar(doc.getId(), novaExt.name(), arquivo);

            doc.setCaminhoArmazenamento(caminhoNovo);
            doc.setTamanhoBytes(arquivo.getSize());
            doc.setExtensao(novaExt);
            doc.setTipo(novaExt.getTipo());
            doc.setAtualizadoPor(admin);
            // A substituição também sincroniza nome/data quando informados, numa única
            // operação (evita um segundo registro de "edição" no histórico).
            if (nome != null && !nome.isBlank()) {
                doc.setNome(nome);
            }
            if (dataAtualizacao != null && !dataAtualizacao.isBlank()) {
                try {
                    doc.setDataAtualizacao(java.time.LocalDate.parse(dataAtualizacao));
                } catch (java.time.format.DateTimeParseException dataInvalida) {
                    throw new NegocioException("erro-data-invalida");
                }
            }

            Documento salvo = documentoRepository.save(doc);
            storage.deletarSeExistir(caminhoAntigo);
            registrarUploadLog(salvo, admin, arquivo.getOriginalFilename());
            var processamento = processamentoService.registrarFila(salvo, admin, arquivo.getOriginalFilename(), arquivo.getContentType());
            eventPublisher.publishEvent(new ProcessamentoAgendadoEvent(processamento.getId()));

            log.info("Arquivo substituído em documento id={}: novo={}", salvo.getId(), caminhoNovo);
            auditar(AuditoriaAcaoEnum.SUBSTITUIR_DOCUMENTO, salvo, admin);
            notificacaoEmailService.notificarNovaPublicacao();
            long duracaoMs = duracaoMs(inicioMs);
            registrarMetricasUpload("SUBSTITUIR", "SUCESSO", duracaoMs);
            log.info("Upload concluido operacao=SUBSTITUIR documentoId={} arquivo={} extensao={} tamanhoBytes={} duracaoMs={}",
                    id, nomeArquivo, novaExt, arquivo.getSize(), duracaoMs);
            return documentoMapper.toResponse(salvo);
        } catch (RuntimeException e) {
            long duracaoMs = duracaoMs(inicioMs);
            registrarMetricasUpload("SUBSTITUIR", "ERRO", duracaoMs);
            log.error("Upload falhou operacao=SUBSTITUIR documentoId={} arquivo={} tamanhoBytes={} duracaoMs={}",
                    id, nomeArquivo, arquivo.getSize(), duracaoMs, e);
            throw e;
        }
    }

    @Transactional
    public DocumentoResponse atualizarMetadados(Long id, DocumentoMetadataRequest request) {
        Documento doc = buscarOuFalhar(id);
        Usuario admin = adminLogado();

        doc.setNome(request.getNome());
        doc.setDescricao(request.getDescricao());
        doc.setDataAtualizacao(request.getDataAtualizacao());
        doc.setAtualizadoPor(admin);

        Documento salvo = documentoRepository.save(doc);
        log.info("Metadata atualizada em documento id={}", salvo.getId());
        auditar(AuditoriaAcaoEnum.ALTERAR_DOCUMENTO, salvo, admin);
        return documentoMapper.toResponse(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        Documento doc = buscarOuFalhar(id);
        Usuario admin = adminLogado();
        String caminho = doc.getCaminhoArmazenamento();
        // Captura o nome antes de excluir — o registro de auditoria precisa sobreviver ao delete.
        auditar(AuditoriaAcaoEnum.EXCLUIR_DOCUMENTO, doc, admin);
        processamentoService.removerPorDocumento(id);
        documentoAbaRepository.deleteByDocumentoId(id);
        documentoRepository.delete(doc);
        // Libera o espaço em disco: o binário não é mais necessário após a exclusão.
        storage.deletarSeExistir(caminho);
        log.info("Documento excluído id={} e arquivo removido do disco: {}", id, caminho);
    }

    private void auditar(AuditoriaAcaoEnum acao, Documento doc, Usuario admin) {
        Auditoria registro = new Auditoria();
        registro.setUsuario(admin != null ? admin.getEmail() : "sistema");
        registro.setAcao(acao.getAcao().name());
        registro.setEntidade(acao.getEntidade().name());
        registro.setEntidadeId(doc.getId());
        registro.setDetalhes(doc.getNome());
        auditoriaService.salvar(registro);
    }

    /**
     * Garante que o total armazenado não ultrapasse {@link #MAX_TOTAL_BYTES}.
     *
     * @param novoTamanho    tamanho do arquivo que será adicionado
     * @param tamanhoLiberado tamanho que será removido na mesma operação (ex.: substituição)
     */
    private void validarLimiteArmazenamento(long novoTamanho, long tamanhoLiberado) {
        long atual = documentoRepository.somarTamanhoBytesAtivos();
        long projetado = atual - tamanhoLiberado + novoTamanho;
        if (projetado > MAX_TOTAL_BYTES) {
            log.warn("Limite de armazenamento excedido: atual={} novo={} liberado={} projetado={} max={}",
                    atual, novoTamanho, tamanhoLiberado, projetado, MAX_TOTAL_BYTES);
            throw new NegocioException("erro-limite-armazenamento");
        }
    }

    private void registrarUploadLog(Documento doc, Usuario admin, String nomeArquivoOriginal) {
        uploadLogRepository.save(DocumentoUploadLog.builder()
                .documento(doc)
                .usuario(admin)
                .nomeArquivo(nomeArquivoOriginal != null ? nomeArquivoOriginal : "(sem nome)")
                .datetime(LocalDateTime.now())
                .build());
    }

    private long duracaoMs(long inicioMs) {
        return System.currentTimeMillis() - inicioMs;
    }

    private void registrarMetricasUpload(String operacao, String resultado, long duracaoMs) {
        meterRegistry.counter("book.documento.upload.total", "operacao", operacao, "resultado", resultado).increment();
        Timer.builder("book.documento.upload.duracao")
                .tag("operacao", operacao)
                .tag("resultado", resultado)
                .register(meterRegistry)
                .record(duracaoMs, TimeUnit.MILLISECONDS);
    }

    private Usuario adminLogado() {
        Usuario admin = authUtils.getUsuarioLogado();
        if (admin == null) {
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }
        return admin;
    }
}

