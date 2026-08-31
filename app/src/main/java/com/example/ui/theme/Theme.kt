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
    primary = PrimarySkyLight,
    onPrimary = BentoBackground,
    primaryContainer = BentoCardElevated,
    onPrimaryContainer = Slate50,
    secondary = PrimaryCyan,
    onSecondary = BentoBackground,
    background = BentoBackground,
    surface = BentoCardBg,
    onSurface = Slate50,
    surfaceVariant = BentoCardInner,
    onSurfaceVariant = Slate200,
    outline = BentoBorder,
    outlineVariant = BentoBorderSubtle,
    error = RecordRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimarySky,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = BentoCardBg,
    secondary = PrimaryCyan,
    onSecondary = Color.White,
    background = Slate50,
    surface = Color.White,
    onSurface = BentoCardBg,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = RecordRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
