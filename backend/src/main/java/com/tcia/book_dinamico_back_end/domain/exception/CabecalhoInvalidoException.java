package com.tcia.book_dinamico_back_end.domain.exception;

public class CabecalhoInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CabecalhoInvalidoException(Exception e) {
        super(e);
    }

    public CabecalhoInvalidoException(String msm) {
        super(msm);
    }
}
