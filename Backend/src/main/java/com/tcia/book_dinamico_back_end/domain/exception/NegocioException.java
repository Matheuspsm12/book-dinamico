package com.tcia.book_dinamico_back_end.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String chave;

    @Getter
    private Object[] args;

    public NegocioException(String chave) {
        super(chave);
        this.chave = chave;
    }

    public NegocioException(String chave, Object... args) {
        super(chave);
        this.chave = chave;
        this.args = args;
    }
}
