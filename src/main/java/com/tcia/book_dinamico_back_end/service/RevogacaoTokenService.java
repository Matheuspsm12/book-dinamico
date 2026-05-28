package com.tcia.book_dinamico_back_end.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Blacklist de tokens JWT revogados (logout).
 * In-memory via Caffeine — match padrão TCIA. Entradas expiram automaticamente após 30 min
 * (mesmo TTL do JWT — D4). Risco aceito: tokens revogados ressuscitam após restart do app.
 */
@Service
@Log4j2
public class RevogacaoTokenService {

    private Cache<String, Boolean> revogacaoCache;

    @PostConstruct
    public void initCache() {
        revogacaoCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public void revogarToken(String jti, Instant expiraEm) {
        revogacaoCache.put(jti, true);
        log.info("Token JTI revogado: {} (expira em {})", jti, expiraEm);
    }

    public boolean isTokenRevogado(String jti) {
        return revogacaoCache.getIfPresent(jti) != null;
    }
}
