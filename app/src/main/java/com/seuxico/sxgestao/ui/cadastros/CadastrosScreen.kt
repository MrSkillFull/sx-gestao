package com.seuxico.sxgestao.ui.cadastros

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.R
import com.seuxico.sxgestao.model.*
import com.seuxico.sxgestao.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun CadastrosScreen(vm: CadastrosViewModel = gestaoViewModel { CadastrosViewModel(it) }) {
    val state by vm.state.collectAsStateWithLifecycle()
    var barbeiros by rememberSaveable { mutableStateOf(true) }
    var confirmar by remember { mutableStateOf(false) }
    val form = state.form
    var valorEmFoco by remember(form?.id) { mutableStateOf(false) }
    val nomeFocus = remember { FocusRequester() }
    val valorFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val lista = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BackHandler(form != null) { vm.abrir(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        state = lista,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SXGestaoTopBar(
                titulo = if (form == null) "Cadastros" else if (form.existente) "Editar Cadastro" else "Novo Cadastro",
                subtitulo = if (form == null) stringResource(R.string.cadastros_description) else null,
                voltar = if (form != null) { { vm.abrir(null) } } else null
            )
        }

        if (state.carregando) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        state.erro?.let { erro ->
            item {
                Text(
                    erro,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
                if (form == null) TextButton(onClick = vm::carregar) { Text("Tentar novamente") }
            }
        }

        state.mensagem?.let {
            item { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }

        if (form == null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = barbeiros,
                        onClick = { barbeiros = true },
                        label = { Text("Barbeiros", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_barber),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !barbeiros,
                        onClick = { barbeiros = false },
                        label = { Text("Serviços", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_service),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Button(
                    onClick = { vm.abrir(CadastroForm(barbeiros)) },
                    enabled = !state.carregando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (barbeiros) "Novo Barbeiro" else "Novo Serviço",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (barbeiros) {
                if (state.barbeiros.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Nenhum barbeiro cadastrado. Cadastre o primeiro barbeiro para iniciar.",
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(state.barbeiros, key = { it.id }) { b ->
                    OutlinedCard(
                        onClick = {
                            vm.abrir(
                                CadastroForm(
                                    barbeiro = true,
                                    id = b.id,
                                    nome = b.nome,
                                    valor = decimal(b.taxaComissaoBp.toLong()),
                                    proprietario = b.proprietario,
                                    ativo = b.ativo,
                                    existente = true
                                )
                            )
                        },
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
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            b.nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (b.proprietario) {
                                            StatusBadge(texto = "Proprietário", tipo = StatusTipo.MUTED)
                                        }
                                    }
                                    Text(
                                        "Taxa: ${decimal(b.taxaComissaoBp.toLong())}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusBadge(
                                    texto = if (b.ativo) "Ativo" else "Inativo",
                                    tipo = if (b.ativo) StatusTipo.SUCESSO else StatusTipo.MUTED
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
            } else {
                if (state.servicos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Nenhum serviço cadastrado. Cadastre o primeiro serviço para o formulário.",
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(state.servicos, key = { it.id }) { s ->
                    OutlinedCard(
                        onClick = {
                            vm.abrir(
                                CadastroForm(
                                    barbeiro = false,
                                    id = s.id,
                                    nome = s.nome,
                                    valor = decimal(s.precoCentavos),
                                    ativo = s.ativo,
                                    existente = true
                                )
                            )
                        },
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
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_service),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        s.nome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        reais(s.precoCentavos),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusBadge(
                                    texto = if (s.ativo) "Ativo" else "Inativo",
                                    tipo = if (s.ativo) StatusTipo.SUCESSO else StatusTipo.MUTED
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
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = form.nome,
                            onValueChange = { vm.alterar(form.copy(nome = it, erroNome = null)) },
                            label = { Text("Nome do cadastro") },
                            enabled = !state.gravando,
                            isError = form.erroNome != null,
                            supportingText = { form.erroNome?.let { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nomeFocus),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { valorFocus.requestFocus() })
                        )

                        if (form.barbeiro && !form.existente) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = form.proprietario,
                                    onCheckedChange = {
                                        vm.alterar(form.copy(proprietario = it, valor = if (it) "0,00" else "50,00"))
                                    },
                                    enabled = !state.gravando,
                                    modifier = Modifier.semantics { contentDescription = "Barbeiro proprietário, taxa fixa em zero por cento" }
                                )
                                Text(
                                    "Barbeiro proprietário (taxa fixa em 0%)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        OutlinedTextField(
                            value = form.valor,
                            onValueChange = { vm.alterar(form.copy(valor = it, erroValor = null)) },
                            label = { Text(if (form.barbeiro) "Taxa de comissão" else "Preço do serviço") },
                            readOnly = form.proprietario,
                            enabled = !state.gravando,
                            isError = form.erroValor != null,
                            prefix = { if (!form.barbeiro) Text("R$ ") },
                            suffix = { if (form.barbeiro) Text("%") },
                            supportingText = {
                                Text(
                                    form.erroValor ?: if (form.proprietario) "Taxa fixa do proprietário."
                                    else if (form.barbeiro) "De 0 a 100%, até duas casas decimais."
                                    else "Exemplo: 30,00. Até duas casas decimais."
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(valorFocus)
                                .onFocusChanged {
                                    if (valorEmFoco && !it.isFocused) vm.normalizarValor()
                                    valorEmFoco = it.isFocused
                                },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
                        )

                        Button(
                            onClick = {
                                vm.salvar()
                                val f = vm.state.value.form
                                if (f?.erroNome != null || f?.erroValor != null) scope.launch {
                                    val nome = f.erroNome != null
                                    lista.scrollToItem(if (nome) 3 else if (form.barbeiro && !form.existente) 5 else 4)
                                    withFrameNanos { }
                                    if (nome) nomeFocus.requestFocus() else valorFocus.requestFocus()
                                }
                            },
                            enabled = !state.gravando && !state.carregando,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                if (state.gravando) "Salvando…" else "Salvar Cadastro",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (form.existente && form.ativo) {
                            OutlinedButton(
                                onClick = { confirmar = true },
                                enabled = !state.gravando,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Inativar Cadastro", color = MaterialTheme.colorScheme.error)
                            }
                        }

                        if (!form.ativo) {
                            StatusBadge(texto = "Inativo · Preservado no histórico", tipo = StatusTipo.MUTED)
                        }
                    }
                }
            }
        }
    }

    if (confirmar && form != null) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Inativar ${form.nome}?") },
            text = { Text("O cadastro sairá das novas seleções e continuará associado aos atendimentos anteriores.") },
            confirmButton = {
                TextButton(onClick = { confirmar = false; vm.salvar(inativar = true) }) {
                    Text("Inativar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text("Voltar") }
            }
        )
    }
}
