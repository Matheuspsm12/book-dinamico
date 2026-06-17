package br.com.tcia.bookdinamico.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class EmailException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailException(String mensagem) {
        super(mensagem);
    }

    public EmailException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
