package com.example.rusoit.ui.theme

import androidx.compose.ui.graphics.Color

/** Tokens HUD ITSIO1 (web dark default). */
object HudColors {
    // Backgrounds
    val BgPrimary = Color(0xFF1E1E20)
    val BgSecondary = Color(0xFF242426)
    val BgCard = Color(0xFF2C2C2F)
    val BgCardHover = Color(0xFF343438)
    val BgInput = Color(0xFF242426)
    val BgNav = Color(0xFF18181A)

    // Accents
    val AccentPrimary = Color(0xFFDC2626)
    val AccentSecondary = Color(0xFFEF4444)
    val AccentGlow = Color(0x2EDC2626) // 18% alpha
    val Amber = Color(0xFFF59E0B)
    val AmberGlow = Color(0x26F59E0B) // 15% alpha
    val Green = Color(0xFF22C55E)
    val GreenGlow = Color(0x1F22C55E) // 12% alpha
    val Blue = Color(0xFF3B82F6)
    val Orange = Color(0xFFF97316)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFF3F4F6)
    val TextMuted = Color(0xFFD1D5DB)
    val TextTertiary = Color(0xFFFCA5A5)

    // Borders
    val BorderSubtle = Color(0x17FFFFFF) // 9% alpha
    val BorderAccent = Color(0x73DC3838) // 45% alpha
}

// Global colors used by Theme.kt
val FireRed = HudColors.AccentPrimary
val FireOrange = HudColors.Orange
val FireYellow = HudColors.Amber

val DarkBackground = HudColors.BgPrimary
val DarkSurface = HudColors.BgSecondary
val DarkCard = HudColors.BgCard

val LightBackground = Color(0xFFC5CEDA) // From ITS_IO_TOKENS_LIGHT
val LightSurface = Color(0xFFE4EAF2)

val TextWhite = HudColors.TextPrimary
val TextGrey = HudColors.TextMuted
