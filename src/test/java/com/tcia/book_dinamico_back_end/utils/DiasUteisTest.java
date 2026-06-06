package com.tcia.book_dinamico_back_end.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit do cálculo de dias úteis usado no prazo da ociosidade (Item 1). */
class DiasUteisTest {

    @Test
    void somaDentroDaSemana() {
        // Segunda 2026-06-01 + 2 dias úteis = Quarta 2026-06-03
        LocalDateTime segunda = LocalDateTime.of(2026, Month.JUNE, 1, 8, 0);
        assertEquals(LocalDateTime.of(2026, Month.JUNE, 3, 8, 0), DiasUteis.somar(segunda, 2));
    }

    @Test
    void pulaFimDeSemana() {
        // Sexta 2026-06-05 + 2 dias úteis pula sáb/dom = Terça 2026-06-09
        LocalDateTime sexta = LocalDateTime.of(2026, Month.JUNE, 5, 8, 0);
        assertEquals(LocalDateTime.of(2026, Month.JUNE, 9, 8, 0), DiasUteis.somar(sexta, 2));
    }

    @Test
    void zeroDiasMantemAData() {
        LocalDateTime quando = LocalDateTime.of(2026, Month.JUNE, 1, 8, 0);
        assertEquals(quando, DiasUteis.somar(quando, 0));
    }

    @Test
    void sabadoMaisUmDiaUtilCaiNaSegunda() {
        // Sábado 2026-06-06 + 1 dia útil = Segunda 2026-06-08
        LocalDateTime sabado = LocalDateTime.of(2026, Month.JUNE, 6, 8, 0);
        assertEquals(LocalDateTime.of(2026, Month.JUNE, 8, 8, 0), DiasUteis.somar(sabado, 1));
    }
}
