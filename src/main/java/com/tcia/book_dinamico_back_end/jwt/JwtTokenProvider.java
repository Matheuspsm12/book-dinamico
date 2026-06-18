package com.tcia.book_dinamico_back_end.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tcia.book_dinamico_back_end.entity.Usuario;
import com.tcia.book_dinamico_back_end.service.RevogacaoTokenService;
import com.tcia.book_dinamico_back_end.utils.RecuperarIpUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Emissão e validação de JWT. HMAC256, issuer fixo, expiry 30 min (D4).
 * Claims: subject=email, nome, role, status, fp (fingerprint IP+UA — D3), jti.
 * Sem permissoes/lojas como em TCIA — não se aplica ao escopo Book Dinâmico.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String ISSUER = "book-dinamico-app";
    private static final long EXPIRY_MINUTES = 30;

    @Value("${app.jwt-secret}")
    private String secret;

    private Algorithm algorithm;

    private final RevogacaoTokenService revogacaoTokenService;

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String gerarToken(Usuario usuario, HttpServletRequest request) {
        Instant now = Instant.now();
        Instant expira = now.plus(EXPIRY_MINUTES, ChronoUnit.MINUTES);
        String jti = UUID.randomUUID().toString();
        String fingerprint = gerarFingerprint(request);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getEmail())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expira))
                .withJWTId(jti)
                .withClaim("nome", usuario.getNome())
                .withClaim("perfil", usuario.getPerfil().getNomePerfil())
                .withClaim("status", usuario.getStatus().name())
                .withClaim("fp", fingerprint)
                .sign(algorithm);
    }

    public boolean validarToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            String jti = jwt.getId();
            if (jti != null && revogacaoTokenService.isTokenRevogado(jti)) {
                log.warn("Token revogado: {}", jti);
                return false;
            }
            JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
            return true;
        } catch (SignatureVerificationException e) {
            log.warn("Assinatura inválida: {}", e.getMessage());
        } catch (TokenExpiredException e) {
            log.warn("Token expirado: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Erro ao validar token: {}", e.getMessage());
        }
        return false;
    }

    public String obterEmail(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            return jwt.getSubject();
        } catch (Exception e) {
            log.error("Erro ao extrair email do token: {}", e.getMessage());
            return null;
        }
    }

    public void revogarToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            String jti = jwt.getId();
            Date expiraEm = jwt.getExpiresAt();
            if (jti != null && expiraEm != null) {
                revogacaoTokenService.revogarToken(jti, expiraEm.toInstant());
            }
        } catch (Exception e) {
            log.warn("Erro ao revogar token: {}", e.getMessage());
        }
    }

    public Instant obterExpiracao(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            Date expiresAt = jwt.getExpiresAt();
            return expiresAt != null ? expiresAt.toInstant() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String gerarFingerprint(HttpServletRequest request) {
        String ip = RecuperarIpUtils.obterIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : "unknown";
        return UUID.nameUUIDFromBytes((ip + (ua != null ? ua : "")).getBytes()).toString();
    }
}
