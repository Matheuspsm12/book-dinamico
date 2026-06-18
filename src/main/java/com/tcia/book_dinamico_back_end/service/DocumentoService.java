package com.tcia.book_dinamico_back_end.service;

import com.tcia.book_dinamico_back_end.controller.mapper.DocumentoMapper;
import com.tcia.book_dinamico_back_end.controller.request.DocumentoMetadataRequest;
import com.tcia.book_dinamico_back_end.controller.response.DocumentoResponse;
import com.tcia.book_dinamico_back_end.entity.Documento;
import com.tcia.book_dinamico_back_end.entity.DocumentoUploadLog;
import com.tcia.book_dinamico_back_end.entity.Usuario;
import com.tcia.book_dinamico_back_end.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.exception.ArquivoException;
import com.tcia.book_dinamico_back_end.exception.ErroAutenticacaoException;
import com.tcia.book_dinamico_back_end.exception.NegocioException;
import com.tcia.book_dinamico_back_end.security.ResourceNotFoundException;
import com.tcia.book_dinamico_back_end.repository.DocumentoRepository;
import com.tcia.book_dinamico_back_end.repository.DocumentoUploadLogRepository;
import com.tcia.book_dinamico_back_end.utils.AuthUtils;
import com.tcia.book_dinamico_back_end.utils.IntegridadeArquivoValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5 — US6 + US7. Upload/edit/replace/listagem/download de documentos.
 * Endpoints admin gateados por {@code @PreAuthorize("hasRole('ADMIN')")} no controller.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoUploadLogRepository uploadLogRepository;
    private final DocumentoMapper documentoMapper;
    private final ArquivoStorageService storage;
    private final IntegridadeArquivoValidator integridadeValidator;
    private final AuthUtils authUtils;

    // ------------------------------------------------------------------
    // Listagem (US6 — RN16/RN19/RN29) — disponível pra qualquer autenticado
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Upload (US7 — RN20-RN32) — admin only
    // ------------------------------------------------------------------

    @Transactional
    public DocumentoResponse criar(DocumentoMetadataRequest metadata, MultipartFile arquivo) {
        Usuario admin = adminLogado();
        ExtensaoDocumento ext = integridadeValidator.validar(arquivo);

        Documento doc = Documento.builder()
                .nome(metadata.getNome())
                .descricao(metadata.getDescricao())
                .dataAtualizacao(metadata.getDataAtualizacao())
                .tipo(ext.getTipo())
                .extensao(ext)
                .tamanhoBytes(arquivo.getSize())
                .caminhoArmazenamento("__placeholder__") // gravado abaixo após ter id
                .criadoPor(admin)
                .atualizadoPor(admin)
                .ativo(true)
                .build();

        Documento salvo = documentoRepository.save(doc);
        String caminho = storage.gravar(salvo.getId(), ext.name(), arquivo);
        salvo.setCaminhoArmazenamento(caminho);
        salvo = documentoRepository.save(salvo);

        registrarUploadLog(salvo, admin, arquivo.getOriginalFilename());

        log.info("Documento criado: id={} nome={} tipo={} ext={} tamanho={}",
                salvo.getId(), salvo.getNome(), salvo.getTipo(), salvo.getExtensao(), salvo.getTamanhoBytes());
        return documentoMapper.toResponse(salvo);
    }

    @Transactional
    public List<DocumentoResponse> criarLote(List<DocumentoMetadataRequest> metadatas, List<MultipartFile> arquivos) {
        if (metadatas == null || arquivos == null || metadatas.size() != arquivos.size() || metadatas.isEmpty()) {
            throw new NegocioException("erro-lote-quantidades-divergentes");
        }
        List<DocumentoResponse> respostas = new ArrayList<>(metadatas.size());
        // Falha em um → rollback de todos (transação @Transactional)
        for (int i = 0; i < metadatas.size(); i++) {
            respostas.add(criar(metadatas.get(i), arquivos.get(i)));
        }
        return respostas;
    }

    /**
     * Substituição de versão (RN25/RN26/N1). Apenas o binário muda; metadata é preservada.
     * O arquivo antigo é apagado do filesystem após o novo ser gravado com sucesso.
     */
    @Transactional
    public DocumentoResponse substituirArquivo(Long id, MultipartFile arquivo) {
        Usuario admin = adminLogado();
        Documento doc = buscarOuFalhar(id);
        ExtensaoDocumento novaExt = integridadeValidator.validar(arquivo);

        String caminhoAntigo = doc.getCaminhoArmazenamento();
        String caminhoNovo = storage.gravar(doc.getId(), novaExt.name(), arquivo);

        doc.setCaminhoArmazenamento(caminhoNovo);
        doc.setTamanhoBytes(arquivo.getSize());
        doc.setExtensao(novaExt);
        doc.setTipo(novaExt.getTipo());
        doc.setAtualizadoPor(admin);
        // dataAtualizacao NÃO é tocada — é campo manual do admin (A6).

        Documento salvo = documentoRepository.save(doc);
        storage.deletarSeExistir(caminhoAntigo);
        registrarUploadLog(salvo, admin, arquivo.getOriginalFilename());

        log.info("Arquivo substituído em documento id={}: novo={}", salvo.getId(), caminhoNovo);
        return documentoMapper.toResponse(salvo);
    }

    /** Edita só metadata — não toca no binário (PUT /{id}). */
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
        return documentoMapper.toResponse(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        Documento doc = buscarOuFalhar(id);
        documentoRepository.delete(doc); // dispara @SQLDelete (soft delete)
        log.info("Documento soft-deletado id={}", id);
    }

    // ------------------------------------------------------------------

    private void registrarUploadLog(Documento doc, Usuario admin, String nomeArquivoOriginal) {
        uploadLogRepository.save(DocumentoUploadLog.builder()
                .documento(doc)
                .usuario(admin)
                .nomeArquivo(nomeArquivoOriginal != null ? nomeArquivoOriginal : "(sem nome)")
                .datetime(LocalDateTime.now())
                .build());
    }

    private Usuario adminLogado() {
        Usuario admin = authUtils.getUsuarioLogado();
        if (admin == null) {
            throw new ErroAutenticacaoException("erro-credenciais-invalidas");
        }
        return admin;
    }
}
