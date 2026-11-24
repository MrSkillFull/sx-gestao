package com.seuxico.sxgestao.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seuxico.sxgestao.data.repository.InfoAtualizacao

@Composable
fun DialogoAtualizacao(
    info: InfoAtualizacao,
    estado: AtualizacaoViewModel.EstadoAtualizacao,
    onConfirmar: () -> Unit,
    onDispensar: () -> Unit,
    onTentarNovamente: () -> Unit
) {
    val baixando = estado as? AtualizacaoViewModel.EstadoAtualizacao.Baixando
    val erro = estado as? AtualizacaoViewModel.EstadoAtualizacao.Erro
    AlertDialog(
        onDismissRequest = { if (baixando == null) onDispensar() },
        title = { Text(if (erro != null) "Falha no download" else "Atualização disponível") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                when {
                    baixando != null -> {
                        Text("Baixando atualização…")
                        Spacer(Modifier.height(12.dp))
                        if (baixando.progresso < 0f) CircularProgressIndicator()
                        else LinearProgressIndicator(progress = { baixando.progresso }, modifier = Modifier.fillMaxWidth())
                    }
                    erro != null -> Text(erro.mensagem)
                    else -> {
                        Text("Versão ${info.versao}")
                        if (info.notas.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(info.notas)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                baixando != null -> Unit
                erro != null -> Button(onClick = onTentarNovamente) { Text("Tentar novamente") }
                else -> Button(onClick = onConfirmar) { Text("Atualizar") }
            }
        },
        dismissButton = {
            if (baixando == null) TextButton(onClick = onDispensar) { Text(if (erro != null) "Fechar" else "Depois") }
        }
    )
}
