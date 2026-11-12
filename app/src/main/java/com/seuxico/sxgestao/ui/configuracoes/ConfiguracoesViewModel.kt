package com.seuxico.sxgestao.ui.configuracoes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuxico.sxgestao.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ConfiguracoesState(val ocupado: Boolean = false, val progresso: String? = null,
    val erro: String? = null, val mensagem: String? = null, val arquivo: String? = null,
    val quantidade: Int? = null)
class ConfiguracoesViewModel(private val repo: GestaoRepository, context: Context) : ViewModel() {
    private val resolver = context.applicationContext.contentResolver
    private val mutable = MutableStateFlow(ConfiguracoesState())
    val state = mutable.asStateFlow()
    private var candidato: BackupValidado? = null
    fun exportar(uri: Uri) = executar("Gerando backup…") {
        val conteudo = repo.exportarBackup()
        requireNotNull(resolver.openOutputStream(uri, "wt")) { "Não foi possível abrir o destino." }.bufferedWriter(Charsets.UTF_8).use { it.write(conteudo) }
        mutable.update { it.copy(mensagem = "Backup gerado com sucesso.") }
    }
    fun validar(uri: Uri) = executar("Validando arquivo…") {
        candidato = null
        mutable.update { it.copy(arquivo = null, quantidade = null) }
        val nome = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "backup selecionado"
        mutable.update { it.copy(progresso = "Validando arquivo $nome…") }
        val conteudo = requireNotNull(resolver.openInputStream(uri)) { "Não foi possível abrir o arquivo." }.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val validado = repo.validarBackup(conteudo)
        candidato = validado
        mutable.update { it.copy(arquivo = nome, quantidade = validado.quantidadeAtendimentos) }
    }
    fun recusar() {
        if (state.value.ocupado) return
        candidato = null
        mutable.update { it.copy(arquivo = null, quantidade = null) }
    }
    fun restaurar() {
        val copia = candidato ?: return
        executar("Restaurando backup…", restauracao = true) {
            repo.restaurarBackup(copia, confirmado = true)
            candidato = null
            mutable.update { it.copy(arquivo = null, quantidade = null, mensagem = "Backup restaurado com sucesso.") }
        }
    }
    private fun executar(progresso: String, restauracao: Boolean = false, acao: suspend () -> Unit) {
        if (state.value.ocupado) return
        mutable.update { it.copy(ocupado = true, progresso = progresso, erro = null, mensagem = null) }
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { acao() } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(erro = if (restauracao) "Não foi possível restaurar. Os dados anteriores foram preservados. ${e.message.orEmpty()}" else "Não foi possível concluir. ${e.message.orEmpty()}") } }
            finally { mutable.update { it.copy(ocupado = false, progresso = null) } }
        }
    }
}
