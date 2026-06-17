package br.com.tcia.bookdinamico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(BookDinamicoBackEndApplication.class);

    public static void main(String[] args) {
        logger.info("BOOK_APP_NAME após iniciar: {}", System.getenv("BOOK_APP_NAME"));
        logger.info("BOOK_APP_CONTEXT_PATH após iniciar: {}", System.getenv("BOOK_APP_CONTEXT_PATH"));
        logger.info("BOOK_APP_PORT após iniciar: {}", System.getenv("BOOK_APP_PORT"));
        logger.info("BOOK_URL_SITE após iniciar: {}", System.getenv("BOOK_URL_SITE"));
        logger.info("BOOK_URL_FRONT_END após iniciar: {}", System.getenv("BOOK_URL_FRONT_END"));
        logger.info("BOOK_AMBIENTE após iniciar: {}", System.getenv("BOOK_AMBIENTE"));
        logger.info("BOOK_DB_URL após iniciar: {}", System.getenv("BOOK_DB_URL"));
        logger.info("BOOK_DB_USERNAME após iniciar: {}", System.getenv("BOOK_DB_USERNAME"));
        logger.info("BOOK_DIRETORIO após iniciar: {}", System.getenv("BOOK_DIRETORIO"));

        SpringApplication.run(BookDinamicoBackEndApplication.class, args);
    }
}
