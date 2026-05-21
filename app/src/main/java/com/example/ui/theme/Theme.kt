package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberAccent,
    onPrimary = Color(0xFF070B0E),
    secondary = DeepPurple,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = Color(0xFFECEFF4),
    surface = DarkSurface,
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFECEFF4),
    outline = GrayBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
