package com.tcia.book_dinamico_back_end.api.controller;

import com.tcia.book_dinamico_back_end.api.request.PermissaoRequest;
import com.tcia.book_dinamico_back_end.api.response.PermissaoResponse;
import com.tcia.book_dinamico_back_end.core.annotation.DocumentarAPI;
import com.tcia.book_dinamico_back_end.domain.service.PermissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/permissoes")
@RequiredArgsConstructor
@Tag(name = "Permissões", description = "Gerenciamento das permissões do sistema")
public class PermissaoController {

    private final PermissaoService service;

    @Operation(summary = "Listar permissões")
    @DocumentarAPI
    @GetMapping
    public ResponseEntity<List<PermissaoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @Operation(summary = "Criar permissão")
    @DocumentarAPI
    @PostMapping
    public ResponseEntity<PermissaoResponse> criar(@Valid @RequestBody PermissaoRequest request) {
        PermissaoResponse criada = service.criar(request);
        return ResponseEntity.created(URI.create("/api/permissoes/" + criada.getId())).body(criada);
    }

    @Operation(summary = "Atualizar permissão")
    @DocumentarAPI
    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(@PathVariable Long id, @Valid @RequestBody PermissaoRequest request) {
        service.atualizar(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Excluir permissão")
    @DocumentarAPI
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
