package com.seuxico.sxgestao.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.seuxico.sxgestao.model.TemaAplicativo
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BrandColors = lightColorScheme(
    primary = Orange, onPrimary = OnOrange,
    primaryContainer = OrangeContainer, onPrimaryContainer = OnOrangeContainer,
    secondary = Orange, onSecondary = OnOrange,
    secondaryContainer = OrangeContainer, onSecondaryContainer = OnOrangeContainer,
    tertiary = MutedForeground, onTertiary = Background,
    background = Background, onBackground = Foreground,
    surface = Surface, onSurface = Foreground,
    surfaceVariant = SurfaceVariant, onSurfaceVariant = MutedForeground,
    surfaceDim = Background, surfaceBright = SurfaceVariant,
    surfaceContainerLowest = Background, surfaceContainerLow = Surface,
    surfaceContainer = Surface, surfaceContainerHigh = SurfaceVariant,
    surfaceContainerHighest = SurfaceVariant,
    outline = Outline, outlineVariant = SurfaceVariant,
    error = Error, onError = OnError,
    errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
    inverseSurface = OrangeContainer, inverseOnSurface = OnOrangeContainer,
    inversePrimary = OrangeContainer, surfaceTint = Orange,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9A52), onPrimary = Color(0xFF231100),
    primaryContainer = Color(0xFF592B00), onPrimaryContainer = Color(0xFFFFDCC0),
    secondary = Color(0xFFFF9A52), onSecondary = Color(0xFF231100),
    secondaryContainer = Color(0xFF592B00), onSecondaryContainer = Color(0xFFFFDCC0),
    tertiary = Color(0xFFCFC4BC), onTertiary = Color(0xFF121212),
    background = Color(0xFF121212), onBackground = Color(0xFFF4F1EF),
    surface = Color(0xFF1E1E1E), onSurface = Color(0xFFF4F1EF),
    surfaceVariant = Color(0xFF282828), onSurfaceVariant = Color(0xFFCFC4BC),
    surfaceDim = Color(0xFF121212), surfaceBright = Color(0xFF282828),
    surfaceContainerLowest = Color(0xFF121212), surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF1E1E1E), surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF282828),
    outline = Color(0xFF423D3A), outlineVariant = Color(0xFF282828),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF690005), onErrorContainer = Color(0xFFFFB4AB),
    inverseSurface = Color(0xFFF4F1EF), inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFF592B00), surfaceTint = Color(0xFFFF9A52),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun SXGestaoTheme(
    tema: TemaAplicativo = TemaAplicativo.LARANJA,
    content: @Composable () -> Unit,
) {
    val colors = if (tema == TemaAplicativo.PRETO) DarkColors else BrandColors
    MaterialTheme(colorScheme = colors, typography = Typography, shapes = AppShapes, content = content)
}
