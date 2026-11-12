package com.seuxico.sxgestao.data.repository

import androidx.room.withTransaction
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.model.*
import java.time.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FormaPagamento { PIX, DINHEIRO, DEBITO, CREDITO }
data class ItemEntrada(val servicoId: String, val descontoCentavos: Long = 0, val id: String = novoId())
data class PagamentoEntrada(val forma: FormaPagamento, val valorCentavos: Long, val id: String = novoId())
data class ResumoFinanceiro(val liquidoCentavos: Long, val recebimentos: Map<FormaPagamento, Long>)
data class ParSemana(val barbeiroId: String, val semana: Long)
class SemanaConfirmadaException(val bloqueios: List<ParSemana>) : IllegalStateException("Desfaça todas as confirmações de comissão indicadas antes de alterar o atendimento.")

object EstadoBase {
    internal val restaurandoMutable = MutableStateFlow(false)
    val restaurando = restaurandoMutable.asStateFlow()
    internal val geracaoMutable = MutableStateFlow(0L)
    val geracao = geracaoMutable.asStateFlow()
}

/** Única fronteira de escrita do app. Decisões e alterações compartilham a transação Room. */
class GestaoRepository(private val db: GestaoDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val dao = db.dao()
    fun barbeiros() = dao.barbeiros()
    fun servicos() = dao.servicos()
    fun confirmacoes() = dao.confirmacoes()
    private fun ordenar(a: AtendimentoCompleto) = a.copy(itens = a.itens.sortedBy { it.id }, pagamentos = a.pagamentos.sortedBy { it.id })
    fun historico(inicio: LocalDate, fimExclusivo: LocalDate) = dao.historico(inicio.toEpochDay(), fimExclusivo.toEpochDay()).map { lista -> lista.map(::ordenar) }
    fun faturamento(inicio: LocalDate, fimExclusivo: LocalDate) = historico(inicio, fimExclusivo).map { registros ->
        val validos = registros.filterNot { it.atendimento.cancelado }
        ResumoFinanceiro(RegrasFinanceiras.soma(validos.map(::liquido)), validos.flatMap { it.pagamentos }
            .groupBy { FormaPagamento.valueOf(it.forma) }.mapValues { (_, partes) -> RegrasFinanceiras.soma(partes.map { it.valorCentavos }) })
    }
    suspend fun atendimento(id: String) = db.withTransaction { dao.atendimento(id)?.let(::ordenar) }
    suspend fun exportarBackup(): String = db.withTransaction {
        BackupCodec.exportar(Retrato(dao.exportarBarbeiros(), dao.exportarServicos(), dao.exportarAtendimentos(), dao.exportarItens(), dao.exportarPagamentos(), dao.exportarConfirmacoes()))
    }
    fun validarBackup(texto: String): BackupValidado = BackupCodec.validar(texto)
    suspend fun restaurarBackup(candidato: BackupValidado, confirmado: Boolean) {
        require(confirmado) { "Confirme a substituição dos dados." }
        check(EstadoBase.restaurandoMutable.compareAndSet(false, true)) { "Já existe uma restauração em andamento." }
        try {
            db.withTransaction {
                dao.limparConfirmacoes(); dao.limparPagamentos(); dao.limparItens(); dao.limparAtendimentos(); dao.limparBarbeiros(); dao.limparServicos()
                val d = candidato.dados
                d.barbeiros.forEach { dao.inserir(it) }; d.servicos.forEach { dao.inserir(it) }; d.atendimentos.forEach { dao.inserir(it) }
                dao.inserirItens(d.itens); dao.inserirPagamentos(d.pagamentos); d.confirmacoes.forEach { dao.inserir(it) }
            }
            EstadoBase.geracaoMutable.update { Math.addExact(it, 1) }
        } finally { EstadoBase.restaurandoMutable.value = false }
    }
    private suspend fun <T> escrita(acao: suspend () -> T): T {
        check(!EstadoBase.restaurando.value) { "Aguarde a restauração terminar." }
        return db.withTransaction {
            check(!EstadoBase.restaurando.value) { "Aguarde a restauração terminar." }
            acao()
        }
    }
    suspend fun salvarBarbeiro(valor: Barbeiro) = escrita {
        RegrasFinanceiras.validarId(valor.id)
        require(valor.nome.isNotBlank() && valor.taxaComissaoBp in 0..10000)
        val anterior = dao.barbeiro(valor.id)
        require(anterior == null || anterior.proprietario == valor.proprietario) { "Não altere a condição de proprietário." }
        require(!valor.proprietario || valor.taxaComissaoBp == 0) { "A taxa do proprietário é zero." }
        if (anterior == null) dao.inserir(valor) else dao.atualizar(valor)
    }
    suspend fun salvarServico(valor: Servico) = escrita {
        RegrasFinanceiras.validarId(valor.id)
        require(valor.nome.isNotBlank() && valor.precoCentavos >= 0)
        if (dao.servico(valor.id) == null) dao.inserir(valor) else dao.atualizar(valor)
    }
    suspend fun registrar(
        barbeiroId: String, local: LocalDateTime, itens: List<ItemEntrada>, pagamentos: List<PagamentoEntrada>,
        fuso: ZoneId = ZoneId.systemDefault(), offset: ZoneOffset? = null,
    ): String = escrita {
        val id = novoId()
        gravar(id, null, barbeiroId, CalendarioFinanceiro.resolver(local, fuso, offset), itens, pagamentos)
        id
    }
    /** local nulo preserva todos os campos temporais, inclusive regras históricas do fuso. */
    suspend fun editar(id: String, barbeiroId: String, itens: List<ItemEntrada>, pagamentos: List<PagamentoEntrada>, local: LocalDateTime? = null, offset: ZoneOffset? = null) = escrita {
        val antigo = requireNotNull(dao.atendimento(id)) { "Atendimento não encontrado." }
        require(!antigo.atendimento.cancelado) { "Atendimento cancelado não pode ser editado." }
        val a = antigo.atendimento
        val tempo = if (local == null) TempoEfetivo(a.efetivoUtcMs, a.fusoId, a.offsetEfetivoSegundos, a.diaLocal, a.semanaInicioLocal)
            else CalendarioFinanceiro.resolver(local, ZoneId.of(a.fusoId), offset)
        gravar(id, antigo, barbeiroId, tempo, itens, pagamentos)
    }
    private suspend fun gravar(id: String, antigo: AtendimentoCompleto?, barbeiroId: String, tempo: TempoEfetivo, itens: List<ItemEntrada>, pagamentos: List<PagamentoEntrada>) {
        RegrasFinanceiras.validarId(barbeiroId)
        val barbeiro = requireNotNull(dao.barbeiro(barbeiroId)) { "Barbeiro não encontrado." }
        val mantido = antigo?.atendimento?.barbeiroId == barbeiroId
        require(mantido || barbeiro.ativo) { "Selecione um barbeiro ativo." }
        desbloqueados(listOfNotNull(antigo?.atendimento?.let { ParSemana(it.barbeiroId, it.semanaInicioLocal) }, ParSemana(barbeiroId, tempo.semana)))
        require(itens.map { it.id }.distinct().size == itens.size)
        require(pagamentos.map { it.id }.distinct().size == pagamentos.size)
        val filhos = itens.map { entrada ->
            RegrasFinanceiras.validarId(entrada.id)
            RegrasFinanceiras.validarId(entrada.servicoId)
            val anterior = antigo?.itens?.find { it.id == entrada.id }
            val servico = requireNotNull(dao.servico(entrada.servicoId)) { "Serviço não encontrado." }
            val mesmo = anterior?.servicoId == entrada.servicoId
            require(mesmo || servico.ativo) { "Selecione um serviço ativo." }
            ItemAtendimento(entrada.id, id, servico.id, if (mesmo) anterior!!.precoAplicadoCentavos else servico.precoCentavos, entrada.descontoCentavos)
        }
        val total = RegrasFinanceiras.liquido(filhos.map { it.precoAplicadoCentavos to it.descontoCentavos })
        RegrasFinanceiras.validarPagamentos(total, pagamentos.map { it.valorCentavos })
        val partes = pagamentos.map {
            RegrasFinanceiras.validarId(it.id)
            Pagamento(it.id, id, it.forma.name, it.valorCentavos)
        }
        val cabecalho = Atendimento(id, barbeiroId, tempo.instanteMs, tempo.fusoId, tempo.offsetSegundos, tempo.dia, tempo.semana,
            if (mantido) antigo!!.atendimento.taxaAplicadaBp else barbeiro.taxaComissaoBp)
        if (antigo == null) dao.inserir(cabecalho) else {
            dao.atualizar(cabecalho)
            dao.removerItens(id)
            dao.removerPagamentos(id)
        }
        dao.inserirItens(filhos)
        dao.inserirPagamentos(partes)
    }
    suspend fun cancelar(id: String, confirmado: Boolean) = escrita {
        require(confirmado) { "Confirme o cancelamento." }
        val a = requireNotNull(dao.atendimento(id)).atendimento
        desbloqueado(a.barbeiroId, a.semanaInicioLocal)
        dao.atualizar(a.copy(cancelado = true))
    }
    suspend fun apurar(barbeiroId: String, segunda: LocalDate): Long = db.withTransaction {
        validarSemana(barbeiroId, segunda)
        apurarInterno(barbeiroId, segunda.toEpochDay())
    }
    suspend fun confirmar(barbeiroId: String, segunda: LocalDate): PagamentoSemanal = escrita {
        validarSemana(barbeiroId, segunda)
        val semana = segunda.toEpochDay()
        dao.confirmacao(barbeiroId, semana) ?: run {
            dao.inserir(PagamentoSemanal(barbeiroId, semana, apurarInterno(barbeiroId, semana), clock.millis()))
            requireNotNull(dao.confirmacao(barbeiroId, semana))
        }
    }
    suspend fun desfazerConfirmacao(barbeiroId: String, segunda: LocalDate, confirmado: Boolean) = escrita {
        require(confirmado) { "Confirme o desfazimento." }
        validarSemana(barbeiroId, segunda)
        dao.desfazer(barbeiroId, segunda.toEpochDay())
    }
    private suspend fun validarSemana(barbeiroId: String, segunda: LocalDate) {
        RegrasFinanceiras.validarId(barbeiroId)
        require(segunda.dayOfWeek == DayOfWeek.MONDAY) { "Informe a segunda-feira da semana." }
        requireNotNull(dao.barbeiro(barbeiroId))
    }
    private suspend fun desbloqueado(barbeiroId: String, semana: Long) {
        desbloqueados(listOf(ParSemana(barbeiroId, semana)))
    }
    private suspend fun desbloqueados(pares: List<ParSemana>) {
        val bloqueios = pares.distinct().filter { dao.confirmacao(it.barbeiroId, it.semana) != null }
        if (bloqueios.isNotEmpty()) throw SemanaConfirmadaException(bloqueios)
    }
    private fun liquido(a: AtendimentoCompleto) = RegrasFinanceiras.liquido(a.itens.map { it.precoAplicadoCentavos to it.descontoCentavos })
    private suspend fun apurarInterno(barbeiroId: String, semana: Long) = RegrasFinanceiras.soma(dao.semana(barbeiroId, semana).map {
        RegrasFinanceiras.comissao(liquido(it), it.atendimento.taxaAplicadaBp)
    })
}
