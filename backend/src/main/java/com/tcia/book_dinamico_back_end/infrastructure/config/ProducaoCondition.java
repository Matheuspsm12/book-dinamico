package com.tcia.book_dinamico_back_end.infrastructure.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;

@Log4j2
public class ProducaoCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        String[] activeProfiles = context.getEnvironment().getActiveProfiles();

        boolean isProducao = Arrays.stream(activeProfiles).anyMatch(p ->
                p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("pro")
        );

        if (isProducao) {
            log.info("Schedulers ATIVADOS — profile de produção detectado: {}", Arrays.toString(activeProfiles));
        } else {
            log.info("Schedulers DESATIVADOS — profiles ativos: {}", Arrays.toString(activeProfiles));
        }

        return isProducao;
    }
}
