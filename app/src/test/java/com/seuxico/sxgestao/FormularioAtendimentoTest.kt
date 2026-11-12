package com.seuxico.sxgestao

import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.ui.atendimentos.*
import org.junit.Assert.*
import org.junit.Test

class FormularioAtendimentoTest {
    private val b = Barbeiro(nome = "João")
    private val s = Servico(nome = "Corte", precoCentavos = 3000)
    private fun form() = AtendimentoForm(b.id, "29/02/2028", "12:30", "America/Sao_Paulo", itens = listOf(ItemForm(s.id, desconto = "5,00"), ItemForm(s.id, desconto = "30,00")))
    @Test fun repeticaoDescontosEDivisaoExata() {
        val f = form().copy(dividido = true, partes = listOf(ParteForm(valor = "10,00"), ParteForm(valor = "15,00")))
        val a = avaliarAtendimento(f, listOf(b), listOf(s))
        assertTrue(a.erros.toString(), a.erros.isEmpty())
        assertEquals(2500L, a.liquido)
        assertEquals(6000L, a.subtotal)
        assertEquals(3500L, a.descontos)
        assertEquals(listOf(1000L, 1500L), a.pagamentos.map { it.valorCentavos })
        assertTrue(avaliarAtendimento(f.copy(partes = listOf(ParteForm(valor = "24,99"))), listOf(b), listOf(s)).erros.containsKey("pagamento"))
    }
    @Test fun gratisDatasInvalidasECadastrosInativos() {
        val gratuito = form().copy(itens = listOf(ItemForm(s.id, desconto = "30,00")))
        val a = avaliarAtendimento(gratuito, listOf(b), listOf(s))
        assertEquals(0L, a.liquido)
        assertTrue(a.pagamentos.isEmpty())
        assertTrue(avaliarAtendimento(form().copy(data = "31/02/2028"), listOf(b), listOf(s)).erros.containsKey("data"))
        assertTrue(avaliarAtendimento(form().copy(hora = "24:00"), listOf(b), listOf(s)).erros.containsKey("hora"))
        assertTrue(avaliarAtendimento(form(), listOf(b.copy(ativo = false)), listOf(s.copy(ativo = false))).erros.size >= 2)
    }
    @Test fun correcaoPreservaPrecoETaxaHistoricosERevalidaNovaAssociacao() {
        val item = ItemAtendimento(novoId(), novoId(), s.id, 3000, 500)
        val tempo = com.seuxico.sxgestao.model.CalendarioFinanceiro.resolver(java.time.LocalDateTime.parse("2024-01-03T12:00:00"), java.time.ZoneOffset.UTC)
        val a = AtendimentoCompleto(Atendimento(item.atendimentoId, b.id, tempo.instanteMs, tempo.fusoId, tempo.offsetSegundos, tempo.dia, tempo.semana, 5000),
            listOf(item), listOf(Pagamento(novoId(), item.atendimentoId, "PIX", 2500)))
        val f = a.formulario()
        val atual = s.copy(ativo = false, precoCentavos = 9000)
        val resultado = avaliarAtendimento(f, listOf(b.copy(ativo = false)), listOf(atual), a)
        assertTrue(resultado.erros.toString(), resultado.erros.isEmpty())
        assertEquals(2500L, resultado.liquido)
        assertEquals(a.pagamentos.single().id, resultado.pagamentos.single().id)
        val novo = ItemForm(s.id)
        assertTrue(avaliarAtendimento(f.copy(itens = f.itens + novo), listOf(b), listOf(atual), a).erros.containsKey(novo.id))
    }
}
