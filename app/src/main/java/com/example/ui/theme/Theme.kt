package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanMinimalColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    onPrimary = Color.White,
    primaryContainer = MinimalPrimaryContainer,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    secondary = MinimalSecondary,
    onSecondary = Color.White,
    secondaryContainer = MinimalSecondaryContainer,
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = MinimalTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MinimalTertiaryContainer,
    onTertiaryContainer = Color(0xFF31111D),
    background = MinimalCanvas,
    onBackground = TextPrimary,
    surface = MinimalSurface,
    onSurface = TextPrimary,
    surfaceVariant = MinimalSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = MinimalSurfaceElevated,
    surfaceContainerHigh = MinimalSurfaceContainer,
    outline = MinimalBorder,
    outlineVariant = MinimalBorderActive,
    error = MinimalError,
    onError = Color.White,
    errorContainer = MinimalErrorContainer,
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanMinimalColorScheme,
        typography = Typography,
        content = content
    )
}
