package com.example.firstapp.common.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Modern "Boyish" Purple & Blue Palette
val DeepIndigo = Color(0xFF1A237E)
val ModernPurple = Color(0xFF673AB7)
val BoyishPurple = Color(0xFF3F51B5)
val NiceOrange = Color(0xFFFF9800)
val DarkOrange = Color(0xFFF57C00)
val SlateBlue = Color(0xFF455A64)
val DarkGunmetal = Color(0xFF1C1C1E)
val Charcoal = Color(0xFF2C2C2E)
val SoftWhite = Color(0xFFF2F2F7)
val MutedSlate = Color(0xFF8E8E93)

// Brand Colors
val LogoBlueDark = Color(0xFF003BA1)
val LogoBlueLight = Color(0xFF00D1FF)

val AppPrimary = LogoBlueDark

// Base Colors for Theme Definition
val AppBackgroundDarkTop = Color(0xFF041528) // Deep Navy for gradient top
val AppBackgroundDarkBottom = Color(0xFF000000) // Pure Black for bottom
val AppBackgroundLight = Color(0xFFF0F2F5)
val AppSecondaryDark = Color(0xFF1C1C1E)

// Theme-aware accessors (MUST be used within @Composable)
val AppBackground @Composable get() = MaterialTheme.colorScheme.background
val AppSecondary @Composable get() = MaterialTheme.colorScheme.secondary
val AppTextPrimary @Composable get() = MaterialTheme.colorScheme.onBackground
val AppTextSecondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val AppAccent = LogoBlueLight

val DividerColor = Color(0xFF2F3336)
val SecondaryText = Color(0xFF71767B)
val TwitterBlue = Color(0xFF1D9BF0)
val SurfaceColor = Color(0xFF16181C)
