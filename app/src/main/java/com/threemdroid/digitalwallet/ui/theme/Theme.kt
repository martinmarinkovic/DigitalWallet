package com.threemdroid.digitalwallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WalletAccent,
    onPrimary = DarkText,
    primaryContainer = WalletAccentDark,
    onPrimaryContainer = DarkText,
    secondary = DarkChromeAlt,
    onSecondary = DarkText,
    secondaryContainer = DarkChromeAlt,
    onSecondaryContainer = DarkText,
    tertiary = WalletAccent,
    onTertiary = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkChrome,
    onSurface = DarkText,
    surfaceVariant = DarkChromeAlt,
    onSurfaceVariant = DarkMutedText,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = WalletAccent,
    onPrimary = Color.White,
    primaryContainer = WalletAccent,
    onPrimaryContainer = Color.White,
    secondary = LightChromeAlt,
    onSecondary = LightText,
    secondaryContainer = LightChromeAlt,
    onSecondaryContainer = LightText,
    tertiary = WalletAccent,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightText,
    surface = LightChrome,
    onSurface = LightText,
    surfaceVariant = LightChromeAlt,
    onSurfaceVariant = LightMutedText,
    outline = LightOutline
)

@Composable
fun DigitalWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor ->
            if (darkTheme) DarkColorScheme else LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
