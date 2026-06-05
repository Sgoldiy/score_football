package com.footballpluse.footballapp.ui.theme

import androidx.compose.ui.graphics.Color

// === DARK THEME ===
val DarkBackground    = Color(0xFF0D0F14)
val DarkSurface       = Color(0xFF1A1E2A)
val DarkCard          = Color(0xFF131620)
val DarkBorder        = Color(0xFF1A1E2A)
val DarkAccentGreen   = Color(0xFF00E676)
val DarkAccentDim     = Color(0xFF00E676).copy(alpha = 0.7f)
val DarkTextPrimary   = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF8B949E)
val DarkTextMuted     = Color(0xFF484F58)
val DarkLive          = Color(0xFFFF4444)
val DarkYellowCard    = Color(0xFFF0C419)
val DarkRedCard       = Color(0xFFFF4444)
val DarkGoal          = Color(0xFF00E676)
val DarkTabBar        = Color(0xFF131620)

// === LIGHT THEME ===
val LightBackground    = Color(0xFFF0F2F5)
val LightSurface       = Color(0xFFFFFFFF)
val LightCard          = Color(0xFFFFFFFF)
val LightBorder        = Color(0xFFE1E4E8)
val LightAccentGreen   = Color(0xFF1A8A4A)
val LightAccentDim     = Color(0xFF2EA866)
val LightTextPrimary   = Color(0xFF0D1117)
val LightTextSecondary = Color(0xFF57606A)
val LightTextMuted     = Color(0xFF8C959F)
val LightLive          = Color(0xFF1A8A4A)
val LightYellowCard    = Color(0xFFB8860B)
val LightRedCard       = Color(0xFFCC2222)
val LightGoal          = Color(0xFF1A8A4A)
val LightTabBar        = Color(0xFFFFFFFF)

// Shared
val Transparent = Color(0x00000000)

// Backward Compatibility / Specific Tokens
val LiveGreen = DarkAccentGreen
val IceBlue = Color(0xFF63B3ED)
val SignalAmber = DarkYellowCard
val DangerRed = DarkRedCard
val PitchBlack = DarkBackground
val PitchSurface = DarkSurface
val PitchSurfaceHigh = DarkCard
val PitchLine = DarkBorder
val TextSecondary = DarkTextSecondary

// Dark Glass Design Tokens
val GlassBg = Color(0xFF131620)
val GlassBorder = Color(0xFF1A1E2A)
val GlassGlowGreen = Color(0xFF00E676)
val GlassGlowDim = Color(0xFF1A1E2A)
val DeepNavy = Color(0xFF0D0F14)
val ScoreGreen = Color(0xFF00E676)
val ScoreDim = Color(0x66FFFFFF)
val CardOverlay = Color(0xFF131620)
