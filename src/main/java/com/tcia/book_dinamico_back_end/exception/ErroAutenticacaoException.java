package com.tcia.book_dinamico_back_end.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ErroAutenticacaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String chave;

    public ErroAutenticacaoException(String chave) {
        super(chave);
        this.chave = chave;
    }
}
