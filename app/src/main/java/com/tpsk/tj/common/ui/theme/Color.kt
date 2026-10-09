package com.tpsk.tj.common.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// --- Unified Brand Palette ---
val BrandPrimary = Color(0xFF14B8A6) // Teal
val BrandAccent = Color(0xFFA855F7)  // Purple

// Core App Colors (Unified)
val AppPrimary = BrandPrimary
val AppAccent = BrandAccent

// Neutral Palette
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val GreyLight = Color(0xFFF0F2F5)
val GreyMedium = Color(0xFFA1A1AA)
val GreyDark = Color(0xFF222222)
val SurfaceElevated = Color(0xFF2A2A2A)
val TextTertiary = Color(0xFF71717A)

// Base Backgrounds
val AppBackgroundDarkTop = Color(0xFF1A1A1A)
val AppBackgroundDarkBottom = Color(0xFF1A1A1A)
val AppBackgroundLight = Color(0xFFF8F9FA)

// --- Compilation Fix Aliases (Redirecting to Unified Palette) ---
val LogoBlueDark = BrandPrimary
val LogoBlueLight = BrandAccent
val TwitterBlue = BrandAccent
val BoyishPurple = BrandPrimary 
val NiceOrange = Color(0xFFFF9800)
val SecondaryText = GreyMedium
val DividerColor = Color(0xFF333333)
val SurfaceColor = Color(0xFF222222)

// Theme-aware accessors (MUST be used within @Composable)
val AppBackground @Composable get() = MaterialTheme.colorScheme.background
val AppSecondary @Composable get() = MaterialTheme.colorScheme.secondary
val AppTextPrimary @Composable get() = MaterialTheme.colorScheme.onBackground
val AppTextSecondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
