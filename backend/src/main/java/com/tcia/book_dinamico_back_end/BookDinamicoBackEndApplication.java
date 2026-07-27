package com.tcia.book_dinamico_back_end;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories
@EnableScheduling
@EnableAsync
public class BookDinamicoBackEndApplication {

    private static final String DEFAULT_CONTEXT_PATH = "/book_dinamico";

    public static void main(String[] args) {
        normalizeContextPath();
        SpringApplication.run(BookDinamicoBackEndApplication.class, args);
    }

    /**
     * Normaliza o context-path ANTES do boot, setando a System property
     * {@code server.servlet.context-path} (precedência máxima). Roda sempre,
     * sem depender do registro do {@code ContextPathEnvironmentPostProcessor}
     * (que pode não ser registrado se o spring.factories cair fora de
     * BOOT-INF/classes no fat jar).
     *
     * <p>Garante que {@code BOOK_CONTEXT_PATH} vindo como {@code book_dinamico},
     * {@code /book_dinamico/}, {@code ''} etc. nunca quebre o Tomcat com
     * {@code IllegalArgumentException: ContextPath must start with '/' ...}.
     */
    private static void normalizeContextPath() {
        String raw = firstNonBlank(
                System.getenv("BOOK_CONTEXT_PATH"),
                System.getProperty("BOOK_CONTEXT_PATH"),
                System.getenv("BOOK_APP_CONTEXT_PATH"),
                System.getProperty("BOOK_APP_CONTEXT_PATH"),
                System.getProperty("server.servlet.context-path"));

        System.setProperty("server.servlet.context-path", normalize(raw));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return DEFAULT_CONTEXT_PATH;
    }

    static String normalize(String value) {
        String cp = value == null ? "" : value.trim();
        if (cp.isEmpty()) {
            return DEFAULT_CONTEXT_PATH;
        }
        if ("/".equals(cp)) {
            return "";
        }
        if (!cp.startsWith("/")) {
            cp = "/" + cp;
        }
        while (cp.length() > 1 && cp.endsWith("/")) {
            cp = cp.substring(0, cp.length() - 1);
        }
        return cp;
    }
}
