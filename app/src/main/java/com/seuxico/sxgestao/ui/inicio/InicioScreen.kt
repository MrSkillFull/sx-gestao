package com.seuxico.sxgestao.ui.inicio

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.R
import com.seuxico.sxgestao.data.repository.*
import com.seuxico.sxgestao.model.*
import com.seuxico.sxgestao.ui.atendimentos.formatoData
import com.seuxico.sxgestao.ui.atendimentos.rotulo
import com.seuxico.sxgestao.ui.components.*
import java.time.LocalDate

@Composable
fun InicioScreen(
    novo: () -> Unit = {},
    comissao: (String, Long) -> Unit = { _, _ -> },
    vm: PainelViewModel = gestaoViewModel { PainelViewModel(it) }
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) vm.atualizarHoje()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InicioCabecalho()

        // Action: Novo Atendimento
        Button(
            onClick = novo,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Novo Atendimento",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 16.sp
            )
        }

        if (state.carregando) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Erro(state.erro)
        if (state.erro != null) {
            TextButton(onClick = vm::carregar) { Text("Tentar novamente") }
        }

        // Section 1: Hoje
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Hoje · ${state.hoje.format(formatoData)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    StatusBadge(
                        texto = "${state.dia.quantidade} atendimentos",
                        tipo = if (state.dia.quantidade > 0) StatusTipo.SUCESSO else StatusTipo.MUTED
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Column {
                    Text(
                        "Faturamento líquido",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        reais(state.dia.liquido),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Recebimentos(state.dia)

                if (state.dia.quantidade == 0) {
                    Text(
                        "Dia sem movimento registrado até o momento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 2: Comissões Semanais
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Comissões semanais",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${state.semana.format(formatoData)} a ${state.semana.plusDays(6).format(formatoData)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Week picker controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { vm.semana(state.semana.minusWeeks(1)) },
                    enabled = !state.gravando,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("« Anterior", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = { vm.semana(LocalDate.now()) },
                    enabled = !state.gravando,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("Atual", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { vm.semana(state.semana.plusWeeks(1)) },
                    enabled = !state.gravando,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("Próxima »", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, ano, mes, dia -> vm.semana(LocalDate.of(ano, mes + 1, dia)) },
                            state.semana.year,
                            state.semana.monthValue - 1,
                            state.semana.dayOfMonth
                        ).show()
                    },
                    enabled = !state.gravando
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = "Escolher semana",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (state.barbeiros.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        "Cadastre barbeiros na aba Cadastros para consultar comissões.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.barbeiros.forEach { barbeiro ->
                val confirmacao = state.confirmacoes.find {
                    it.barbeiroId == barbeiro.id && it.semanaInicioLocal == state.semana.toEpochDay()
                }
                OutlinedCard(
                    onClick = { comissao(barbeiro.id, state.semana.toEpochDay()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_barber),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    barbeiro.nome,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    reais(state.resumo.comissoes[barbeiro.id] ?: 0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(
                                texto = if (confirmacao == null) "Pendente" else "Confirmada",
                                tipo = if (confirmacao == null) StatusTipo.ALERTA else StatusTipo.SUCESSO
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Resumo da Semana
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Resumo da semana",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${state.semana.format(formatoData)} a ${state.semana.plusDays(6).format(formatoData)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Faturamento líquido",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            reais(state.resumo.liquido),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Parcela da barbearia",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            reais(state.resumo.parcelaBarbearia),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    "Líquido da semana menos comissões apuradas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Recebimentos(state.resumo)

                if (state.resumo.quantidade == 0) {
                    Text(
                        "Semana sem movimento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InicioCabecalho() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // O nome da barbearia já é anunciado no texto de boas-vindas.
        Image(
            painter = painterResource(R.drawable.logo_seu_xico),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .width(128.dp)
                .padding(8.dp)
                .aspectRatio(982f / 811f)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.inicio_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Claro · 320dp", widthDp = 320)
@Preview(name = "Escuro · 320dp", widthDp = 320, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Fonte ampliada · 320dp", widthDp = 320, fontScale = 2f)
@Composable
private fun InicioCabecalhoPreview() {
    val escuro = androidx.compose.foundation.isSystemInDarkTheme()
    SXGestaoTheme(tema = if (escuro) TemaAplicativo.PRETO else TemaAplicativo.LARANJA) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                InicioCabecalho()
            }
        }
    }
}

@Composable
fun Recebimentos(resumo: ResumoPeriodo) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Recebimentos por forma",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FormaPagamento.entries.forEach { forma ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            forma.rotulo(),
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            reais(resumo.recebimentos[forma] ?: 0),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
