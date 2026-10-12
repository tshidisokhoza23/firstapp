package com.tpsk.tj.common.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DefaultChipActiveGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))
)

@Composable
fun AppFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeGradient: Brush = DefaultChipActiveGradient,
    unselectedColor: Color = Color(0xFF242424),
    unselectedBorder: Color = Color(0xFF333333),
    activeTextColor: Color = Color.White,
    unselectedTextColor: Color = Color(0xFF00E5FF)
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.Transparent else unselectedColor,
        border = if (isSelected) null else BorderStroke(1.dp, unselectedBorder),
        modifier = modifier
            .height(32.dp)
            .then(if (isSelected) Modifier.background(activeGradient, RoundedCornerShape(16.dp)) else Modifier)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) activeTextColor else unselectedTextColor
            )
        }
    }
}
