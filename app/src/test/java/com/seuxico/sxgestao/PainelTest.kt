package com.seuxico.sxgestao

import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.ui.inicio.*
import org.junit.Assert.*
import org.junit.Test

class PainelTest {
    @Test fun parcelaUsaComissaoApuradaESomaSemMultiplicarItensEPartes() {
        val id = novoId()
        val b = novoId()
        val a = AtendimentoCompleto(Atendimento(id, b, 0, "UTC", 0, 0, -3, 3000),
            listOf(ItemAtendimento(novoId(), id, novoId(), 6000, 0), ItemAtendimento(novoId(), id, novoId(), 4000, 0)),
            listOf(Pagamento(novoId(), id, "PIX", 2500), Pagamento(novoId(), id, "DEBITO", 7500)))
        val r = resumir(listOf(a, a.copy(atendimento = a.atendimento.copy(id = novoId(), cancelado = true))))
        assertEquals(10000L, r.liquido)
        assertEquals(3000L, r.comissoes[b])
        assertEquals(7000L, r.parcelaBarbearia)
        assertEquals(1, r.quantidade)
        assertEquals(10000L, r.recebimentos.values.sum())
        assertEquals(0L, resumir(emptyList()).parcelaBarbearia)
    }
}
