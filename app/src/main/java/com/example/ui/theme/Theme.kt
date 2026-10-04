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
import com.example.core.design.CinColors

private val CinDarkColorScheme = darkColorScheme(
    primary = CinColors.AccentViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1065),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = CinColors.AccentPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF701A75),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = CinColors.AccentCyan,
    onTertiary = Color.Black,
    background = CinColors.BgVoid,
    onBackground = CinColors.TextPrimary,
    surface = CinColors.BgSurface,
    onSurface = CinColors.TextPrimary,
    surfaceVariant = CinColors.BgElevated,
    onSurfaceVariant = CinColors.TextSecond,
    outline = CinColors.BorderDefault,
    outlineVariant = CinColors.BorderSubtle,
    error = CinColors.Danger,
    onError = Color.White
)

private val CinLightColorScheme = lightColorScheme(
    primary = CinColors.AccentViolet,
    onPrimary = Color.White,
    secondary = CinColors.AccentPink,
    onSecondary = Color.White,
    tertiary = CinColors.AccentCyan,
    onTertiary = Color.Black,
    background = CinColors.LightBgVoid,
    onBackground = CinColors.LightTextPrimary,
    surface = CinColors.LightBgSurface,
    onSurface = CinColors.LightTextPrimary,
    surfaceVariant = CinColors.LightBgElevated,
    onSurfaceVariant = CinColors.LightTextSecond,
    outline = CinColors.LightBorder,
    error = CinColors.Danger,
    onError = Color.White
)

@Composable
fun CineIATheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CinDarkColorScheme
        else -> CinLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
