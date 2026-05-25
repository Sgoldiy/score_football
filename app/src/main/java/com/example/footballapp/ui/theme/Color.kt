package com.example.footballapp.ui.theme

import androidx.compose.ui.graphics.Color

// === DARK THEME ===
val DarkBackground    = Color(0xFF0D1117)
val DarkSurface       = Color(0xFF161B22)
val DarkCard          = Color(0xFF1C2128)
val DarkBorder        = Color(0xFF30363D)
val DarkAccentGreen   = Color(0xFF00E676)
val DarkAccentDim     = Color(0xFF1A8A4A)
val DarkTextPrimary   = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF8B949E)
val DarkTextMuted     = Color(0xFF484F58)
val DarkLive          = Color(0xFF00E676)
val DarkYellowCard    = Color(0xFFF0C419)
val DarkRedCard       = Color(0xFFE74C3C)
val DarkGoal          = Color(0xFF00E676)
val DarkTabBar        = Color(0xFF0D1117)

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
val GlassBg = Color(0x0DFFFFFF)         // very subtle white overlay
val GlassBorder = Color(0x1AFFFFFF)    // subtle white border
val GlassGlowGreen = Color(0xFF00FF87) // electric green glow for live
val GlassGlowDim = Color(0x33FFFFFF)   // dimmed grey glow for finished
val DeepNavy = Color(0xFF0A0E1A)       // base background
val ScoreGreen = Color(0xFF00FF87)     // live score electric green
val ScoreDim = Color(0x66FFFFFF)       // dimmed score for finished games
val CardOverlay = Color(0x08FFFFFF)    // ultra-subtle card base
