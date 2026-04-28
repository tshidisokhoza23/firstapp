package com.example.firstapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    tertiary = AppAccent,
    background = AppBackground,
    surface = AppSecondary,
    onPrimary = Color.White,
    onSecondary = SoftWhite,
    onBackground = SoftWhite,
    onSurface = SoftWhite
)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    tertiary = BoyishPurple,
    background = Color.White,
    surface = SoftWhite,
    onPrimary = Color.White,
    onSecondary = DarkGunmetal,
    onBackground = DarkGunmetal,
    onSurface = DarkGunmetal
)

@Composable
fun FirstAppTheme(
    darkTheme: Boolean = true, // Force dark theme for now to match AppBackground usage
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to ensure custom theme colors are used
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}