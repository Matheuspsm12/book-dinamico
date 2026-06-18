package com.tcia.book_dinamico_back_end.domain.exception;

public class ErroGenericoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErroGenericoException(Exception e) {
        super(e);
    }

    public ErroGenericoException(String msm) {
        super(msm);
    }
}
