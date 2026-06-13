package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MinimalPrimary,
    secondary = MinimalSecondary,
    tertiary = MinimalTertiary,
    background = CodeHighlightBg, // Darker lavender slate
    surface = MinimalSurfaceVariant,
    onBackground = MinimalOnBackground,
    onSurface = MinimalOnBackground,
    surfaceVariant = MinimalOutline,
    onSurfaceVariant = MinimalOnSurfaceVariant,
    outline = MinimalOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    secondary = MinimalSecondary,
    tertiary = MinimalTertiary,
    background = MinimalBg,
    surface = MinimalSurface,
    surfaceVariant = MinimalSurfaceVariant,
    onPrimary = MinimalBg,
    onSecondary = MinimalBg,
    onTertiary = MinimalBg,
    onBackground = MinimalOnBackground,
    onSurface = MinimalOnBackground,
    onSurfaceVariant = MinimalOnSurfaceVariant,
    outline = MinimalOutline,
    outlineVariant = MinimalOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Clean Minimalism defaults to Light
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
