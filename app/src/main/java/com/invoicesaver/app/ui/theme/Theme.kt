package com.invoicesaver.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A5FB4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF535F70),
    background = Color(0xFFF9F9FF),
    surface = Color(0xFFF9F9FF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C6FF),
    onPrimary = Color(0xFF002F66),
    primaryContainer = Color(0xFF00468F),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBBC7DB),
    background = Color(0xFF111318),
    surface = Color(0xFF111318)
)

@Composable
fun InvoiceSaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
