package com.example.footballapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LiveGreen,
    secondary = IceBlue,
    tertiary = SignalAmber,
    background = PitchBlack,
    surface = PitchSurface,
    onPrimary = Color.Black,
    onSecondary = PitchBlack,
    onTertiary = PitchBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = PitchSurfaceHigh,
    outline = PitchLine,
    error = DangerRed
)

private val LightColorScheme = DarkColorScheme // Force dark theme for now as requested

@Composable
fun FootballAppTheme(
    darkTheme: Boolean = true, // Force dark theme for the premium look
    dynamicColor: Boolean = false, // Disable dynamic color to maintain the strict premium identity
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
