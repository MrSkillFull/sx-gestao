package com.seuxico.sxgestao.model

import java.math.BigDecimal
import java.time.*
import java.util.UUID

object RegrasFinanceiras {
    fun soma(valores: Iterable<Long>): Long = valores.fold(0L, Math::addExact)
    fun liquido(itens: List<Pair<Long, Long>>): Long {
        require(itens.isNotEmpty()) { "Inclua ao menos um serviço." }
        return soma(itens.map { (preco, desconto) ->
            require(preco >= 0 && desconto in 0..preco) { "Desconto fora do preço do item." }
            preco - desconto
        })
    }
    fun validarPagamentos(liquido: Long, valores: List<Long>) {
        require(liquido >= 0)
        require(if (liquido == 0L) valores.isEmpty() else valores.isNotEmpty() && valores.all { it > 0 }) { "Informe pagamentos positivos ou nenhum para atendimento gratuito." }
        require(soma(valores) == liquido) { "Os pagamentos devem quitar o valor líquido." }
    }
    fun comissao(liquido: Long, taxaBp: Int): Long {
        require(liquido >= 0 && taxaBp in 0..10000)
        return Math.addExact(Math.multiplyExact(liquido / 10000, taxaBp.toLong()), ((liquido % 10000) * taxaBp + 5000) / 10000)
    }
    fun taxaBp(percentual: String): Int {
        val valor = BigDecimal(percentual.replace(',', '.'))
        require(valor.scale() <= 2 && valor >= BigDecimal.ZERO && valor <= BigDecimal(100)) { "Taxa deve estar entre 0 e 100%, com até duas casas." }
        return valor.movePointRight(2).intValueExact()
    }
    fun validarId(id: String) { require(UUID.fromString(id).toString() == id) { "UUID inválido." } }
}

data class TempoEfetivo(val instanteMs: Long, val fusoId: String, val offsetSegundos: Int, val dia: Long, val semana: Long)
object CalendarioFinanceiro {
    fun semana(dia: LocalDate): Long = dia.minusDays(dia.dayOfWeek.value - 1L).toEpochDay()
    fun resolver(local: LocalDateTime, fuso: ZoneId, offset: ZoneOffset? = null): TempoEfetivo {
        val offsets = fuso.rules.getValidOffsets(local)
        require(offsets.isNotEmpty()) { "Horário inexistente neste fuso. Escolha outro horário." }
        require(offset != null || offsets.size == 1) { "Horário ambíguo. Informe o deslocamento UTC." }
        val escolhido = offset ?: offsets.single()
        require(escolhido in offsets) { "Deslocamento inválido para este horário." }
        return TempoEfetivo(local.toInstant(escolhido).toEpochMilli(), fuso.id, escolhido.totalSeconds, local.toLocalDate().toEpochDay(), semana(local.toLocalDate()))
    }
}
