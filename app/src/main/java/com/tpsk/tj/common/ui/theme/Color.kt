package com.tpsk.tj.common.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// --- Unified Brand Palette ---
val BrandPrimary = Color(0xFF003BA1) // Logo Blue Dark
val BrandAccent = Color(0xFF00D1FF)  // Logo Blue Light

// Core App Colors (Unified)
val AppPrimary = BrandPrimary
val AppAccent = BrandAccent

// Neutral Palette
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val GreyLight = Color(0xFFF0F2F5)
val GreyMedium = Color(0xFF8E8E93)
val GreyDark = Color(0xFF1C1C1E)

// Base Backgrounds
val AppBackgroundDarkTop = Color(0xFF041528)
val AppBackgroundDarkBottom = Color(0xFF000000)
val AppBackgroundLight = Color(0xFFF8F9FA)

// --- Compilation Fix Aliases (Redirecting to Unified Palette) ---
val LogoBlueDark = BrandPrimary
val LogoBlueLight = BrandAccent
val TwitterBlue = BrandAccent
val BoyishPurple = BrandPrimary 
val NiceOrange = Color(0xFFFF9800)
val SecondaryText = GreyMedium
val DividerColor = Color(0xFF2F3336)
val SurfaceColor = Color(0xFF16181C)

// Theme-aware accessors (MUST be used within @Composable)
val AppBackground @Composable get() = MaterialTheme.colorScheme.background
val AppSecondary @Composable get() = MaterialTheme.colorScheme.secondary
val AppTextPrimary @Composable get() = MaterialTheme.colorScheme.onBackground
val AppTextSecondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
