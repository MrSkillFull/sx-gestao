package com.seuxico.sxgestao.ui.atendimentos

import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import com.seuxico.sxgestao.model.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

val formatoData: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
val formatoHora: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)
data class ItemForm(val servicoId: String, val id: String = novoId(), val desconto: String = "0,00")
data class ParteForm(val id: String = novoId(), val forma: FormaPagamento = FormaPagamento.PIX, val valor: String = "")
data class AtendimentoForm(val barbeiroId: String = "", val data: String, val hora: String,
    val fuso: String, val offset: String? = null, val itens: List<ItemForm> = emptyList(),
    val dividido: Boolean = false, val formaUnica: FormaPagamento = FormaPagamento.PIX,
    val partes: List<ParteForm> = emptyList())
data class AvaliacaoForm(val erros: Map<String, String>, val subtotal: Long? = null,
    val descontos: Long? = null, val liquido: Long? = null, val local: LocalDateTime? = null,
    val offset: ZoneOffset? = null, val pagamentos: List<PagamentoEntrada> = emptyList())

fun AtendimentoCompleto.horarioLocal(): LocalDateTime = LocalDateTime.ofInstant(
    Instant.ofEpochMilli(atendimento.efetivoUtcMs), ZoneOffset.ofTotalSeconds(atendimento.offsetEfetivoSegundos))

fun AtendimentoCompleto.formulario(): AtendimentoForm {
    val local = horarioLocal()
    return AtendimentoForm(atendimento.barbeiroId, local.format(formatoData), local.format(formatoHora), atendimento.fusoId,
        ZoneOffset.ofTotalSeconds(atendimento.offsetEfetivoSegundos).toString(),
        itens.map { ItemForm(it.servicoId, it.id, decimal(it.descontoCentavos)) }, pagamentos.size > 1,
        pagamentos.firstOrNull()?.let { FormaPagamento.valueOf(it.forma) } ?: FormaPagamento.PIX,
        pagamentos.map { ParteForm(it.id, FormaPagamento.valueOf(it.forma), decimal(it.valorCentavos)) })
}

fun avaliarAtendimento(form: AtendimentoForm, barbeiros: List<Barbeiro>, servicos: List<Servico>, original: AtendimentoCompleto? = null): AvaliacaoForm {
    val erros = linkedMapOf<String, String>()
    if (barbeiros.none { it.id == form.barbeiroId && (it.ativo || original?.atendimento?.barbeiroId == it.id) }) erros["barbeiro"] = "Selecione um barbeiro ativo."
    val data = runCatching { LocalDate.parse(form.data, formatoData) }.getOrNull()
    val hora = runCatching { LocalTime.parse(form.hora, formatoHora) }.getOrNull()
    if (data == null) erros["data"] = "Informe uma data existente em dd/mm/aaaa."
    if (hora == null) erros["hora"] = "Informe uma hora válida em hh:mm."
    val local = if (data != null && hora != null) LocalDateTime.of(data, hora) else null
    val offset = form.offset?.let { runCatching { ZoneOffset.of(it) }.getOrNull() }
    val tempoPreservado = original != null && local == original.horarioLocal().withSecond(0).withNano(0) &&
        (offset == null || offset.totalSeconds == original.atendimento.offsetEfetivoSegundos)
    if (local != null && !tempoPreservado) runCatching { CalendarioFinanceiro.resolver(local, ZoneId.of(form.fuso), offset) }
        .exceptionOrNull()?.let { erros["hora"] = it.message ?: "Confira a data e o horário." }
    if (form.itens.isEmpty()) erros["itens"] = "Inclua ao menos um serviço."
    val precos = mutableListOf<Long>()
    val descontos = mutableListOf<Long>()
    form.itens.forEach { item ->
        val mantido = original?.itens?.find { it.id == item.id && it.servicoId == item.servicoId }
        val servico = servicos.find { it.id == item.servicoId }
        val preco = mantido?.precoAplicadoCentavos ?: servico?.precoCentavos
        val desconto = runCatching { centavos(item.desconto) }.getOrNull()
        if (servico == null || (mantido == null && !servico.ativo)) erros[item.id] = "Selecione um serviço ativo."
        else if (preco == null || desconto == null || desconto !in 0..preco) erros[item.id] = "Informe desconto entre zero e ${reais(preco ?: 0)}."
        if (preco != null) precos += preco
        if (desconto != null) descontos += desconto
    }
    val subtotal = runCatching { RegrasFinanceiras.soma(precos) }.getOrNull()
    val descontoTotal = runCatching { RegrasFinanceiras.soma(descontos) }.getOrNull()
    val liquido = if (form.itens.isNotEmpty() && form.itens.none { erros.containsKey(it.id) }) runCatching {
        RegrasFinanceiras.liquido(precos.zip(descontos))
    }.getOrNull() else null
    if (subtotal == null || descontoTotal == null || (form.itens.isNotEmpty() && liquido == null && form.itens.none { erros.containsKey(it.id) })) erros["itens"] = "O total excede o limite permitido."
    val pagamentos = mutableListOf<PagamentoEntrada>()
    if (liquido != null && liquido > 0) {
        if (!form.dividido) pagamentos += PagamentoEntrada(form.formaUnica, liquido, original?.pagamentos?.singleOrNull()?.id ?: novoId())
        else {
            form.partes.forEach { parte ->
                val valor = runCatching { centavos(parte.valor).also { require(it > 0) } }.getOrNull()
                if (valor == null) erros[parte.id] = "Informe uma parte positiva com até duas casas."
                else pagamentos += PagamentoEntrada(parte.forma, valor, parte.id)
            }
            if (form.partes.any { erros.containsKey(it.id) } || runCatching { RegrasFinanceiras.validarPagamentos(liquido, pagamentos.map { it.valorCentavos }) }.isFailure)
                erros["pagamento"] = "As partes devem quitar exatamente ${reais(liquido)}."
        }
    }
    return AvaliacaoForm(erros, subtotal, descontoTotal, liquido, local, offset, pagamentos)
}
