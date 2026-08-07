package com.example.firstapp.home.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.firstapp.R

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.ColorFilter

@Composable
fun AppLogo(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    // Use the main high-res logo for everything
    val logoRes = R.drawable.app_logo_main
    
    // If no tint provided, keep original brand colors (Blue/Cyan)
    val finalColorFilter = if (tint != null) ColorFilter.tint(tint) else null
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "App Logo",
            modifier = Modifier.fillMaxSize(),
            colorFilter = finalColorFilter
        )
    }
}






@Composable
fun AppLogoWithText(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        AppLogo(size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))
        // Placeholder for app name
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
        )
    }
}
