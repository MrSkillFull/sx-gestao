package com.seuxico.sxgestao.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import com.seuxico.sxgestao.model.*
import java.time.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ResumoPeriodo(val liquido: Long = 0, val quantidade: Int = 0, val recebimentos: Map<FormaPagamento, Long> = emptyMap(), val comissoes: Map<String, Long> = emptyMap()) {
    val parcelaBarbearia get() = Math.subtractExact(liquido, RegrasFinanceiras.soma(comissoes.values))
}
fun resumir(registros: List<AtendimentoCompleto>): ResumoPeriodo {
    val validos = registros.filterNot { it.atendimento.cancelado }
    return ResumoPeriodo(RegrasFinanceiras.soma(validos.map { a -> RegrasFinanceiras.liquido(a.itens.map { it.precoAplicadoCentavos to it.descontoCentavos }) }), validos.size,
        validos.flatMap { it.pagamentos }.groupBy { FormaPagamento.valueOf(it.forma) }.mapValues { (_, partes) -> RegrasFinanceiras.soma(partes.map { it.valorCentavos }) },
        validos.groupBy { it.atendimento.barbeiroId }.mapValues { (_, atendimentos) -> RegrasFinanceiras.soma(atendimentos.map { a -> RegrasFinanceiras.comissao(RegrasFinanceiras.liquido(a.itens.map { it.precoAplicadoCentavos to it.descontoCentavos }), a.atendimento.taxaAplicadaBp) }) })
}
data class PainelState(val hoje: LocalDate = LocalDate.now(), val semana: LocalDate = LocalDate.ofEpochDay(CalendarioFinanceiro.semana(LocalDate.now())),
    val dia: ResumoPeriodo = ResumoPeriodo(), val resumo: ResumoPeriodo = ResumoPeriodo(), val barbeiros: List<Barbeiro> = emptyList(),
    val confirmacoes: List<PagamentoSemanal> = emptyList(), val carregando: Boolean = true, val gravando: Boolean = false,
    val erro: String? = null, val mensagem: String? = null)
class PainelViewModel(private val repo: GestaoRepository) : ViewModel() {
    private val periodo = MutableStateFlow(LocalDate.now() to LocalDate.ofEpochDay(CalendarioFinanceiro.semana(LocalDate.now())))
    private val mutable = MutableStateFlow(PainelState())
    val state = mutable.asStateFlow()
    init {
        carregar()
        viewModelScope.launch {
            while (true) {
                val agora = ZonedDateTime.now()
                val proximoDia = agora.toLocalDate().plusDays(1).atStartOfDay(agora.zone)
                delay(Duration.between(agora, proximoDia).toMillis().coerceAtLeast(1))
                atualizarHoje()
            }
        }
    }
    fun carregar() { viewModelScope.launch {
        try {
            combine(repo.historico(LocalDate.MIN, LocalDate.MAX), repo.barbeiros(), repo.confirmacoes(), periodo) { registros, barbeiros, confirmacoes, periodo ->
                val (hoje, semana) = periodo
                PainelState(hoje, semana, resumir(registros.filter { it.atendimento.diaLocal == hoje.toEpochDay() }),
                    resumir(registros.filter { it.atendimento.semanaInicioLocal == semana.toEpochDay() }), barbeiros, confirmacoes, false)
            }.collect { resultado -> mutable.update { resultado.copy(gravando = it.gravando, erro = it.erro, mensagem = it.mensagem) } }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { mutable.update { it.copy(carregando = false, erro = "Não foi possível apurar o período. Tente novamente.") } }
    } }
    fun atualizarHoje() { periodo.update { LocalDate.now() to it.second } }
    fun semana(dia: LocalDate) { if (!state.value.gravando) periodo.update { it.first to LocalDate.ofEpochDay(CalendarioFinanceiro.semana(dia)) } }
    fun confirmar(barbeiroId: String, segunda: LocalDate) = operar { repo.confirmar(barbeiroId, segunda); "Pagamento confirmado." }
    fun desfazer(barbeiroId: String, segunda: LocalDate) = operar { repo.desfazerConfirmacao(barbeiroId, segunda, true); "Confirmação desfeita. Pagamento pendente." }
    private fun operar(acao: suspend () -> String) {
        if (state.value.gravando) return
        mutable.update { it.copy(gravando = true, erro = null, mensagem = null) }
        viewModelScope.launch {
            try { val mensagem = acao(); mutable.update { it.copy(gravando = false, mensagem = mensagem) } }
            catch (e: CancellationException) { throw e } catch (e: Exception) { mutable.update { it.copy(gravando = false, erro = e.message ?: "Não foi possível gravar. Tente novamente.") } }
        }
    }
}
