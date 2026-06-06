package com.tcia.book_dinamico_back_end.utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * Cálculo de dias úteis (usado no prazo de resposta da ociosidade).
 * Pula sábados e domingos. Feriados estão fora de escopo (ver spec).
 */
public final class DiasUteis {

    private DiasUteis() {
    }

    /** Soma {@code diasUteis} dias úteis a {@code inicio}, pulando fins de semana. */
    public static LocalDateTime somar(LocalDateTime inicio, int diasUteis) {
        LocalDateTime data = inicio;
        int adicionados = 0;
        while (adicionados < diasUteis) {
            data = data.plusDays(1);
            DayOfWeek dia = data.getDayOfWeek();
            if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
                adicionados++;
            }
        }
        return data;
    }
}
