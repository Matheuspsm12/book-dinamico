package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoResultado;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoTipo;
import com.tcia.book_dinamico_back_end.domain.exception.ArquivoException;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Documento;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.domain.repository.ProcessamentoRepository;
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

@Log4j2
@Service
@RequiredArgsConstructor
public class ProcessamentoService {

    private final ProcessamentoRepository processamentoRepository;
    private final ExtracaoConteudoService extracaoConteudoService;

    @Transactional(readOnly = true)
    public Processamento buscarPorId(Long id) {
        return processamentoRepository.findById(id)
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

    private void processar(Processamento p) {
        try {
            log.info("Processando item {}: {}", p.getId(), p.getNomeArquivo());
            p.setDataInicio(LocalDateTime.now());

            extracaoConteudoService.extrair(p);

            p.setArquivoProcessado(p.getArquivoAProcessar());
            p.setExecutado(true);
            p.setReprocessar(false);
            p.setResultado(ProcessamentoResultado.SUCESSO.name());
            p.setResultadoAmigavel(ProcessamentoResultado.SUCESSO.getDescricao());
            p.setDataFim(LocalDateTime.now());

            processamentoRepository.save(p);
            log.info("Item {} processado com sucesso.", p.getId());
        } catch (Exception e) {
            log.error("Erro ao processar item {}: {}", p.getId(), e.getMessage());
            p.setExecutado(true);
            p.setReprocessar(false);
            p.setResultado(ProcessamentoResultado.ERRO.name());
            p.setResultadoAmigavel(e instanceof NegocioException
                    ? e.getMessage()
                    : ProcessamentoResultado.ERRO.getDescricao());
            p.setDataFim(LocalDateTime.now());
            processamentoRepository.save(p);
        }
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
