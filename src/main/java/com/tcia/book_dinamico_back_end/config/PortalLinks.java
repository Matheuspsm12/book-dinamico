package com.tcia.book_dinamico_back_end.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Constrói URLs do front-end usadas em e-mails (links de ação).
 * {@code app.url-front-end} pode ser uma lista separada por vírgula (origens CORS) —
 * usamos a primeira como base canônica. Tokens JWT são url-safe (base64url), sem encoding extra.
 */
@Component
public class PortalLinks {

    private final String base;

    public PortalLinks(@Value("${app.url-front-end}") String urlFrontEnd) {
        this.base = urlFrontEnd.split(",")[0].trim().replaceAll("/+$", "");
    }

    public String portal() {
        return base;
    }

    public String redefinirSenha(String token) {
        return base + "/redefinir-senha?token=" + token;
    }

    public String manterAcesso(String token) {
        return base + "/manter-acesso?token=" + token;
    }
}
