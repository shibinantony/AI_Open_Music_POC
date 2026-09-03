package com.brave.jsabmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SaavnTeal,
    secondary = SaavnTealAccent,
    background = AmoledBlack,
    surface = AmoledSurface,
    surfaceVariant = AmoledCard,
    onPrimary = AmoledBlack,
    onSecondary = AmoledBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun JSABMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
