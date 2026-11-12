package com.seuxico.sxgestao.ui.cadastros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.GestaoRepository
import com.seuxico.sxgestao.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CadastroForm(val barbeiro: Boolean, val id: String = novoId(), val nome: String = "",
    val valor: String = if (barbeiro) "50,00" else "", val proprietario: Boolean = false,
    val ativo: Boolean = true, val existente: Boolean = false,
    val erroNome: String? = null, val erroValor: String? = null)
data class CadastrosState(val barbeiros: List<Barbeiro> = emptyList(), val servicos: List<Servico> = emptyList(),
    val carregando: Boolean = true, val gravando: Boolean = false, val erro: String? = null,
    val mensagem: String? = null, val form: CadastroForm? = null)

class CadastrosViewModel(private val repo: GestaoRepository) : ViewModel() {
    private val mutable = MutableStateFlow(CadastrosState())
    val state = mutable.asStateFlow()
    init { carregar() }
    fun carregar() { viewModelScope.launch {
        try { combine(repo.barbeiros(), repo.servicos()) { b, s -> b to s }.collect { (b, s) ->
            mutable.update { it.copy(barbeiros = b, servicos = s, carregando = false, erro = null) }
        } } catch (e: CancellationException) { throw e } catch (e: Exception) {
            mutable.update { it.copy(carregando = false, erro = "Não foi possível ler os cadastros. Tente novamente.") }
        }
    } }
    fun abrir(form: CadastroForm?) { if (!state.value.gravando) mutable.update { it.copy(form = form, mensagem = null, erro = null) } }
    fun alterar(form: CadastroForm) { if (!state.value.gravando) mutable.update { it.copy(form = form) } }
    fun normalizarValor() {
        val form = state.value.form ?: return
        val valor = runCatching { centavos(form.valor).also { if (form.barbeiro) require(it <= 10000) } }.getOrNull()
        alterar(form.copy(valor = valor?.let(::decimal) ?: form.valor,
            erroValor = if (valor == null) "Valor inválido. Confira o formato e o limite." else null))
    }
    fun salvar(inativar: Boolean = false) {
        val form = state.value.form ?: return
        if (state.value.gravando || state.value.carregando) return
        val valor = runCatching { if (form.barbeiro) {
            if (form.proprietario) 0L else centavos(form.valor).also { require(it <= 10000) }
        } else centavos(form.valor) }.getOrNull()
        val validado = form.copy(erroNome = if (form.nome.isBlank()) "Informe o nome." else null,
            erroValor = if (valor == null) if (form.barbeiro) "Informe de 0 a 100%, com até duas casas." else "Informe um preço válido com até duas casas." else null)
        mutable.update { it.copy(form = validado, erro = null) }
        if (validado.erroNome != null || valor == null) return
        mutable.update { it.copy(gravando = true) }
        viewModelScope.launch {
            try {
                if (form.barbeiro) repo.salvarBarbeiro(Barbeiro(form.id, form.nome.trim(), form.ativo && !inativar, form.proprietario, valor.toInt()))
                else repo.salvarServico(Servico(form.id, form.nome.trim(), form.ativo && !inativar, valor))
                mutable.update { it.copy(form = null, gravando = false, mensagem = if (inativar) "Cadastro inativado." else "Cadastro salvo.") }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                mutable.update { it.copy(gravando = false, erro = "Não foi possível salvar. Confira os dados e tente novamente.") }
            }
        }
    }
}
