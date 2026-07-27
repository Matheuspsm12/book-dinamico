package com.tcia.book_dinamico_back_end.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

/**
 * Normaliza o context-path do servidor a partir de {@code BOOK_CONTEXT_PATH}.
 *
 * <p>O binding de {@code server.servlet.context-path} via
 * {@code @ConfigurationProperties} resolve apenas {@code ${...}} e NÃO avalia
 * SpEL, então valores mal formatados (sem barra inicial, com barra final, ou
 * string vazia setada explicitamente) quebram o boot do Tomcat com
 * {@code IllegalArgumentException: ContextPath must start with '/' and not end with '/'}.
 *
 * <p>Por isso o context-path é definido aqui, em código, garantindo:
 * <ul>
 *   <li>barra inicial adicionada quando ausente ({@code book_dinamico} -&gt; {@code /book_dinamico});</li>
 *   <li>barra(s) final(is) removida(s) ({@code /book_dinamico/} -&gt; {@code /book_dinamico});</li>
 *   <li>valor vazio ou {@code "/"} vira {@code ""} (raiz), que é válido para o Tomcat.</li>
 * </ul>
 */
@Component
public class ContextPathNormalizer
        implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private final String rawContextPath;

    public ContextPathNormalizer(
            @Value("${BOOK_CONTEXT_PATH:/book_dinamico}") String rawContextPath) {
        this.rawContextPath = rawContextPath;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        if (factory instanceof ConfigurableServletWebServerFactory servletFactory) {
            servletFactory.setContextPath(normalize(rawContextPath));
        }
    }

    static String normalize(String value) {
        String cp = value == null ? "" : value.trim();
        if (cp.isEmpty() || cp.equals("/")) {
            return "";
        }
        if (!cp.startsWith("/")) {
            cp = "/" + cp;
        }
        while (cp.endsWith("/")) {
            cp = cp.substring(0, cp.length() - 1);
        }
        return cp;
    }
}
