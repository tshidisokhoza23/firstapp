package com.example.firstapp.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppPrimary

@Composable
fun AppLogo(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(AppPrimary, AppAccent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "App Logo",
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
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
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        )
    }
}
