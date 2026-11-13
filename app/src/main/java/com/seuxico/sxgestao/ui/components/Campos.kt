package com.seuxico.sxgestao.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.seuxico.sxgestao.R
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
    selecionar: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    destacado: Boolean = false, adicionar: Boolean = false) {
    var aberto by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (destacado) 8.dp else 0.dp)) {
        if (!adicionar) Text(rotulo)
        Box {
            val botaoModifier = Modifier.fillMaxWidth().semantics {
                contentDescription = if (adicionar) rotulo else "$rotulo: ${valor.ifBlank { "não selecionado" }}"
            }
            if (destacado) {
                OutlinedButton(
                    onClick = { aberto = true },
                    enabled = enabled,
                    modifier = botaoModifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (adicionar) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        valor.ifBlank { "Selecionar $rotulo" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!adicionar) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(onClick = { aberto = true }, enabled = enabled, modifier = botaoModifier) {
                    Text(valor.ifBlank { "Selecionar $rotulo" })
                }
            }
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
