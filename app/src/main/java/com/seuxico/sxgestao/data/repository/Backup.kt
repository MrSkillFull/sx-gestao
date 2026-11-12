package com.seuxico.sxgestao.data.repository

import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.model.*
import java.security.MessageDigest
import java.time.*
import org.json.*

internal data class Retrato(val barbeiros: List<Barbeiro>, val servicos: List<Servico>, val atendimentos: List<Atendimento>,
    val itens: List<ItemAtendimento>, val pagamentos: List<Pagamento>, val confirmacoes: List<PagamentoSemanal>)

/** Cópia isolada: só o codec pode criar um candidato que passou por todas as validações. */
class BackupValidado internal constructor(internal val dados: Retrato) {
    val quantidadeAtendimentos: Int get() = dados.atendimentos.size
}

internal object BackupCodec {
    private val tabelas = setOf("barbeiros", "servicos", "atendimentos", "itens", "pagamentos", "confirmacoes")
    private fun hash(payload: String) = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun objeto(texto: String): JSONObject {
        val leitor = JSONTokener(texto)
        val objeto = leitor.nextValue() as? JSONObject ?: error("Objeto JSON ausente.")
        require(leitor.nextClean() == '\u0000') { "Conteúdo adicional inesperado." }
        return objeto
    }
    private fun JSONObject.chaves(esperadas: Set<String>) { require(keys().asSequence().toSet() == esperadas) { "Campos ausentes ou incompatíveis." } }
    private fun JSONArray.texto(i: Int): String = (get(i) as? String) ?: error("Texto esperado.")
    private fun JSONArray.numero(i: Int): Long {
        val valor = get(i)
        require(valor is Int || valor is Long) { "Número inteiro esperado." }
        return (valor as Number).toLong()
    }
    private fun JSONArray.inteiro(i: Int): Int = numero(i).also { require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) }.toInt()
    private fun JSONArray.booleano(i: Int): Boolean = (get(i) as? Boolean) ?: error("Indicador booleano esperado.")
    private fun <T> JSONObject.linhas(nome: String, colunas: Int, criar: (JSONArray) -> T): List<T> {
        val tabela = get(nome) as? JSONArray ?: error("Tabela $nome ausente.")
        return (0 until tabela.length()).map { indice ->
            val linha = tabela.get(indice) as? JSONArray ?: error("Linha inválida em $nome.")
            require(linha.length() == colunas) { "Linha incompleta ou incompatível em $nome." }
            criar(linha)
        }
    }
    fun exportar(dados: Retrato): String {
        validarDados(dados)
        fun linhas(valores: List<List<Any>>) = JSONArray(valores.map { JSONArray(it) })
        val payload = JSONObject()
            .put("barbeiros", linhas(dados.barbeiros.map { listOf(it.id, it.nome, it.ativo, it.proprietario, it.taxaComissaoBp) }))
            .put("servicos", linhas(dados.servicos.map { listOf(it.id, it.nome, it.ativo, it.precoCentavos) }))
            .put("atendimentos", linhas(dados.atendimentos.map { listOf(it.id, it.barbeiroId, it.efetivoUtcMs, it.fusoId, it.offsetEfetivoSegundos, it.diaLocal, it.semanaInicioLocal, it.taxaAplicadaBp, it.cancelado) }))
            .put("itens", linhas(dados.itens.map { listOf(it.id, it.atendimentoId, it.servicoId, it.precoAplicadoCentavos, it.descontoCentavos) }))
            .put("pagamentos", linhas(dados.pagamentos.map { listOf(it.id, it.atendimentoId, it.forma, it.valorCentavos) }))
            .put("confirmacoes", linhas(dados.confirmacoes.map { listOf(it.barbeiroId, it.semanaInicioLocal, it.valorConfirmadoCentavos, it.confirmadoEmUtcMs) })).toString()
        return JSONObject().put("formato", "SXGESTAO").put("versaoFormato", 1).put("versaoBanco", 1)
            .put("payload", payload).put("sha256", hash(payload)).toString()
    }
    fun validar(texto: String): BackupValidado {
        val envelope = objeto(texto)
        envelope.chaves(setOf("formato", "versaoFormato", "versaoBanco", "payload", "sha256"))
        require(envelope.get("formato") == "SXGESTAO" && envelope.get("versaoFormato") == 1 && envelope.get("versaoBanco") == 1) { "Versão de backup incompatível com este aplicativo." }
        val payload = envelope.get("payload") as? String ?: error("Conteúdo ausente.")
        require(envelope.get("sha256") == hash(payload)) { "Arquivo corrompido: checksum divergente." }
        val p = objeto(payload)
        p.chaves(tabelas)
        val dados = Retrato(
            p.linhas("barbeiros", 5) { Barbeiro(it.texto(0), it.texto(1), it.booleano(2), it.booleano(3), it.inteiro(4)) },
            p.linhas("servicos", 4) { Servico(it.texto(0), it.texto(1), it.booleano(2), it.numero(3)) },
            p.linhas("atendimentos", 9) { Atendimento(it.texto(0), it.texto(1), it.numero(2), it.texto(3), it.inteiro(4), it.numero(5), it.numero(6), it.inteiro(7), it.booleano(8)) },
            p.linhas("itens", 5) { ItemAtendimento(it.texto(0), it.texto(1), it.texto(2), it.numero(3), it.numero(4)) },
            p.linhas("pagamentos", 4) { Pagamento(it.texto(0), it.texto(1), it.texto(2), it.numero(3)) },
            p.linhas("confirmacoes", 4) { PagamentoSemanal(it.texto(0), it.numero(1), it.numero(2), it.numero(3)) })
        validarDados(dados)
        return BackupValidado(dados)
    }
    private fun validarDados(d: Retrato) {
        fun ids(valores: List<String>) {
            valores.forEach(RegrasFinanceiras::validarId)
            require(valores.distinct().size == valores.size) { "Chave duplicada." }
        }
        ids(d.barbeiros.map { it.id }); ids(d.servicos.map { it.id }); ids(d.atendimentos.map { it.id })
        ids(d.itens.map { it.id }); ids(d.pagamentos.map { it.id })
        val barbeiros = d.barbeiros.associateBy { it.id }
        val servicos = d.servicos.associateBy { it.id }
        val atendimentos = d.atendimentos.associateBy { it.id }
        d.barbeiros.forEach { require(it.nome.isNotBlank() && it.taxaComissaoBp in 0..10000 && (!it.proprietario || it.taxaComissaoBp == 0)) { "Barbeiro inválido." } }
        d.servicos.forEach { require(it.nome.isNotBlank() && it.precoCentavos >= 0) { "Serviço inválido." } }
        d.itens.forEach { require(it.atendimentoId in atendimentos && it.servicoId in servicos) { "Item com vínculo inexistente." }; require(it.precoAplicadoCentavos >= 0 && it.descontoCentavos in 0..it.precoAplicadoCentavos) { "Desconto ou preço inválido." } }
        d.pagamentos.forEach { require(it.atendimentoId in atendimentos && it.valorCentavos > 0) { "Pagamento inválido." }; FormaPagamento.valueOf(it.forma) }
        val itens = d.itens.groupBy { it.atendimentoId }
        val pagamentos = d.pagamentos.groupBy { it.atendimentoId }
        val comissoes = mutableMapOf<ParSemana, MutableList<Long>>()
        d.atendimentos.forEach { a ->
            val b = requireNotNull(barbeiros[a.barbeiroId]) { "Barbeiro do atendimento inexistente." }
            require(a.taxaAplicadaBp in 0..10000 && (!b.proprietario || a.taxaAplicadaBp == 0)) { "Taxa aplicada inválida." }
            ZoneId.of(a.fusoId) // Valida o identificador, sem reinterpretar regras históricas.
            val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(a.efetivoUtcMs), ZoneOffset.ofTotalSeconds(a.offsetEfetivoSegundos))
            require(local.toLocalDate().toEpochDay() == a.diaLocal && CalendarioFinanceiro.semana(local.toLocalDate()) == a.semanaInicioLocal) { "Classificação temporal incoerente." }
            val total = RegrasFinanceiras.liquido(itens[a.id].orEmpty().map { it.precoAplicadoCentavos to it.descontoCentavos })
            RegrasFinanceiras.validarPagamentos(total, pagamentos[a.id].orEmpty().map { it.valorCentavos })
            if (!a.cancelado) comissoes.getOrPut(ParSemana(a.barbeiroId, a.semanaInicioLocal)) { mutableListOf() }.add(RegrasFinanceiras.comissao(total, a.taxaAplicadaBp))
        }
        require(d.confirmacoes.map { ParSemana(it.barbeiroId, it.semanaInicioLocal) }.distinct().size == d.confirmacoes.size) { "Confirmação semanal duplicada." }
        d.confirmacoes.forEach { c ->
            require(c.barbeiroId in barbeiros && LocalDate.ofEpochDay(c.semanaInicioLocal).dayOfWeek == DayOfWeek.MONDAY) { "Confirmação com vínculo ou semana inválida." }
            require(c.valorConfirmadoCentavos >= 0 && c.valorConfirmadoCentavos == RegrasFinanceiras.soma(comissoes[ParSemana(c.barbeiroId, c.semanaInicioLocal)].orEmpty())) { "Confirmação diverge da apuração." }
        }
    }
}
