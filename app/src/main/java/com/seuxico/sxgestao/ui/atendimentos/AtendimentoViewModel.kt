package com.seuxico.sxgestao.ui.atendimentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import java.time.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AtendimentoState(val form: AtendimentoForm, val barbeiros: List<Barbeiro> = emptyList(),
    val servicos: List<Servico> = emptyList(), val carregando: Boolean = true,
    val gravando: Boolean = false, val enviado: Boolean = false, val salvo: Boolean = false,
    val erro: String? = null, val aviso: String? = null, val original: AtendimentoCompleto? = null,
    val confirmacoes: List<PagamentoSemanal> = emptyList())
class AtendimentoViewModel(private val repo: GestaoRepository, private val id: String? = null) : ViewModel() {
    private val agora = LocalDateTime.now()
    private var inicial = AtendimentoForm(data = agora.format(formatoData), hora = agora.format(formatoHora), fuso = ZoneId.systemDefault().id)
    private val mutable = MutableStateFlow(AtendimentoState(inicial))
    val state = mutable.asStateFlow()
    val alterado get() = state.value.form != inicial
    init {
        if (id == null) carregar() else viewModelScope.launch {
            try {
                val original = requireNotNull(repo.atendimento(id)) { "Atendimento não encontrado." }
                require(!original.atendimento.cancelado) { "Atendimento cancelado é somente leitura." }
                inicial = original.formulario()
                mutable.update { it.copy(form = inicial, original = original) }
                carregar()
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                mutable.update { it.copy(erro = e.message ?: "Não foi possível ler o atendimento.") }
            }
        }
    }
    fun carregar() { viewModelScope.launch {
        try { combine(repo.barbeiros(), repo.servicos(), repo.confirmacoes()) { b, s, c -> Triple(b, s, c) }.collect { (b, s, c) ->
            mutable.update { it.copy(barbeiros = b, servicos = s, confirmacoes = c, carregando = false) }
        } } catch (e: CancellationException) { throw e } catch (e: Exception) {
            mutable.update { it.copy(carregando = false, erro = "Não foi possível ler os cadastros. Tente novamente.") }
        }
    } }
    fun avaliar() = state.value.let { avaliarAtendimento(it.form, it.barbeiros, it.servicos, it.original) }
    fun preco(item: ItemForm): Long? = state.value.original?.itens?.find { it.id == item.id && it.servicoId == item.servicoId }?.precoAplicadoCentavos
        ?: state.value.servicos.find { it.id == item.servicoId }?.precoCentavos
    fun bloqueios(): List<ParSemana> {
        val atual = state.value
        val destino = avaliar().local?.let { ParSemana(atual.form.barbeiroId, com.seuxico.sxgestao.model.CalendarioFinanceiro.semana(it.toLocalDate())) }
        val origem = atual.original?.atendimento?.let { ParSemana(it.barbeiroId, it.semanaInicioLocal) }
        return listOfNotNull(origem, destino).distinct().filter { par -> atual.confirmacoes.any { it.barbeiroId == par.barbeiroId && it.semanaInicioLocal == par.semana } }
    }
    fun alterar(form: AtendimentoForm) {
        if (state.value.gravando) return
        val avaliacao = avaliarAtendimento(form, state.value.barbeiros, state.value.servicos, state.value.original)
        val gratuito = avaliacao.liquido == 0L && form.partes.isNotEmpty()
        mutable.update { it.copy(form = if (gratuito) form.copy(partes = emptyList()) else form,
            erro = null, aviso = if (gratuito) "Atendimento gratuito. Os pagamentos foram removidos do formulário." else it.aviso) }
    }
    fun salvar() {
        if (state.value.gravando || state.value.carregando) return
        val avaliacao = avaliar()
        mutable.update { it.copy(enviado = true, erro = null) }
        if (avaliacao.erros.isNotEmpty()) return
        val form = state.value.form
        mutable.update { it.copy(gravando = true) }
        viewModelScope.launch {
            try {
                val itens = form.itens.map { ItemEntrada(it.servicoId, com.seuxico.sxgestao.model.centavos(it.desconto), it.id) }
                val original = state.value.original
                if (original == null) repo.registrar(form.barbeiroId, requireNotNull(avaliacao.local), itens,
                    avaliacao.pagamentos, ZoneId.of(form.fuso), avaliacao.offset)
                else {
                    val mesmoTempo = form.data == inicial.data && form.hora == inicial.hora && (form.offset == null || form.offset == inicial.offset)
                    repo.editar(original.atendimento.id, form.barbeiroId, itens, avaliacao.pagamentos,
                        local = if (mesmoTempo) null else avaliacao.local, offset = avaliacao.offset)
                }
                mutable.update { it.copy(gravando = false, salvo = true) }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                mutable.update { it.copy(gravando = false, erro = e.message ?: "Não foi possível salvar. Tente novamente.") }
            }
        }
    }
}
