package com.tcia.book_dinamico_back_end.api.controller;

import com.tcia.book_dinamico_back_end.core.annotation.DocumentarAPI;
import com.tcia.book_dinamico_back_end.api.request.LoginRequest;
import com.tcia.book_dinamico_back_end.api.request.RecuperarSenhaRequest;
import com.tcia.book_dinamico_back_end.api.request.RedefinirSenhaRequest;
import com.tcia.book_dinamico_back_end.api.response.TokenResponse;
import com.tcia.book_dinamico_back_end.domain.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/autenticacao")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e logout")
public class AuthController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Realiza login", description = "Autentica via e-mail e senha. Retorna JWT.")
    @DocumentarAPI
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        TokenResponse token = usuarioService.autenticar(request, httpRequest);
        return ResponseEntity.ok(token);
    }

    @Operation(summary = "Recuperar senha por e-mail",
            description = "Gera uma senha temporária e envia para o e-mail informado, caso exista.")
    @DocumentarAPI
    @PostMapping("/recuperar-senha")
    public ResponseEntity<Void> recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest request) {
        usuarioService.recuperarSenhaPorEmail(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redefinir senha por token",
            description = "Recebe o token enviado por e-mail e a nova senha, e redefine a senha do usuário.")
    @DocumentarAPI
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        usuarioService.redefinirSenha(request.getToken(), request.getNovaSenha());
        return ResponseEntity.noContent().build();
    }
}
