package com.seuxico.sxgestao

import com.seuxico.sxgestao.model.*
import org.junit.Assert.*
import org.junit.Test

class FormatosTest {
    @Test fun entradaExataSemArredondamento() {
        assertEquals(1230L, centavos("12,30"))
        assertEquals(30L, Math.addExact(centavos("0,10"), centavos("0,20")))
        assertEquals(Long.MAX_VALUE, centavos("92233720368547758,07"))
        listOf("1,234", "1e2", "-1", "", "1.234,56", "92233720368547758,08").forEach {
            assertTrue("Deveria rejeitar $it", runCatching { centavos(it) }.isFailure)
        }
        assertEquals("33,33", decimal(3333))
    }
}
