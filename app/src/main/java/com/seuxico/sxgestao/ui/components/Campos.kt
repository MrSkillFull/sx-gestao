package com.seuxico.sxgestao.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.seuxico.sxgestao.model.*

@Composable
fun Erro(texto: String?) { if (texto != null) Text(texto, color = MaterialTheme.colorScheme.error,
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }

@Composable
fun <T> Selecao(rotulo: String, valor: String, opcoes: List<T>, nome: (T) -> String,
    selecionar: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var aberto by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(rotulo)
        Box {
            OutlinedButton(onClick = { aberto = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$rotulo: ${valor.ifBlank { "não selecionado" }}" }) { Text(valor.ifBlank { "Selecionar $rotulo" }) }
            DropdownMenu(aberto, { aberto = false }) {
                opcoes.forEach { opcao -> DropdownMenuItem(text = { Text(nome(opcao)) }, onClick = { aberto = false; selecionar(opcao) }) }
            }
        }
    }
}

@Composable
fun CampoDecimal(rotulo: String, valor: String, alterar: (String) -> Unit, modifier: Modifier = Modifier,
    erro: String? = null, enabled: Boolean = true, ultimo: Boolean = false) {
    var esteveEmFoco by remember { mutableStateOf(false) }
    var erroLocal by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    OutlinedTextField(valor, { erroLocal = null; alterar(it) }, label = { Text(rotulo) }, prefix = { Text("R$") },
        enabled = enabled, isError = erro != null || erroLocal != null,
        supportingText = { Text(erro ?: erroLocal ?: "Até duas casas decimais. Exemplo: 12,30.") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = if (ultimo) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }, onDone = { focus.clearFocus() }),
        modifier = modifier.fillMaxWidth().onFocusChanged {
            if (esteveEmFoco && !it.isFocused) {
                val numero = runCatching { centavos(valor) }.getOrNull()
                if (numero != null) alterar(decimal(numero)) else erroLocal = "Informe um valor válido com até duas casas."
            }
            esteveEmFoco = it.isFocused
        })
}
