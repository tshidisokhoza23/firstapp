package com.tpsk.tj.common.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSurface,
    tertiary = DarkAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = White,
    onSecondary = White,
    onBackground = White,
    onSurface = White,
    outline = Color(0xFF2A2C34),
    surfaceVariant = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = Color(0xFFF2F2F7),
    tertiary = LightAccent,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = Black,
    onBackground = Black,
    onSurface = Black,
    outline = GreyMedium,
    surfaceVariant = GreyLight
)

@Composable
fun TJTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
