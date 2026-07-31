package com.tcia.book_dinamico_back_end.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Fonte única de verdade sobre "estamos em produção?", alinhada ao mesmo critério
 * usado para ligar os schedulers (profile ativo prod/pro).
 */
@Service
@RequiredArgsConstructor
public class AmbienteService {

    private final Environment environment;

    public boolean isProducao() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("pro"));
    }
}
