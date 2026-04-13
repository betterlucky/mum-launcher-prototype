package com.daveharris.mumlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E67D1),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4C82E8),
    background = Color(0xFFEAF2FF),
    surface = Color(0xFFF8FBFF),
    onSurface = Color(0xFF16304F),
    error = Color(0xFFA63A29),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BC2FF),
    onPrimary = Color(0xFF0D2347),
    secondary = Color(0xFFB8D2FF),
    background = Color(0xFF0F1726),
    surface = Color(0xFF172338),
    onSurface = Color(0xFFEAF2FF),
    error = Color(0xFFFFB4A8),
)

@Composable
fun MumLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
