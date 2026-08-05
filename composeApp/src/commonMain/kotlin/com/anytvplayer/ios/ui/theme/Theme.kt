package com.anytvplayer.ios.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val TwitiDarkScheme = darkColorScheme(
    primary = TwitiMint,
    onPrimary = Color.Black,
    primaryContainer = TwitiCyan,
    onPrimaryContainer = Color.Black,
    secondary = TwitiGreen,
    onSecondary = Color.Black,
    secondaryContainer = TwitiTeal,
    onSecondaryContainer = Color.White,
    tertiary = GradientEnd,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextSecondary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextTertiary,
    outline = GlassBorder,
    outlineVariant = GlassWhite
)

private val TwitiLightScheme = lightColorScheme(
    primary = TwitiMint,
    onPrimary = Color.White,
    primaryContainer = TwitiCyan,
    onPrimaryContainer = Color.White,
    secondary = TwitiGreen,
    onSecondary = Color.White,
    secondaryContainer = TwitiTeal,
    onSecondaryContainer = Color.Black,
    tertiary = GradientEnd,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightGlassBorder,
    outlineVariant = LightGlassWhite
)

@Composable
fun TwitiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isCkb: Boolean = false,
    content: @Composable () -> Unit
) {
    val fontFamily = if (isCkb) SpedaFontFamily else FontFamily.SansSerif
    MaterialTheme(
        colorScheme = if (darkTheme) TwitiDarkScheme else TwitiLightScheme,
        typography = twitiTypography(fontFamily),
        content = content
    )
}
