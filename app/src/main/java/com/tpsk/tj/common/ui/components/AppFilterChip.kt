package com.tpsk.tj.common.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.tpsk.tj.common.ui.theme.LocalIsDarkTheme

val DefaultChipActiveGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFA855F7), Color(0xFF14B8A6))
)

@Composable
fun AppFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    val unselectedBg = if (isDark) Color(0xFF242424) else Color(0xFFF2F4F8)
    val unselectedBorder = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
    val unselectedTextColor = if (isDark) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.Transparent else unselectedBg,
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else unselectedBorder),
        modifier = modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isSelected) Modifier.background(DefaultChipActiveGradient, RoundedCornerShape(16.dp))
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else unselectedTextColor
            )
        }
    }
}
