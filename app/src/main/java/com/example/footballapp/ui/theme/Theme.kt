package com.example.footballapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Expose a global theme state that ANY composable can read
val LocalIsDarkTheme = compositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary          = DarkAccentGreen,
    onPrimary        = Color(0xFF003319),
    primaryContainer = DarkAccentDim,
    background       = DarkBackground,
    surface          = DarkSurface,
    surfaceVariant   = DarkCard,
    onBackground     = DarkTextPrimary,
    onSurface        = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline          = DarkBorder,
    error            = DarkRedCard,
)

private val LightColorScheme = lightColorScheme(
    primary          = LightAccentGreen,
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = LightAccentDim,
    background       = LightBackground,
    surface          = LightSurface,
    surfaceVariant   = LightCard,
    onBackground     = LightTextPrimary,
    onSurface        = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline          = LightBorder,
    error            = LightRedCard,
)

@Composable
fun FootballPlusTheme(
    darkTheme: Boolean,          // pass this from your ViewModel/DataStore
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            content     = content
        )
    }
}
