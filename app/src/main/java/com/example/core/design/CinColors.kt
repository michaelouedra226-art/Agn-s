package com.example.core.design

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object CinColors {
    // Backgrounds
    val BgVoid = Color(0xFF0A0A0F)       // Deep cinema void
    val BgSurface = Color(0xFF13131A)    // Standard surface
    val BgElevated = Color(0xFF1C1C25)   // Elevated cards
    val BgHover = Color(0xFF232330)      // Pressed / hover state

    // Borders
    val BorderSubtle = Color(0xFF1F1F2A)
    val BorderDefault = Color(0xFF2A2A38)
    val BorderAccent = Color(0x668B5CF6)

    // Typography
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecond = Color(0xFFA0A0B0)
    val TextTertiary = Color(0xFF6E6E80)

    // Accents
    val AccentViolet = Color(0xFF8B5CF6) // Primary Studio Violet
    val AccentPink = Color(0xFFEC4899)   // Secondary Magenta
    val AccentCyan = Color(0xFF06B6D4)   // Tertiary Cyan
    val Success = Color(0xFF10B981)      // Green success
    val Warning = Color(0xFFF59E0B)      // Amber warning
    val Danger = Color(0xFFEF4444)       // Red danger

    // Gradients
    val GradientFilm = Brush.linearGradient(listOf(AccentViolet, AccentPink))
    val GradientGemini = Brush.linearGradient(listOf(Color(0xFF4285F4), AccentViolet))
    val GradientAgnes = Brush.linearGradient(listOf(Warning, AccentPink))
    val GradientCyanViolet = Brush.linearGradient(listOf(AccentCyan, AccentViolet))
    val GradientCard = Brush.verticalGradient(listOf(Color(0x228B5CF6), Color(0x00000000)))

    // Light equivalents
    val LightBgVoid = Color(0xFFFAFAFB)
    val LightBgSurface = Color(0xFFFFFFFF)
    val LightBgElevated = Color(0xFFF0F0F5)
    val LightTextPrimary = Color(0xFF1A1A1F)
    val LightTextSecond = Color(0xFF6B7280)
    val LightBorder = Color(0xFFE5E7EB)
}
