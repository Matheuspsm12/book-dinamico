package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.infrastructure.mapper.DocumentoMapper;
import com.tcia.book_dinamico_back_end.api.request.DocumentoMetadataRequest;
import com.tcia.book_dinamico_back_end.api.response.DocumentoResponse;
import com.tcia.book_dinamico_back_end.domain.model.Documento;
import com.tcia.book_dinamico_back_end.domain.model.DocumentoUploadLog;
import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.core.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.domain.exception.ArquivoException;
import com.tcia.book_dinamico_back_end.domain.exception.ErroAutenticacaoException;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.exception.ResourceNotFoundException;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoRepository;
import com.tcia.book_dinamico_back_end.domain.repository.DocumentoUploadLogRepository;
import com.tcia.book_dinamico_back_end.core.util.AuthUtils;
import com.tcia.book_dinamico_back_end.core.util.IntegridadeArquivoValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final ProcessamentoService processamentoService;

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
        Usuario admin = adminLogado();
        ExtensaoDocumento ext = integridadeValidator.validar(arquivo);

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
        String caminho = storage.gravar(salvo.getId(), ext.name(), arquivo);
        salvo.setCaminhoArmazenamento(caminho);
        salvo = documentoRepository.save(salvo);

        registrarUploadLog(salvo, admin, arquivo.getOriginalFilename());
        processamentoService.registrarFila(salvo, admin, arquivo.getOriginalFilename(), arquivo.getContentType());

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
        for (int i = 0; i < metadatas.size(); i++) {
            respostas.add(criar(metadatas.get(i), arquivos.get(i)));
        }
        return respostas;
    }

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

        Documento salvo = documentoRepository.save(doc);
        storage.deletarSeExistir(caminhoAntigo);
        registrarUploadLog(salvo, admin, arquivo.getOriginalFilename());
        processamentoService.registrarFila(salvo, admin, arquivo.getOriginalFilename(), arquivo.getContentType());

        log.info("Arquivo substituído em documento id={}: novo={}", salvo.getId(), caminhoNovo);
        return documentoMapper.toResponse(salvo);
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
        return documentoMapper.toResponse(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        Documento doc = buscarOuFalhar(id);
        documentoRepository.delete(doc);
        log.info("Documento soft-deletado id={}", id);
    }

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

