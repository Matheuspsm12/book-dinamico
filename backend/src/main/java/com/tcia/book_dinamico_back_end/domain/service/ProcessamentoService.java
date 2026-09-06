package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.core.enums.AuditoriaAcaoEnum;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoResultado;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoTipo;
import com.tcia.book_dinamico_back_end.core.util.UsuarioLogadoUtil;
import com.tcia.book_dinamico_back_end.domain.model.Auditoria;
import com.tcia.book_dinamico_back_end.domain.exception.ArquivoException;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Documento;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.domain.repository.ProcessamentoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProcessamentoService {

    private final ProcessamentoRepository processamentoRepository;
    private final ExtracaoConteudoService extracaoConteudoService;
    private final AuditoriaService auditoriaService;
    private final UsuarioLogadoUtil usuarioLogadoUtil;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public Processamento buscarPorId(Long id) {
        return processamentoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("processamento-nao-encontrado"));
    }

    @Transactional(readOnly = true)
    public Processamento buscarUltimoPorDocumento(Long documentoId) {
        return processamentoRepository.findFirstByDocumentoIdOrderByDataStartDescIdDesc(documentoId)
                .orElseThrow(() -> new NegocioException("processamento-nao-encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<Processamento> buscarTodos(Pageable pageable) {
        return processamentoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Processamento> buscarTodos(Integer tipoProcessamentoId, Pageable pageable) {
        if (tipoProcessamentoId != null) {
            return processamentoRepository.findByTipoProcessamento(tipoProcessamentoId, pageable);
        }
        return processamentoRepository.findAll(pageable);
    }

    @Transactional
    public Processamento registrarFila(
            Documento documento,
            Usuario usuario,
            String nomeArquivoOriginal,
            String contentType) {

        String nomeArquivo = nomeArquivoOriginal != null && !nomeArquivoOriginal.isBlank()
                ? nomeArquivoOriginal
                : documento.getNome();

        Processamento processamento = Processamento.builder()
                .nomeArquivo(nomeArquivo)
                .contentType(contentType)
                .tipoProcessamento(ProcessamentoTipo.DOCUMENTO.getCodigo())
                .executado(false)
                .reprocessar(false)
                .qtdReprocessar(0)
                .qtdReprocessado(0)
                .resultado(ProcessamentoResultado.AGENDADO.name())
                .resultadoAmigavel(ProcessamentoResultado.AGENDADO.getDescricao())
                .parametro(String.valueOf(documento.getId()))
                .arquivoAProcessar(documento.getCaminhoArmazenamento())
                .tamanho(formatarTamanho(documento.getTamanhoBytes()))
                .usuario(usuario)
                .documento(documento)
                .build();

        return processamentoRepository.save(processamento);
    }

    @Transactional
    public void verificarProcessamento() {
        processamentoRepository.findByExecutadoFalseOrReprocessarTrue().forEach(this::processar);
    }

    @Transactional
    public void processarImediato(Processamento processamento) {
        processar(processamento);
    }

    @Transactional
    public void processarPorId(Long id) {
        processar(buscarPorId(id));
    }

    private void processar(Processamento p) {
        long inicioMs = System.currentTimeMillis();
        Long documentoId = p.getDocumento() != null ? p.getDocumento().getId() : null;
        try {
            log.info("Processamento iniciado id={} documentoId={} arquivo={}",
                    p.getId(), documentoId, p.getNomeArquivo());
            p.setDataInicio(LocalDateTime.now());

            extracaoConteudoService.extrair(p);

            p.setArquivoProcessado(p.getArquivoAProcessar());
            p.setExecutado(true);
            p.setReprocessar(false);
            p.setResultado(ProcessamentoResultado.SUCESSO.name());
            p.setResultadoAmigavel(ProcessamentoResultado.SUCESSO.getDescricao());
            p.setDataFim(LocalDateTime.now());

            processamentoRepository.save(p);
            auditar(p, "SUCESSO", p.getNomeArquivo());
            long duracaoMs = duracaoMs(inicioMs);
            registrarMetricas("SUCESSO", duracaoMs);
            log.info("Processamento concluido id={} documentoId={} arquivo={} resultado={} duracaoMs={}",
                    p.getId(), documentoId, p.getNomeArquivo(), p.getResultado(), duracaoMs);
        } catch (Exception e) {
            long duracaoMs = duracaoMs(inicioMs);
            registrarMetricas("ERRO", duracaoMs);
            log.error("Processamento falhou id={} documentoId={} arquivo={} duracaoMs={}",
                    p.getId(), documentoId, p.getNomeArquivo(), duracaoMs, e);
            p.setExecutado(true);
            p.setReprocessar(false);
            p.setResultado(ProcessamentoResultado.ERRO.name());
            p.setResultadoAmigavel(e instanceof NegocioException
                    ? e.getMessage()
                    : ProcessamentoResultado.ERRO.getDescricao());
            p.setDataFim(LocalDateTime.now());
            processamentoRepository.save(p);
            auditar(p, "ERRO", e.getMessage());
        }
    }

    private long duracaoMs(long inicioMs) {
        return System.currentTimeMillis() - inicioMs;
    }

    private void registrarMetricas(String resultado, long duracaoMs) {
        meterRegistry.counter("book.processamento.total", "resultado", resultado).increment();
        Timer.builder("book.processamento.duracao")
                .tag("resultado", resultado)
                .register(meterRegistry)
                .record(duracaoMs, TimeUnit.MILLISECONDS);
    }

    private void auditar(Processamento p, String status, String detalhe) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuarioLogadoUtil.getEmailUsuarioLogado());
        auditoria.setAcao(AuditoriaAcaoEnum.PROCESSAR_DOCUMENTO.getAcao().name());
        auditoria.setEntidade(AuditoriaAcaoEnum.PROCESSAR_DOCUMENTO.getEntidade().name());
        auditoria.setEntidadeId(p.getId());
        auditoria.setDetalhes(status + " - " + detalhe);
        auditoriaService.salvar(auditoria);
    }

    @Transactional
    public Processamento registrarDocumentoProcessado(
            Documento documento,
            Usuario usuario,
            String nomeArquivoOriginal,
            String contentType) {

        LocalDateTime agora = LocalDateTime.now();
        String nomeArquivo = nomeArquivoOriginal != null && !nomeArquivoOriginal.isBlank()
                ? nomeArquivoOriginal
                : documento.getNome();

        Processamento processamento = Processamento.builder()
                .nomeArquivo(nomeArquivo)
                .contentType(contentType)
                .dataInicio(agora)
                .dataFim(agora)
                .tipoProcessamento(ProcessamentoTipo.DOCUMENTO.getCodigo())
                .executado(true)
                .reprocessar(false)
                .qtdReprocessar(0)
                .qtdReprocessado(0)
                .resultado(ProcessamentoResultado.SUCESSO.name())
                .resultadoAmigavel(ProcessamentoResultado.SUCESSO.getDescricao())
                .parametro(String.valueOf(documento.getId()))
                .arquivoAProcessar(documento.getCaminhoArmazenamento())
                .arquivoProcessado(documento.getCaminhoArmazenamento())
                .tamanho(formatarTamanho(documento.getTamanhoBytes()))
                .usuario(usuario)
                .documento(documento)
                .build();

        return processamentoRepository.save(processamento);
    }

    @Transactional(readOnly = true)
    public Resource download(Long id) {
        Processamento processamento = buscarPorId(id);
        String caminho = ProcessamentoResultado.SUCESSO.getDescricao().equals(processamento.getResultadoAmigavel())
                ? processamento.getArquivoProcessado()
                : processamento.getArquivoAProcessar();

        if (caminho == null || caminho.isBlank()) {
            throw new ArquivoException("arquivo-nao-encontrado");
        }

        try {
            Resource resource = new UrlResource(Path.of(caminho).normalize().toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ArquivoException("arquivo-nao-encontrado");
        } catch (MalformedURLException e) {
            throw new ArquivoException("erro-download-arquivo", e);
        }
    }

    @Transactional
    public void reprocessar(Long id, boolean marcarReprocessar) {
        Processamento processamento = buscarPorId(id);
        processamento.setExecutado(false);
        processamento.setReprocessar(marcarReprocessar);
        processamento.setResultado(ProcessamentoResultado.REPROCESSAMENTO_AGENDADO.name());
        processamento.setResultadoAmigavel(ProcessamentoResultado.REPROCESSAMENTO_AGENDADO.getDescricao());
        processamento.setQtdReprocessado(processamento.getQtdReprocessado() + 1);
        processamento.setDataFim(null);
        processamentoRepository.save(processamento);
        processar(processamento);
    }

    @Transactional
    public void removerPorDocumento(Long documentoId) {
        processamentoRepository.deleteByDocumentoId(documentoId);
    }

    private String formatarTamanho(Long bytes) {
        if (bytes == null) {
            return "0B";
        }
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            return "%dKB".formatted(bytes / 1024);
        }
        return "%dMB".formatted(bytes / (1024 * 1024));
    }
}
