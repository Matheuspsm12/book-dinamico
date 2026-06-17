package br.com.tcia.bookdinamico.controller;

import br.com.tcia.bookdinamico.annotations.DocumentarAPI;
import br.com.tcia.bookdinamico.controller.request.LoginRequest;
import br.com.tcia.bookdinamico.controller.response.TokenResponse;
import br.com.tcia.bookdinamico.service.UsuarioService;
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
}
