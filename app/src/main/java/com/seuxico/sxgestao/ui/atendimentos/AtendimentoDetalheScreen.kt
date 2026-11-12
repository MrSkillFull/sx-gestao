package com.seuxico.sxgestao.ui.atendimentos

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
import com.seuxico.sxgestao.data.repository.FormaPagamento
import com.seuxico.sxgestao.model.*
import com.seuxico.sxgestao.ui.components.*

@Composable
fun AtendimentoDetalheScreen(
    id: String,
    voltar: () -> Unit,
    editar: () -> Unit,
    vm: HistoricoViewModel = gestaoViewModel { HistoricoViewModel(it) },
    comissao: (String, Long) -> Unit = { _, _ -> }
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmar by remember { mutableStateOf(false) }
    val registro = state.registros.find { it.atendimento.id == id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SXGestaoTopBar(
            titulo = "Detalhe do Atendimento",
            voltar = voltar
        )

        Erro(state.erro)
        BloqueiosComissao(vm.bloqueios(id), state.barbeiros, comissao)
        state.mensagem?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (state.carregando) CircularProgressIndicator()
        if (!state.carregando && registro == null) Text("Atendimento não encontrado.")

        if (registro != null) {
            val a = registro.atendimento
            val barbeiro = state.barbeiros.find { it.id == a.barbeiroId }
            val total = RegrasFinanceiras.liquido(registro.itens.map { it.precoAplicadoCentavos to it.descontoCentavos })

            // Barber & Date Card
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_barber),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "${barbeiro?.nome ?: "Barbeiro"}${if (barbeiro?.ativo == false) " (Inativo)" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusBadge(
                            texto = if (a.cancelado) "Cancelado" else "Registrado",
                            tipo = if (a.cancelado) StatusTipo.MUTED else StatusTipo.SUCESSO
                        )
                    }

                    Text(
                        "${registro.horarioLocal().format(formatoData)} às ${registro.horarioLocal().format(formatoHora)} (${a.fusoId})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Services Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Serviços Lançados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    registro.itens.forEachIndexed { index, item ->
                        val servico = state.servicos.find { it.id == item.servicoId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${index + 1}. ${servico?.nome ?: "Serviço"}${if (servico?.ativo == false) " (Inativo)" else ""}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Preço: ${reais(item.precoAplicadoCentavos)} · Desconto: ${reais(item.descontoCentavos)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                reais(item.precoAplicadoCentavos - item.descontoCentavos),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Financial Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Resumo Financeiro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Líquido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            reais(total),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Taxa aplicada (${decimal(a.taxaAplicadaBp.toLong())}%)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Comissão: ${reais(RegrasFinanceiras.comissao(total, a.taxaAplicadaBp))}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Payments Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Pagamentos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    if (registro.pagamentos.isEmpty()) {
                        StatusBadge(texto = "Atendimento Gratuito", tipo = StatusTipo.MUTED)
                    }

                    registro.pagamentos.forEach {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(FormaPagamento.valueOf(it.forma).rotulo(), style = MaterialTheme.typography.bodyMedium)
                            Text(reais(it.valorCentavos), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!a.cancelado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = editar,
                        enabled = !state.gravando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Editar Atendimento")
                    }
                    OutlinedButton(
                        onClick = { confirmar = true },
                        enabled = !state.gravando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (state.gravando) "Cancelando…" else "Cancelar Atendimento",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (confirmar && registro != null) {
        val a = registro.atendimento
        val barbeiro = state.barbeiros.find { it.id == a.barbeiroId }
        val total = RegrasFinanceiras.liquido(registro.itens.map { it.precoAplicadoCentavos to it.descontoCentavos })

        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Cancelar atendimento?") },
            text = {
                Text("${barbeiro?.nome} · ${registro.horarioLocal().format(formatoData)} · ${reais(total)}. O registro permanecerá no histórico como cancelado e sairá dos totais.")
            },
            confirmButton = {
                TextButton(onClick = { confirmar = false; vm.cancelar(id) }) {
                    Text("Confirmar Cancelamento", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text("Voltar") }
            }
        )
    }
}
