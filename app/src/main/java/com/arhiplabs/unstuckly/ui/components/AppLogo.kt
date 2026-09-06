package com.arhiplabs.unstuckly.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.arhiplabs.unstuckly.R
import com.arhiplabs.unstuckly.ui.theme.AppThemeMode
import com.arhiplabs.unstuckly.ui.theme.LocalAppPreferences

/**
 * Brand Logo component that dynamically switches between light theme (uplogo)
 * and dark theme (darl_logo) based on user preference and system theme.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = "Unstuckly"
) {
    val preferences = LocalAppPreferences.current
    val themeMode by preferences.themeMode.collectAsState()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val logoRes = if (isDark) R.drawable.darl_logo else R.drawable.uplogo

    Image(
        painter = painterResource(id = logoRes),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
