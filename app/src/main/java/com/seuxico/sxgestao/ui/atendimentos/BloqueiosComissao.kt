package com.seuxico.sxgestao.ui.atendimentos

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import com.seuxico.sxgestao.data.local.Barbeiro
import com.seuxico.sxgestao.data.repository.ParSemana
import java.time.LocalDate

@Composable
fun BloqueiosComissao(bloqueios: List<ParSemana>, barbeiros: List<Barbeiro>, abrir: (String, Long) -> Unit) {
    bloqueios.forEach { par ->
        val segunda = LocalDate.ofEpochDay(par.semana)
        val nome = barbeiros.find { it.id == par.barbeiroId }?.nome ?: "Barbeiro"
        Text("Comissão confirmada para $nome, de ${segunda.format(formatoData)} a ${segunda.plusDays(6).format(formatoData)}. Desfaça a confirmação para continuar.",
            color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        TextButton(onClick = { abrir(par.barbeiroId, par.semana) }) { Text("Ver comissão · $nome · ${segunda.format(formatoData)}") }
    }
}
