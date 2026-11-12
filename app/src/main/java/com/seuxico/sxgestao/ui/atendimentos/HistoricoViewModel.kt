package com.seuxico.sxgestao.ui.atendimentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.GestaoRepository
import com.seuxico.sxgestao.data.repository.ParSemana
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoricoState(val registros: List<AtendimentoCompleto> = emptyList(), val barbeiros: List<Barbeiro> = emptyList(),
    val servicos: List<Servico> = emptyList(), val carregando: Boolean = true, val gravando: Boolean = false,
    val erro: String? = null, val mensagem: String? = null, val confirmacoes: List<PagamentoSemanal> = emptyList())
class HistoricoViewModel(private val repo: GestaoRepository) : ViewModel() {
    private val mutable = MutableStateFlow(HistoricoState())
    val state = mutable.asStateFlow()
    init { carregar() }
    fun carregar() { viewModelScope.launch {
        try {
            combine(repo.historico(LocalDate.MIN, LocalDate.MAX), repo.barbeiros(), repo.servicos(), repo.confirmacoes()) { registros, b, s, c ->
                HistoricoState(registros.asReversed(), b, s, carregando = false, confirmacoes = c)
            }.collect { novo -> mutable.update { novo.copy(gravando = it.gravando, erro = it.erro, mensagem = it.mensagem) } }
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            mutable.update { it.copy(carregando = false, erro = "Não foi possível ler o histórico. Tente novamente.") }
        }
    } }
    fun bloqueios(id: String): List<ParSemana> {
        val a = state.value.registros.find { it.atendimento.id == id }?.atendimento ?: return emptyList()
        return state.value.confirmacoes.filter { it.barbeiroId == a.barbeiroId && it.semanaInicioLocal == a.semanaInicioLocal }.map { ParSemana(it.barbeiroId, it.semanaInicioLocal) }
    }
    fun cancelar(id: String) {
        if (state.value.gravando) return
        mutable.update { it.copy(gravando = true, erro = null) }
        viewModelScope.launch {
            try { repo.cancelar(id, true); mutable.update { it.copy(gravando = false, mensagem = "Atendimento cancelado.") } }
            catch (e: CancellationException) { throw e } catch (e: Exception) {
                mutable.update { it.copy(gravando = false, erro = e.message ?: "Não foi possível cancelar. Tente novamente.") }
            }
        }
    }
}
