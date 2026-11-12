package com.seuxico.sxgestao.ui.atendimentos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.R
import com.seuxico.sxgestao.data.repository.FormaPagamento
import com.seuxico.sxgestao.model.*
import com.seuxico.sxgestao.ui.components.*
import java.time.*

fun FormaPagamento.rotulo() = when (this) {
    FormaPagamento.PIX -> "Pix"
    FormaPagamento.DINHEIRO -> "Dinheiro"
    FormaPagamento.DEBITO -> "Cartão de débito"
    FormaPagamento.CREDITO -> "Cartão de crédito"
}

@Composable
fun AtendimentoFormScreen(
    voltar: () -> Unit,
    vm: AtendimentoViewModel = gestaoViewModel { AtendimentoViewModel(it) },
    comissao: (String, Long) -> Unit = { _, _ -> }
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val form = state.form
    val avaliacao = vm.avaliar()
    var visitados by remember { mutableStateOf(setOf<String>()) }
    var dataEmFoco by remember { mutableStateOf(false) }
    var horaEmFoco by remember { mutableStateOf(false) }
    val erros = avaliacao.erros.filterKeys { state.enviado || it in visitados }
    var descartar by remember { mutableStateOf(false) }
    val sair = { if (!state.gravando) { if (vm.alterado) descartar = true else voltar() } }

    BackHandler { sair() }

    val context = LocalContext.current
    LaunchedEffect(state.salvo) {
        if (state.salvo) {
            android.widget.Toast.makeText(context, "Atendimento salvo.", android.widget.Toast.LENGTH_SHORT).show()
            voltar()
        }
    }

    val focus = LocalFocusManager.current
    val focos = remember { mutableMapOf<String, FocusRequester>() }
    fun foco(chave: String) = focos.getOrPut(chave) { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SXGestaoTopBar(
            titulo = if (state.original == null) "Novo Atendimento" else "Editar Atendimento",
            voltar = { sair() }
        )

        if (state.carregando) CircularProgressIndicator()
        Erro(state.erro)
        BloqueiosComissao(vm.bloqueios(), state.barbeiros, comissao)

        if (state.erro != null && !state.carregando) {
            TextButton(onClick = vm::carregar) { Text("Atualizar dados") }
        }

        state.aviso?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }

        // Card 1: Dados do Atendimento
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_barber),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Barbeiro e Horário",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                Selecao(
                    rotulo = "Barbeiro",
                    valor = state.barbeiros.find { it.id == form.barbeiroId }?.let { "${it.nome}${if (it.ativo) "" else " (Inativo)"}" } ?: "",
                    opcoes = state.barbeiros.filter { it.ativo || it.id == state.original?.atendimento?.barbeiroId },
                    nome = { "${it.nome}${if (it.ativo) "" else " (Inativo)"}" },
                    selecionar = { vm.alterar(form.copy(barbeiroId = it.id)) },
                    modifier = Modifier.focusRequester(foco("barbeiro")).focusable(),
                    enabled = !state.gravando
                )

                if (state.barbeiros.none { it.ativo }) {
                    Text("Cadastre um barbeiro ativo na aba Cadastros.", color = MaterialTheme.colorScheme.error)
                }
                Erro(erros["barbeiro"])

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = form.data,
                            onValueChange = { vm.alterar(form.copy(data = it, offset = null)) },
                            label = { Text("Data efetiva") },
                            supportingText = { Text(erros["data"] ?: "dd/mm/aaaa") },
                            isError = erros.containsKey("data"),
                            enabled = !state.gravando,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { foco("hora").requestFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(foco("data"))
                                .onFocusChanged {
                                    if (dataEmFoco && !it.isFocused) visitados = visitados + "data"
                                    dataEmFoco = it.isFocused
                                }
                        )
                        TextButton(
                            onClick = {
                                val dia = runCatching { LocalDate.parse(form.data, formatoData) }.getOrElse { LocalDate.now() }
                                DatePickerDialog(
                                    context,
                                    { _, ano, mes, data ->
                                        vm.alterar(form.copy(data = LocalDate.of(ano, mes + 1, data).format(formatoData), offset = null))
                                    },
                                    dia.year,
                                    dia.monthValue - 1,
                                    dia.dayOfMonth
                                ).show()
                            },
                            enabled = !state.gravando
                        ) {
                            Text("Calendário")
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = form.hora,
                            onValueChange = { vm.alterar(form.copy(hora = it, offset = null)) },
                            label = { Text("Hora efetiva") },
                            supportingText = { Text(erros["hora"] ?: "hh:mm") },
                            isError = erros.containsKey("hora"),
                            enabled = !state.gravando,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(foco("hora"))
                                .onFocusChanged {
                                    if (horaEmFoco && !it.isFocused) visitados = visitados + "hora"
                                    horaEmFoco = it.isFocused
                                }
                        )
                        TextButton(
                            onClick = {
                                val hora = runCatching { LocalTime.parse(form.hora, formatoHora) }.getOrElse { LocalTime.now() }
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        vm.alterar(form.copy(hora = LocalTime.of(h, m).format(formatoHora), offset = null))
                                    },
                                    hora.hour,
                                    hora.minute,
                                    true
                                ).show()
                            },
                            enabled = !state.gravando
                        ) {
                            Text("Relógio")
                        }
                    }
                }

                val offsets = runCatching {
                    ZoneId.of(form.fuso).rules.getValidOffsets(
                        LocalDateTime.of(LocalDate.parse(form.data, formatoData), LocalTime.parse(form.hora, formatoHora))
                    )
                }.getOrDefault(emptyList())

                if (offsets.size > 1) {
                    Selecao(
                        rotulo = "Deslocamento UTC (horário ambíguo)",
                        valor = form.offset ?: "",
                        opcoes = offsets,
                        nome = { it.toString() },
                        selecionar = { vm.alterar(form.copy(offset = it.toString())) },
                        enabled = !state.gravando
                    )
                }
            }
        }

        // Card 2: Serviços
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_service),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Serviços",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .focusRequester(foco("itens"))
                            .focusable()
                            .semantics { heading() }
                    )
                }
                Erro(erros["itens"])

                form.itens.forEachIndexed { index, item ->
                    key(item.id) {
                        val servico = state.servicos.find { it.id == item.servicoId }
                        val preco = vm.preco(item)

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${index + 1}. ${servico?.nome ?: "Serviço indisponível"}${if (servico?.ativo == false) " (Inativo)" else ""} · Preço: ${preco?.let(::reais) ?: ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                CampoDecimal(
                                    rotulo = "Desconto do item ${index + 1}",
                                    valor = item.desconto,
                                    alterar = { valor ->
                                        vm.alterar(vm.state.value.form.let { atual ->
                                            atual.copy(itens = atual.itens.map { if (it.id == item.id) it.copy(desconto = valor) else it })
                                        })
                                    },
                                    erro = erros[item.id],
                                    enabled = !state.gravando,
                                    modifier = Modifier.focusRequester(foco(item.id)),
                                    ultimo = index == form.itens.lastIndex && !form.dividido
                                )

                                val desconto = runCatching { centavos(item.desconto) }.getOrNull()
                                if (preco != null && desconto != null && desconto in 0..preco) {
                                    Text(
                                        "Líquido do item: ${reais(preco - desconto)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TextButton(
                                    onClick = { vm.alterar(form.copy(itens = form.itens.filterNot { it.id == item.id })) },
                                    enabled = !state.gravando
                                ) {
                                    Text(
                                        "Remover item ${index + 1} ${servico?.nome ?: ""}",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Selecao(
                    rotulo = "Adicionar serviço",
                    valor = "Adicionar serviço",
                    opcoes = state.servicos.filter { it.ativo },
                    nome = { "${it.nome} · ${reais(it.precoCentavos)}" },
                    selecionar = { vm.alterar(form.copy(itens = form.itens + ItemForm(it.id))) },
                    enabled = !state.gravando
                )

                if (state.servicos.none { it.ativo }) {
                    Text("Cadastre um serviço ativo na aba Cadastros.", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Card 3: Pagamento
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_receipt),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Pagamento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .focusRequester(foco("pagamento"))
                            .focusable()
                            .semantics { heading() }
                    )
                }
                Erro(erros["pagamento"])

                if (avaliacao.liquido == 0L) {
                    StatusBadge(texto = "Atendimento gratuito · sem pagamento", tipo = StatusTipo.MUTED)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !form.dividido,
                            onClick = { vm.alterar(form.copy(dividido = false)) },
                            label = { Text("Pagamento único") },
                            enabled = !state.gravando,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = form.dividido,
                            onClick = {
                                vm.alterar(form.copy(dividido = true, partes = form.partes.ifEmpty { listOf(ParteForm(valor = avaliacao.liquido?.let(::decimal) ?: "")) }))
                            },
                            label = { Text("Dividir pagamento") },
                            enabled = !state.gravando,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!form.dividido) {
                        Selecao(
                            rotulo = "Forma de pagamento",
                            valor = form.formaUnica.rotulo(),
                            opcoes = FormaPagamento.entries,
                            nome = { it.rotulo() },
                            selecionar = { vm.alterar(form.copy(formaUnica = it)) },
                            enabled = !state.gravando
                        )
                        Text(
                            "Pagamento integral: ${avaliacao.liquido?.let(::reais) ?: "Confira os serviços e descontos"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        form.partes.forEachIndexed { index, parte ->
                            key(parte.id) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Selecao(
                                            rotulo = "Forma da parte ${index + 1}",
                                            valor = parte.forma.rotulo(),
                                            opcoes = FormaPagamento.entries,
                                            nome = { it.rotulo() },
                                            selecionar = { forma ->
                                                vm.alterar(form.copy(partes = form.partes.map { if (it.id == parte.id) it.copy(forma = forma) else it }))
                                            },
                                            enabled = !state.gravando
                                        )

                                        CampoDecimal(
                                            rotulo = "Valor da parte ${index + 1}",
                                            valor = parte.valor,
                                            alterar = { valor ->
                                                vm.alterar(vm.state.value.form.let { atual ->
                                                    atual.copy(partes = atual.partes.map { if (it.id == parte.id) it.copy(valor = valor) else it })
                                                })
                                            },
                                            erro = erros[parte.id],
                                            enabled = !state.gravando,
                                            modifier = Modifier.focusRequester(foco(parte.id)),
                                            ultimo = index == form.partes.lastIndex
                                        )

                                        TextButton(
                                            onClick = { vm.alterar(form.copy(partes = form.partes.filterNot { it.id == parte.id })) },
                                            enabled = !state.gravando
                                        ) {
                                            Text("Remover parte ${index + 1}", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { vm.alterar(form.copy(partes = form.partes + ParteForm())) },
                            enabled = !state.gravando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Adicionar parte de pagamento")
                        }

                        val soma = runCatching { RegrasFinanceiras.soma(form.partes.map { centavos(it.valor) }) }.getOrNull()
                        soma?.let { total ->
                            Text("Total informado: ${reais(total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            avaliacao.liquido?.let { liquido ->
                                Text(
                                    if (total <= liquido) "Falta: ${reais(liquido - total)}" else "Excede: ${reais(total - liquido)}",
                                    color = if (total == liquido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Resumo & Ação
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
                    "Resumo Financeiro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                val taxa = if (form.barbeiroId == state.original?.atendimento?.barbeiroId) state.original?.atendimento?.taxaAplicadaBp
                else state.barbeiros.find { it.id == form.barbeiroId }?.taxaComissaoBp

                taxa?.let {
                    Text("Taxa aplicada: ${decimal(it.toLong())}%", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(avaliacao.subtotal?.let(::reais) ?: "-", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Descontos", style = MaterialTheme.typography.bodyMedium)
                    Text(avaliacao.descontos?.let(::reais) ?: "-", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Líquido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        avaliacao.liquido?.let(::reais) ?: "-",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = {
                        focus.clearFocus()
                        vm.salvar()
                        vm.avaliar().erros.keys.firstOrNull()?.let { focos[it]?.requestFocus() }
                    },
                    enabled = !state.gravando && !state.carregando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        if (state.gravando) "Salvando…" else if (state.original == null) "Concluir Atendimento" else "Salvar Alterações",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (descartar) {
        AlertDialog(
            onDismissRequest = { descartar = false },
            title = { Text("Descartar alterações?") },
            text = { Text("As alterações deste atendimento serão perdidas.") },
            confirmButton = {
                TextButton(onClick = { descartar = false; voltar() }) {
                    Text("Descartar alterações", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { descartar = false }) { Text("Continuar editando") }
            }
        )
    }
}
