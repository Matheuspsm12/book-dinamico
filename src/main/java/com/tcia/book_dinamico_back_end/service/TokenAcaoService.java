package com.tcia.book_dinamico_back_end.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Tokens de ação enviados por e-mail (redefinição de senha, manter acesso por ociosidade).
 * Stateless: JWT HMAC256 assinado com o mesmo {@code app.jwt-secret}, com claim {@code purpose}
 * pra que um token de um propósito não sirva pra outro. Single-use via {@link RevogacaoTokenService}
 * (blacklist do jti após consumo). Obs.: o cache de revogação expira em 30 min — suficiente pro
 * RESET_SENHA (TTL 30 min); pro MANTER_ACESSO o consumo é idempotente, então reuso é inofensivo.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TokenAcaoService {

    public enum Proposito { RESET_SENHA, MANTER_ACESSO }

    private static final String ISSUER = "book-app-acao";
    private static final String CLAIM_PURPOSE = "purpose";

    @Value("${app.jwt-secret}")
    private String secret;

    private Algorithm algorithm;

    private final RevogacaoTokenService revogacaoTokenService;

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    /** Gera um token assinado pro usuário com o propósito e validade informados. */
    public String gerar(Long usuarioId, Proposito proposito, Duration ttl) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(usuarioId))
                .withClaim(CLAIM_PURPOSE, proposito.name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(ttl)))
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }

    /**
     * Valida assinatura, issuer, propósito, expiração e blacklist (single-use).
     * Retorna o id do usuário se válido; {@code Optional.empty()} caso contrário.
     */
    public Optional<Long> validar(String token, Proposito proposito) {
        try {
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withClaim(CLAIM_PURPOSE, proposito.name())
                    .build()
                    .verify(token);
            String jti = jwt.getId();
            if (jti != null && revogacaoTokenService.isTokenRevogado(jti)) {
                log.warn("Token de ação já utilizado: jti={}", jti);
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(jwt.getSubject()));
        } catch (Exception e) {
            log.warn("Token de ação inválido (propósito={}): {}", proposito, e.getMessage());
            return Optional.empty();
        }
    }

    /** Marca o token como usado (single-use), revogando o jti até a expiração. */
    public void consumir(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).withIssuer(ISSUER).build().verify(token);
            if (jwt.getId() != null && jwt.getExpiresAt() != null) {
                revogacaoTokenService.revogarToken(jwt.getId(), jwt.getExpiresAt().toInstant());
            }
        } catch (Exception e) {
            log.warn("Falha ao consumir token de ação: {}", e.getMessage());
        }
    }
}
