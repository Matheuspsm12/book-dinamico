package com.tcia.book_dinamico_back_end.controller;

import com.tcia.book_dinamico_back_end.annotations.DocumentarAPI;
import com.tcia.book_dinamico_back_end.controller.request.EsqueciSenhaRequest;
import com.tcia.book_dinamico_back_end.controller.request.LoginRequest;
import com.tcia.book_dinamico_back_end.controller.request.ManterAcessoRequest;
import com.tcia.book_dinamico_back_end.controller.request.RedefinirSenhaRequest;
import com.tcia.book_dinamico_back_end.controller.response.TokenResponse;
import com.tcia.book_dinamico_back_end.service.OciosidadeService;
import com.tcia.book_dinamico_back_end.service.UsuarioService;
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
@Tag(name = "Autenticação", description = "Endpoints de login, logout e recuperação de senha")
public class AuthController {

    private final UsuarioService usuarioService;
    private final OciosidadeService ociosidadeService;

    @Operation(summary = "Realiza login", description = "Autentica via e-mail e senha. Retorna JWT.")
    @DocumentarAPI
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        TokenResponse token = usuarioService.autenticar(request, httpRequest);
        return ResponseEntity.ok(token);
    }

    @Operation(summary = "Esqueci minha senha",
            description = "Envia um link de redefinição para o e-mail informado. Responde sempre 204 (não revela se o e-mail existe).")
    @DocumentarAPI
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        usuarioService.solicitarResetSenha(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redefinir senha",
            description = "Define uma nova senha a partir do token recebido por e-mail (single-use).")
    @DocumentarAPI
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        usuarioService.redefinirSenha(request.getToken(), request.getNovaSenha());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Manter meu acesso",
            description = "Confirma interesse em manter o acesso a partir do token do e-mail de ociosidade.")
    @DocumentarAPI
    @PostMapping("/manter-acesso")
    public ResponseEntity<Void> manterAcesso(@Valid @RequestBody ManterAcessoRequest request) {
        ociosidadeService.manterAcesso(request.getToken());
        return ResponseEntity.noContent().build();
    }
}
