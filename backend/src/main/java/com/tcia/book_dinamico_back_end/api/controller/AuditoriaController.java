package com.tcia.book_dinamico_back_end.api.controller;

import com.tcia.book_dinamico_back_end.api.response.AuditoriaResponse;
import com.tcia.book_dinamico_back_end.core.annotation.DocumentarAPI;
import com.tcia.book_dinamico_back_end.domain.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Histórico de ações no sistema")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Histórico de documentos",
            description = "Lista paginada das ações (criação, edição, substituição e exclusão) sobre documentos, mais recentes primeiro.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/documentos")
    public ResponseEntity<Page<AuditoriaResponse>> historicoDocumentos(Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.listarHistoricoDocumentos(pageable));
    }
}
