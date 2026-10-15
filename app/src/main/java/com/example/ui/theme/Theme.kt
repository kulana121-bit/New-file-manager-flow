package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = CyanGlow,
    secondary = PurpleGlow,
    onSecondary = Color.White,
    background = DarkBg,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkGlassBorder,
    error = DangerRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = VioletGlow,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF1E293B),
    secondary = PurpleGlow,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = LightCardBg,
    onSurfaceVariant = Color(0xFF64748B),
    outline = LightGlassBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun FileFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
