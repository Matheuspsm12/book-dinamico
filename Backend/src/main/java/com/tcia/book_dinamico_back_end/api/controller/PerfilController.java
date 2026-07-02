package com.tcia.book_dinamico_back_end.api.controller;

import com.tcia.book_dinamico_back_end.api.request.PerfilRequest;
import com.tcia.book_dinamico_back_end.api.response.PerfilResponse;
import com.tcia.book_dinamico_back_end.core.annotation.DocumentarAPI;
import com.tcia.book_dinamico_back_end.domain.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/perfis")
@RequiredArgsConstructor
@Tag(name = "Perfis", description = "Gerenciamento dos perfis do sistema")
public class PerfilController {

    private final PerfilService service;

    @Operation(summary = "Listar perfis")
    @DocumentarAPI
    @GetMapping
    public ResponseEntity<List<PerfilResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @Operation(summary = "Buscar perfil por ID")
    @DocumentarAPI
    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @Operation(summary = "Criar perfil", description = "Cria um perfil informando suas permissões")
    @DocumentarAPI
    @PostMapping
    public ResponseEntity<PerfilResponse> criar(@Valid @RequestBody PerfilRequest request) {
        PerfilResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/perfis/" + criado.getId())).body(criado);
    }

    @Operation(summary = "Atualizar perfil", description = "Atualiza nome, descrição e permissões do perfil")
    @DocumentarAPI
    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(@PathVariable Long id, @Valid @RequestBody PerfilRequest request) {
        service.atualizar(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Excluir perfil", description = "Exclusão lógica (soft delete) do perfil")
    @DocumentarAPI
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
