package br.com.tcia.bookdinamico.controller;

import br.com.tcia.bookdinamico.annotations.DocumentarAPI;
import br.com.tcia.bookdinamico.controller.request.DocumentoMetadataRequest;
import br.com.tcia.bookdinamico.controller.response.DocumentoResponse;
import br.com.tcia.bookdinamico.entity.Documento;
import br.com.tcia.bookdinamico.service.DocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Catálogo e upload de documentos do Book Dinâmico")
public class DocumentoController {

    private final DocumentoService documentoService;

    // -- Usuários autenticados (US5 — RN16/RN17) ------------------------

    @Operation(summary = "Listar documentos", description = "Documentos ativos ordenados por última atualização.")
    @DocumentarAPI
    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> listar() {
        return ResponseEntity.ok(documentoService.listar());
    }

    @Operation(summary = "Obter detalhes do documento")
    @DocumentarAPI
    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.buscar(id));
    }

    @Operation(summary = "Baixar binário do documento",
            description = "Streama o arquivo com Content-Disposition attachment (US5 / RN16).")
    @DocumentarAPI
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> baixar(@PathVariable Long id) {
        Documento doc = documentoService.buscarOuFalhar(id);
        Resource r = documentoService.baixar(id);
        String filename = doc.getNome().replaceAll("\\s+", "_") + "." + doc.getExtensao().name().toLowerCase();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(r);
    }

    // -- Admin (US7 — RN20-RN32) ---------------------------------------

    @Operation(summary = "Upload de um documento",
            description = "Multipart: 'metadata' (JSON) + 'arquivo' (binário). Valida extensão, magic bytes, tamanho. Admin only.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> criar(
            @RequestPart("metadata") @Valid DocumentoMetadataRequest metadata,
            @RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.criar(metadata, arquivo));
    }

    @Operation(summary = "Upload em lote",
            description = "Multipart: 'metadata' (JSON array) + 'arquivos' (lista de binários). "
                    + "Quantidades devem casar. Admin only. RN23/N6.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/lote", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentoResponse>> criarLote(
            @RequestPart("metadata") @Valid List<DocumentoMetadataRequest> metadatas,
            @RequestPart("arquivos") List<MultipartFile> arquivos) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoService.criarLote(metadatas, arquivos));
    }

    @Operation(summary = "Substituir binário",
            description = "Substitui o arquivo de um documento existente. Metadata é preservada. RN25/N1.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> substituirArquivo(
            @PathVariable Long id,
            @RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(documentoService.substituirArquivo(id, arquivo));
    }

    @Operation(summary = "Editar metadata", description = "Atualiza nome/descricao/dataAtualizacao. Não toca no binário.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DocumentoResponse> atualizarMetadados(
            @PathVariable Long id,
            @Valid @RequestBody DocumentoMetadataRequest request) {
        return ResponseEntity.ok(documentoService.atualizarMetadados(id, request));
    }

    @Operation(summary = "Soft delete", description = "Esconde o documento (ativo=false via @SQLDelete).")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        documentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
