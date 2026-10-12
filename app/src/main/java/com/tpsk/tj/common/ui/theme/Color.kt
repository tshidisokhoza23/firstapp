package com.tpsk.tj.common.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// --- Light Theme Palette (Royal Blue Theme) ---
val LightPrimary = Color(0xFF0B57D0)        // Royal Blue #0B57D0
val LightAccent = Color(0xFF0284C7)         // Sky Accent #0284C7
val LightBottomNavBg = Color(0xFFE8EEF9)   // Soft ice blue
val LightBottomNavPill = Color(0xFFD3E3FD) // Active tab pill container
val LightBottomNavActive = Color(0xFF0B57D0) // Active icon & text
val LightBottomNavUnselected = Color(0xFF444746)

// --- Dark Theme Palette (Cyan-Teal & Purple Theme from Dark Mode screenshots) ---
val DarkPrimary = Color(0xFF00D2C2)         // Electric Cyan-Teal #00D2C2
val DarkAccent = Color(0xFF8B5CF6)          // Purple #8B5CF6
val DarkBackground = Color(0xFF121316)      // Dark Charcoal #121316
val DarkSurface = Color(0xFF1E2026)         // Dark Card Surface #1E2026
val DarkBottomNavBg = Color(0xFF121316)
val DarkBottomNavPill = Color(0xFF1E2B45)
val DarkBottomNavActive = Color(0xFF00D2C2)
val DarkBottomNavUnselected = Color(0xFF8E929B)

// Core Static Color Values
val BrandPrimary = LightPrimary
val BrandAccent = LightAccent
val AppPrimary = LightPrimary
val AppAccent = LightAccent

// Bottom Nav Colors
val AppBottomNavBg = LightBottomNavBg
val AppBottomNavPill = LightBottomNavPill
val AppBottomNavActive = LightBottomNavActive
val AppBottomNavUnselected = LightBottomNavUnselected

// Neutral Palette
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val GreyLight = Color(0xFFF2F4F8)
val GreyMedium = Color(0xFF747775)
val GreyDark = Color(0xFF1F1F1F)
val SurfaceElevated = Color(0xFF1E2026)
val TextTertiary = Color(0xFF747775)

// Base Backgrounds
val AppBackgroundDarkTop = Color(0xFF121316)
val AppBackgroundDarkBottom = Color(0xFF0E0F12)
val AppBackgroundLight = Color(0xFFFFFFFF)

// Static Color Aliases
val LogoBlueDark = LightPrimary
val LogoBlueLight = LightAccent
val TwitterBlue = LightPrimary
val BoyishPurple = LightPrimary 
val NiceOrange = Color(0xFFFF9800)
val SecondaryText = GreyMedium
val DividerColor = Color(0xFFE0E0E0)
val SurfaceColor = White

// Theme-aware accessors (ONLY inside Composable UI)
val AppBackground @Composable get() = MaterialTheme.colorScheme.background
val AppSecondary @Composable get() = MaterialTheme.colorScheme.secondary
val AppTextPrimary @Composable get() = MaterialTheme.colorScheme.onBackground
val AppTextSecondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
