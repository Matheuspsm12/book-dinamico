package com.tcia.book_dinamico_back_end.core.annotation;

import com.tcia.book_dinamico_back_end.core.enums.AuditoriaAcaoEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditar {

    String mensagem() default "Ação realizada no sistema";

    AuditoriaAcaoEnum acao();

    boolean detalhes() default true;
}
