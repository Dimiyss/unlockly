package com.arhiplabs.unstuckly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Static Base Palette Tokens
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate400 = Color(0xFF94A3B8)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)
val PureWhite = Color(0xFFFFFFFF)

// Static tokens used when configuring ColorScheme
val DarkBackground = Slate900
val DarkSurface = Slate800
val DarkSurfaceVariant = Slate700
val DarkTextPrimary = Slate50
val DarkTextSecondary = Slate400
val DarkTextMuted = Color(0xFF64748B)
val DarkOutline = Slate700

val LightBackground = Slate50
val LightSurface = PureWhite
val LightSurfaceVariant = Slate100
val LightTextPrimary = Slate900
val LightTextSecondary = Slate600
val LightTextMuted = Slate400
val LightOutline = Color(0xFFE2E8F0)

// Vibrant Brand Accents
val PrimaryIndigo = Color(0xFF6366F1)
val SecondaryPurple = Color(0xFFA855F7)
val AccentPink = Color(0xFFEC4899)
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRose = Color(0xFFF43F5E)

// Dynamic theme-aware accessors for seamless light/dark mode support
val BackgroundDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val SurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val SurfaceVariantDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

val AppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val AppSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val AppSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val AppTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val AppTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val AppOutline: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline
