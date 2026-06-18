package com.tcia.book_dinamico_back_end.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ArquivoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String chave;

    public ArquivoException(String chave) {
        super(chave);
        this.chave = chave;
    }

    public ArquivoException(String chave, Throwable causa) {
        super(chave, causa);
        this.chave = chave;
    }
}
