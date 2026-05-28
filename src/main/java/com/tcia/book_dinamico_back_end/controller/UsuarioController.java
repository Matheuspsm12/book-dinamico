package com.tcia.book_dinamico_back_end.controller;

import com.tcia.book_dinamico_back_end.annotations.DocumentarAPI;
import com.tcia.book_dinamico_back_end.controller.request.UsuarioCadastroRequest;
import com.tcia.book_dinamico_back_end.controller.request.UsuarioEdicaoRequest;
import com.tcia.book_dinamico_back_end.controller.request.UsuarioFiltroRequest;
import com.tcia.book_dinamico_back_end.controller.response.UsuarioResponse;
import com.tcia.book_dinamico_back_end.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gerenciamento de usuários do portal")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // -- Público --------------------------------------------------------

    @Operation(summary = "Autocadastro", description = "Cria usuário com status PENDENTE (US2 / RN05–RN08).")
    @DocumentarAPI
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioCadastroRequest request) {
        UsuarioResponse response = usuarioService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -- Admin (US3 / RN09–RN11) ----------------------------------------

    @Operation(summary = "Aprovar usuário pendente",
            description = "Transição PENDENTE→APROVADO. Aplica cap de 40 (RN15/A10/N2). Dispara e-mail.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/aprovar")
    public ResponseEntity<UsuarioResponse> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.aprovar(id));
    }

    @Operation(summary = "Rejeitar usuário pendente",
            description = "Transição PENDENTE→REJEITADO. Dispara e-mail.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<UsuarioResponse> rejeitar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.rejeitar(id));
    }

    // -- Admin (US4 / RN12-RN15) ---------------------------------------

    @Operation(summary = "Paginar usuários",
            description = "Lista paginada com filtros opcionais por status, empresa e nome (RN12/RN13).")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/paginar")
    public ResponseEntity<Page<UsuarioResponse>> paginar(
            @RequestBody(required = false) UsuarioFiltroRequest filtro,
            Pageable pageable) {
        return ResponseEntity.ok(usuarioService.paginar(filtro, pageable));
    }

    @Operation(summary = "Editar usuário",
            description = "Admin edita nome/empresa/email (RN14/A3). Não altera role/senha/status.")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioEdicaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @Operation(summary = "Desativar usuário", description = "Transição → DESATIVADO (RN14).")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/desativar")
    public ResponseEntity<UsuarioResponse> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desativar(id));
    }

    @Operation(summary = "Reativar usuário desativado",
            description = "Transição DESATIVADO → APROVADO. Reentra no cap de 40 (RN15).")
    @DocumentarAPI
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/ativar")
    public ResponseEntity<UsuarioResponse> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.ativar(id));
    }
}
