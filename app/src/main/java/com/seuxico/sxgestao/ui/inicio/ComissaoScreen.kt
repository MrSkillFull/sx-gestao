package com.seuxico.sxgestao.ui.inicio

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.R
import com.seuxico.sxgestao.model.*
import com.seuxico.sxgestao.ui.atendimentos.*
import com.seuxico.sxgestao.ui.components.*
import java.time.*

@Composable
fun ComissaoScreen(
    barbeiroId: String,
    semana: Long,
    voltar: () -> Unit,
    vm: PainelViewModel = gestaoViewModel { PainelViewModel(it) }
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val segunda = LocalDate.ofEpochDay(semana)

    LaunchedEffect(semana) { vm.semana(segunda) }

    val barbeiro = state.barbeiros.find { it.id == barbeiroId }
    val confirmacao = state.confirmacoes.find { it.barbeiroId == barbeiroId && it.semanaInicioLocal == semana }
    var desfazer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SXGestaoTopBar(
            titulo = "Comissão Semanal",
            voltar = voltar
        )

        if (state.carregando || state.semana != segunda) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Erro(state.erro)
        state.mensagem?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }

        // Barber Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_barber),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        barbeiro?.nome ?: "Carregando barbeiro…",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Período: ${segunda.format(formatoData)} a ${segunda.plusDays(6).format(formatoData)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary & Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Situação da Apuração",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusBadge(
                        texto = if (confirmacao == null) "Pendente" else "Confirmada",
                        tipo = if (confirmacao == null) StatusTipo.ALERTA else StatusTipo.SUCESSO
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                if (state.semana == segunda) {
                    Column {
                        Text(
                            "Valor Apurado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            reais(state.resumo.comissoes[barbeiroId] ?: 0),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (confirmacao != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Valor confirmado: ${reais(confirmacao.valorConfirmadoCentavos)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(confirmacao.confirmadoEmUtcMs), ZoneId.systemDefault())
                        Text(
                            "Confirmada em ${local.format(formatoData)} às ${local.format(formatoHora)} (${ZoneId.systemDefault().id})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { desfazer = true },
                        enabled = !state.gravando,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Desfazer Confirmação", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Button(
                        onClick = { vm.confirmar(barbeiroId, segunda) },
                        enabled = !state.gravando && !state.carregando && barbeiro != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(if (state.gravando) "Confirmando…" else "Confirmar Pagamento")
                    }
                }
            }
        }
    }

    if (desfazer && confirmacao != null) {
        AlertDialog(
            onDismissRequest = { desfazer = false },
            title = { Text("Desfazer confirmação?") },
            text = {
                Text(
                    "${barbeiro?.nome} · ${segunda.format(formatoData)} a ${segunda.plusDays(6).format(formatoData)} · ${reais(confirmacao.valorConfirmadoCentavos)}. A comissão voltará a ficar pendente. Atendimentos e recebimentos serão preservados."
                )
            },
            confirmButton = {
                TextButton(onClick = { desfazer = false; vm.desfazer(barbeiroId, segunda) }) {
                    Text("Desfazer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { desfazer = false }) { Text("Voltar") }
            }
        )
    }
}
