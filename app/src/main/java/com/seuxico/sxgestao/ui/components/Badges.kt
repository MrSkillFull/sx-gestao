package com.seuxico.sxgestao.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seuxico.sxgestao.ui.theme.*

enum class StatusTipo {
    SUCESSO,
    ALERTA,
    MUTED,
}

@Composable
fun StatusBadge(texto: String, tipo: StatusTipo, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (tipo) {
        StatusTipo.SUCESSO -> StatusGreenContainer to StatusGreenText
        StatusTipo.ALERTA -> StatusAmberContainer to StatusAmberText
        StatusTipo.MUTED -> StatusMutedContainer to StatusMutedText
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = texto,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
