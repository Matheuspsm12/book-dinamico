package com.tcia.book_dinamico_back_end.api.controller;

import com.tcia.book_dinamico_back_end.api.response.LogConsultaResponse;
import com.tcia.book_dinamico_back_end.core.annotation.DocumentarAPI;
import com.tcia.book_dinamico_back_end.domain.service.DiagnosticoLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/diagnostico")
@RequiredArgsConstructor
@Tag(name = "Diagnóstico", description = "Inspeção operacional da aplicação (somente ADMIN)")
public class DiagnosticoController {

    private final DiagnosticoLogService diagnosticoLogService;

    @Operation(summary = "Tail do log da aplicação",
            description = "Retorna as últimas linhas do arquivo de log (book.log), já estruturadas por "
                    + "nível/logger/mensagem, com stack traces agrupados. Aceita filtro por nível mínimo "
                    + "e busca textual, e permite escolher um arquivo rotacionado.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/logs")
    public ResponseEntity<LogConsultaResponse> logs(
            @RequestParam(defaultValue = "300") int linhas,
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String arquivo) {
        return ResponseEntity.ok(diagnosticoLogService.consultar(linhas, nivel, busca, arquivo));
    }
}
