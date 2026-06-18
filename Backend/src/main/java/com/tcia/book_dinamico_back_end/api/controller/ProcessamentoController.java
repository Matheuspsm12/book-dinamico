package com.tcia.book_dinamico_back_end.api.controller;

import com.tcia.book_dinamico_back_end.api.assembler.ProcessamentoModelAssembler;
import com.tcia.book_dinamico_back_end.api.response.ProcessamentoResponse;
import com.tcia.book_dinamico_back_end.core.annotation.DocumentarAPI;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoTipo;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import com.tcia.book_dinamico_back_end.domain.service.ProcessamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/processamentos", "/processamento"})
@Tag(name = "Processamentos", description = "Gerenciamento de processamentos")
public class ProcessamentoController {

    private final ProcessamentoService processamentoService;
    private final ProcessamentoModelAssembler assembler;
    private final PagedResourcesAssembler<EntityModel<ProcessamentoResponse>> assemblerPage;

    @Operation(summary = "Buscar processamento por ID",
            description = "Retorna os detalhes de um processamento especifico pelo seu ID.")
    @DocumentarAPI
    @GetMapping("/{id}")
    public EntityModel<ProcessamentoResponse> buscar(@PathVariable Long id) {
        Processamento processamento = processamentoService.buscarPorId(id);
        return assembler.toModel(processamento);
    }

    @Operation(summary = "Listar todos os processamentos",
            description = "Retorna uma lista paginada de todos os processamentos.")
    @DocumentarAPI
    @GetMapping
    public ResponseEntity<?> listar(Pageable pageable) {
        var processamentos = processamentoService.buscarTodos(ordenarPorPadrao(pageable));
        var responsePage = processamentos.map(assembler::toModel);
        return ResponseEntity.ok(assemblerPage.toModel(responsePage));
    }

    @Operation(summary = "Filtrar processamentos",
            description = "Retorna uma lista paginada de processamentos filtrados por tipo.")
    @DocumentarAPI
    @GetMapping("/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam(required = false) String tipoProcessamento,
            Pageable pageable) {

        Integer tipoProcessamentoId = null;
        if (tipoProcessamento != null && !tipoProcessamento.isBlank()) {
            ProcessamentoTipo tipo;
            try {
                tipo = ProcessamentoTipo.valueOf(tipoProcessamento.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body("Tipo de processamento invalido.");
            }
            tipoProcessamentoId = tipo.getCodigo();
        }

        var processamentos = processamentoService.buscarTodos(tipoProcessamentoId, ordenarPorPadrao(pageable));
        var lista = processamentos.stream().map(assembler::toModel).toList();
        var listaPage = new PageImpl<>(lista, processamentos.getPageable(), processamentos.getTotalElements());

        return ResponseEntity.ok(assemblerPage.toModel(listaPage));
    }

    @Operation(summary = "Download de arquivo de processamento",
            description = "Realiza o download do arquivo associado a um processamento.")
    @DocumentarAPI
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Processamento processamento = processamentoService.buscarPorId(id);
        Resource resource = processamentoService.download(id);

        String filename = processamento.getNomeArquivo() != null
                ? processamento.getNomeArquivo()
                : "processamento-%d".formatted(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }

    @Operation(summary = "Reprocessar por ID",
            description = "Solicita o reprocessamento de um item especifico pelo seu ID.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reprocessar/{id}")
    public ResponseEntity<Void> reprocessar(@PathVariable Long id) {
        processamentoService.reprocessar(id, false);
        return ResponseEntity.ok().build();
    }

    private Pageable ordenarPorPadrao(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "dataInicio"));
        }
        return pageable;
    }
}
