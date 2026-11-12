package com.seuxico.sxgestao

import com.seuxico.sxgestao.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class RegrasFinanceirasTest {
    @Test fun descontosEPagamentos() {
        assertEquals(2500L, RegrasFinanceiras.liquido(listOf(3000L to 500L, 3000L to 3000L)))
        RegrasFinanceiras.validarPagamentos(2500, listOf(1000, 1500))
        RegrasFinanceiras.validarPagamentos(0, emptyList())
        listOf(listOf(0L), listOf(-1L), listOf(2499L), listOf(2501L)).forEach { valores ->
            assertThrows(IllegalArgumentException::class.java) { RegrasFinanceiras.validarPagamentos(2500, valores) }
        }
        assertThrows(IllegalArgumentException::class.java) { RegrasFinanceiras.validarPagamentos(0, listOf(1)) }
        assertThrows(IllegalArgumentException::class.java) { RegrasFinanceiras.liquido(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { RegrasFinanceiras.liquido(listOf(1L to 2L)) }
    }
    @Test fun arredondamentoPorAtendimentoSemOverflow() {
        assertEquals(1L, RegrasFinanceiras.comissao(2, 5000))
        assertEquals(2L, RegrasFinanceiras.soma(listOf(RegrasFinanceiras.comissao(1, 5000), RegrasFinanceiras.comissao(1, 5000))))
        assertEquals(3333L, RegrasFinanceiras.comissao(10000, 3333))
        assertEquals(Long.MAX_VALUE, RegrasFinanceiras.comissao(Long.MAX_VALUE, 10000))
        assertEquals(0L, RegrasFinanceiras.comissao(Long.MAX_VALUE, 0))
        assertThrows(ArithmeticException::class.java) { RegrasFinanceiras.soma(listOf(Long.MAX_VALUE, 1)) }
    }
    @Test fun taxas() {
        assertEquals(3333, RegrasFinanceiras.taxaBp("33,33"))
        assertEquals(10000, RegrasFinanceiras.taxaBp("100"))
        listOf("-0.01", "100.01", "33.333").forEach { taxa ->
            assertThrows(IllegalArgumentException::class.java) { RegrasFinanceiras.taxaBp(taxa) }
        }
    }
    @Test fun viradaCivilEFuso() {
        val fuso = ZoneId.of("America/Sao_Paulo")
        val domingo = CalendarioFinanceiro.resolver(LocalDateTime.parse("2023-12-31T23:59:00"), fuso)
        val segunda = CalendarioFinanceiro.resolver(LocalDateTime.parse("2024-01-01T00:00:00"), fuso)
        assertEquals(LocalDate.parse("2023-12-25").toEpochDay(), domingo.semana)
        assertEquals(LocalDate.parse("2024-01-01").toEpochDay(), segunda.semana)
        assertEquals(LocalDate.parse("2024-01-01"), Instant.ofEpochMilli(domingo.instanteMs).atZone(ZoneOffset.UTC).toLocalDate())
    }
    @Test fun transicoesExigemHorarioValidoEOffsetExplicito() {
        val fuso = ZoneId.of("America/New_York")
        assertThrows(IllegalArgumentException::class.java) { CalendarioFinanceiro.resolver(LocalDateTime.parse("2024-03-10T02:30:00"), fuso) }
        val ambiguo = LocalDateTime.parse("2024-11-03T01:30:00")
        assertThrows(IllegalArgumentException::class.java) { CalendarioFinanceiro.resolver(ambiguo, fuso) }
        val a = CalendarioFinanceiro.resolver(ambiguo, fuso, ZoneOffset.ofHours(-4))
        val b = CalendarioFinanceiro.resolver(ambiguo, fuso, ZoneOffset.ofHours(-5))
        assertEquals(3600000L, b.instanteMs - a.instanteMs)
        assertEquals(a.dia, b.dia)
    }
}
