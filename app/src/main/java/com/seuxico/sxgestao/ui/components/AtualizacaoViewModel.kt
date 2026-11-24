package com.seuxico.sxgestao.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuxico.sxgestao.data.repository.AtualizacaoRepository
import com.seuxico.sxgestao.data.repository.InfoAtualizacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AtualizacaoViewModel(
    private val repository: AtualizacaoRepository = AtualizacaoRepository()
) : ViewModel() {
    sealed interface EstadoAtualizacao {
        data object Verificando : EstadoAtualizacao
        data object Nenhuma : EstadoAtualizacao
        data class Disponivel(val info: InfoAtualizacao) : EstadoAtualizacao
        data class Baixando(val progresso: Float) : EstadoAtualizacao
        data class Erro(val mensagem: String, val info: InfoAtualizacao) : EstadoAtualizacao
        data class Instalando(val arquivo: File) : EstadoAtualizacao
    }

    private val _estado = MutableStateFlow<EstadoAtualizacao>(EstadoAtualizacao.Nenhuma)
    val estado: StateFlow<EstadoAtualizacao> = _estado.asStateFlow()
    val info: InfoAtualizacao? get() = infoAtualizacao
    var verificacaoConcluidaSemAtualizacao: Boolean = false
        private set
    private var infoAtualizacao: InfoAtualizacao? = null

    fun verificar(versaoAtual: String) {
        if (_estado.value == EstadoAtualizacao.Verificando) return
        _estado.value = EstadoAtualizacao.Verificando
        viewModelScope.launch {
            val info = repository.verificarAtualizacao(versaoAtual)
            infoAtualizacao = info
            verificacaoConcluidaSemAtualizacao = info == null
            _estado.value = info?.let(EstadoAtualizacao::Disponivel) ?: EstadoAtualizacao.Nenhuma
        }
    }

    fun confirmarDownload(cacheDir: File) = baixar(cacheDir)
    fun tentarNovamente(cacheDir: File) = baixar(cacheDir)
    fun dispensar() { _estado.value = EstadoAtualizacao.Nenhuma }

    private fun baixar(cacheDir: File) {
        val info = infoAtualizacao ?: return
        viewModelScope.launch {
            try {
                val destino = File(cacheDir, "SXGestao-update.apk")
                val arquivo = repository.baixarApk(info.urlDownload, destino) { baixado, total ->
                    val progresso = if (total > 0) (baixado.toFloat() / total).coerceIn(0f, 1f) else -1f
                    _estado.value = EstadoAtualizacao.Baixando(progresso)
                }
                _estado.value = EstadoAtualizacao.Instalando(arquivo)
            } catch (_: Exception) {
                _estado.value = EstadoAtualizacao.Erro("Não foi possível baixar a atualização.", info)
            }
        }
    }
}
