package com.arhiplabs.unstuckly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    secondary = SecondaryPurple,
    tertiary = AccentPink,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = DarkTextPrimary,
    onSecondary = DarkTextPrimary,
    onTertiary = DarkTextPrimary,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = ErrorRose
)

val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    secondary = SecondaryPurple,
    tertiary = AccentPink,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onTertiary = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = ErrorRose
)

@Composable
fun UnstucklyTheme(
    preferences: AppPreferences = AppPreferences.getInstance(LocalContext.current),
    content: @Composable () -> Unit
) {
    val themeMode by preferences.themeMode.collectAsState()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalAppPreferences provides preferences
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
