package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = SoftRosePink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3F192C),
    onPrimaryContainer = Color(0xFFFFDDE4),
    secondary = WarmPeach,
    onSecondary = WarmChocoPlumDark,
    secondaryContainer = Color(0xFF3B1E19),
    onSecondaryContainer = WarmPeachLight,
    tertiary = WarmHoneyGold,
    background = ChocolateDarkBg,       // Qora fonda shokolad rang fon
    surface = WarmChocoPlumSurface,       // Soft Glass Surface (#251A2D)
    surfaceVariant = WarmChocoPlumElevated, // Elevated Card Surface (#32233B)
    onBackground = Color(0xFFFDF4F5),
    onSurface = Color(0xFFFDF4F5),
    onSurfaceVariant = Color(0xFFD4B8C7),
    outline = SoftGlassBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = SoftRosePink,
    onPrimary = Color.White,
    primaryContainer = WarmPeachSoft,
    onPrimaryContainer = Color(0xFFBE123C),
    secondary = WarmPeach,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDE6),
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = WarmHoneyGold,
    background = PureWhiteBg, // Oq fonda prosta oq
    surface = SurfaceWhite,
    surfaceVariant = Color(0xFFFFF0F3),
    onBackground = Color(0xFF1E1424),
    onSurface = Color(0xFF1E1424),
    onSurfaceVariant = Color(0xFF786270),
    outline = Color(0x25F43F5E)
)

@Composable
fun VisolTheme(
    themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

