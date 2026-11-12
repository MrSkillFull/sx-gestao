package com.seuxico.sxgestao.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
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
    errorContainer = OnError, onErrorContainer = Error,
    inverseSurface = Foreground, inverseOnSurface = Background,
    inversePrimary = OrangeContainer, surfaceTint = Orange,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun SXGestaoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = Typography, shapes = AppShapes, content = content)
}
