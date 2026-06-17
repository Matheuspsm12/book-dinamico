package br.com.tcia.bookdinamico.controller.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiErroResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
