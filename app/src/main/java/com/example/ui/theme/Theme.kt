package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DeckACyan,
    onPrimary = Color(0xFF020617),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = DeckBAmber,
    onSecondary = Color(0xFF020617),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFFFECB3),
    tertiary = DspSurroundViolet,
    onTertiary = Color.White,
    background = AuraBackground,
    onBackground = TextPrimary,
    surface = AuraSurface,
    onSurface = TextPrimary,
    surfaceVariant = AuraSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = AuraBorder,
    error = DangerRed,
    onError = Color.White
)

private val LightColorScheme = DarkColorScheme // AuraDeck is an audiophile console, designed dark-first

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve audiophile neon aesthetic
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

