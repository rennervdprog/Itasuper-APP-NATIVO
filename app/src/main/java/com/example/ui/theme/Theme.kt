package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ItaSuperPrimary,
    onPrimary = Color.White,
    primaryContainer = ItaSuperHighlightBg,
    onPrimaryContainer = ItaSuperHighlightText,
    secondary = ItaSuperSecondary,
    onSecondary = ItaSuperTextPrimary,
    tertiary = ItaSuperWarning,
    background = Color(0xFF18181B),
    onBackground = Color.White,
    surface = Color(0xFF27272A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3F3F46),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = ItaSuperBorder,
    error = ItaSuperError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ItaSuperPrimary,
    onPrimary = Color.White,
    primaryContainer = ItaSuperHighlightBg,
    onPrimaryContainer = ItaSuperHighlightText,
    secondary = ItaSuperSecondary,
    onSecondary = ItaSuperTextPrimary,
    tertiary = ItaSuperWarning,
    background = ItaSuperBackground,
    onBackground = ItaSuperTextPrimary,
    surface = ItaSuperBackground,
    onSurface = ItaSuperTextPrimary,
    surfaceVariant = ItaSuperSecondary,
    onSurfaceVariant = ItaSuperTextSecondary,
    outline = ItaSuperBorder,
    error = ItaSuperError,
    onError = Color.White
)

@Composable
fun ItaSuperTheme(
    darkTheme: Boolean = false, // Always force light theme as specified in design system
    dynamicColor: Boolean = false, // Maintain ItaSuper brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
