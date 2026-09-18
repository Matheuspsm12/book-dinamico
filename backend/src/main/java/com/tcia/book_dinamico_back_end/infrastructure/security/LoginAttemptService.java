package com.tcia.book_dinamico_back_end.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Controla tentativas de login por e-mail e por IP, bloqueando temporariamente
 * após uma sequência de falhas (mitigação de força bruta em /autenticacao/login).
 * Estado é mantido em memória (Caffeine) e expira automaticamente.
 */
@Service
@Log4j2
public class LoginAttemptService {

    private static final String PREFIXO_EMAIL = "email:";
    private static final String PREFIXO_IP = "ip:";

    @Value("${app.login.max-tentativas-email:5}")
    private int maxTentativasEmail;

    @Value("${app.login.max-tentativas-ip:20}")
    private int maxTentativasIp;

    @Value("${app.login.janela-minutos:15}")
    private long janelaMinutos;

    @Value("${app.login.bloqueio-minutos:15}")
    private long bloqueioMinutos;

    private Cache<String, Integer> tentativasCache;
    private Cache<String, Boolean> bloqueioCache;

    @PostConstruct
    void initCache() {
        tentativasCache = Caffeine.newBuilder()
                .expireAfterWrite(janelaMinutos, TimeUnit.MINUTES)
                .maximumSize(50_000)
                .build();
        bloqueioCache = Caffeine.newBuilder()
                .expireAfterWrite(bloqueioMinutos, TimeUnit.MINUTES)
                .maximumSize(50_000)
                .build();
    }

    public boolean estaBloqueado(String email, String ip) {
        return bloqueioCache.getIfPresent(chaveEmail(email)) != null
                || bloqueioCache.getIfPresent(chaveIp(ip)) != null;
    }

    public void registrarFalha(String email, String ip) {
        registrarFalha(chaveEmail(email), maxTentativasEmail, email);
        registrarFalha(chaveIp(ip), maxTentativasIp, ip);
    }

    public void registrarSucesso(String email, String ip) {
        tentativasCache.invalidate(chaveEmail(email));
        tentativasCache.invalidate(chaveIp(ip));
        bloqueioCache.invalidate(chaveEmail(email));
        bloqueioCache.invalidate(chaveIp(ip));
    }

    private void registrarFalha(String chave, int maxTentativas, String referencia) {
        Integer atual = tentativasCache.getIfPresent(chave);
        int novo = (atual == null ? 0 : atual) + 1;
        tentativasCache.put(chave, novo);
        if (novo >= maxTentativas) {
            bloqueioCache.put(chave, true);
            log.warn("Tentativas excedidas para {} ({}); login bloqueado por {} minuto(s)",
                    referencia == null ? "desconhecido" : referencia, novo, bloqueioMinutos);
        }
    }

    private String chaveEmail(String email) {
        return PREFIXO_EMAIL + (email != null ? email.toLowerCase() : "desconhecido");
    }

    private String chaveIp(String ip) {
        return PREFIXO_IP + (ip != null ? ip : "desconhecido");
    }
}