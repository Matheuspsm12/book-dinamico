package com.tcia.book_dinamico_back_end.infrastructure.config;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class ContextPathEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULT_CONTEXT_PATH = "/book_dinamico";
    private static final String PROPERTY_SOURCE_NAME = "normalizedContextPath";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String contextPath = firstText(
                environment.getProperty("BOOK_CONTEXT_PATH"),
                environment.getProperty("BOOK_APP_CONTEXT_PATH"),
                environment.getProperty("server.servlet.context-path"));

        String normalizedContextPath = normalizeContextPath(contextPath);

        environment.getPropertySources().addFirst(new MapPropertySource(
                PROPERTY_SOURCE_NAME,
                Map.of("server.servlet.context-path", normalizedContextPath)));
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return DEFAULT_CONTEXT_PATH;
    }

    private String normalizeContextPath(String value) {
        String contextPath = value.trim();
        if (!StringUtils.hasText(contextPath)) {
            return DEFAULT_CONTEXT_PATH;
        }
        if ("/".equals(contextPath)) {
            return "";
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        while (contextPath.length() > 1 && contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        return contextPath;
    }
}
