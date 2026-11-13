package com.seuxico.sxgestao.ui.configuracoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.seuxico.sxgestao.model.TemaAplicativo

@Composable
internal fun SeletorTema(tema: TemaAplicativo, selecionar: (TemaAplicativo) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aparência", style = MaterialTheme.typography.titleLarge)
            Text(
                "Escolha as cores do aplicativo. Sua escolha será mantida neste aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.selectableGroup()) {
                listOf(
                    TemaAplicativo.PRETO to "Preto e laranja",
                    TemaAplicativo.LARANJA to "Laranja e preto",
                ).forEach { (opcao, titulo) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = 56.dp)
                            .selectable(
                                selected = tema == opcao,
                                onClick = { selecionar(opcao) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = tema == opcao, onClick = null)
                        Text(titulo, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
