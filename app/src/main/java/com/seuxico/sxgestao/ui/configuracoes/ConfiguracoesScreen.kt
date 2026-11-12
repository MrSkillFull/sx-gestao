package com.seuxico.sxgestao.ui.configuracoes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.R
import com.seuxico.sxgestao.ui.components.*

@Composable
fun ConfiguracoesScreen(fornecido: ConfiguracoesViewModel? = null) {
    val context = LocalContext.current.applicationContext
    val vm: ConfiguracoesViewModel = fornecido ?: gestaoViewModel { ConfiguracoesViewModel(it, context) }
    val state by vm.state.collectAsStateWithLifecycle()
    val criar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportar) }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::validar) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SXGestaoTopBar(
            titulo = "Configurações",
            subtitulo = stringResource(R.string.configuracoes_description)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Backup e Restauração",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Text(
                    "Escolha um arquivo pelo seletor do Android. Guarde uma cópia de segurança para proteger e recuperar os dados do estabelecimento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { criar.launch("sx-gestao-backup.json") },
                        enabled = !state.ocupado,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Gerar Backup", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = { abrir.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        enabled = !state.ocupado,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Restaurar Backup", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (state.ocupado) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Processando arquivo...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                state.progresso?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                Erro(state.erro)

                state.mensagem?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        }
    }

    if (state.arquivo != null && !state.ocupado) {
        AlertDialog(
            onDismissRequest = vm::recusar,
            title = { Text("Restaurar backup?") },
            text = {
                Text(
                    "O arquivo ${state.arquivo}, com ${state.quantidade} atendimentos, substituirá todos os dados atuais do SX Gestão."
                )
            },
            confirmButton = {
                TextButton(onClick = vm::restaurar) {
                    Text("Substituir dados", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::recusar) { Text("Voltar") }
            }
        )
    }
}
