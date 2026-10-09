package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.media.model.Category

@Composable
fun CategoryChipBar(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    selectedQualityMode: String = "balanced",
    onQualityModeSelected: (String) -> Unit = {}
) {
    var showQualityMenu by remember { mutableStateOf(false) }

    val qualityOptions = listOf(
        "off" to "Off",
        "balanced" to "Balanced (default)",
        "high" to "High quality only",
        "following" to "Following only",
        "topics" to "Topics I follow",
        "calm" to "Anti-ragebait filter"
    )
    val qualityLabel = qualityOptions.firstOrNull { it.first == selectedQualityMode }?.second ?: "Balanced (default)"

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))
    )

    val defaultCategories = remember(categories) {
        if (categories.isNotEmpty()) categories
        else listOf(
            Category("all", "All"),
            Category("technology", "Technology"),
            Category("music", "Music"),
            Category("gaming", "Gaming")
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Category Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(defaultCategories) { category ->
                val isSelected = (selectedCategoryId ?: "all") == category.id
                Surface(
                    onClick = { onCategorySelected(if (category.id == "all") null else category.id) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color.Transparent else Color(0xFF242424),
                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = if (isSelected) Modifier.background(gradientBrush, RoundedCornerShape(20.dp)) else Modifier
                ) {
                    Text(
                        text = category.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF00E5FF),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Quality Mode Dropdown Chip Row
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Surface(
                onClick = { showQualityMenu = true },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF242424),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Quality Mode",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quality: $qualityLabel ▾",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            DropdownMenu(
                expanded = showQualityMenu,
                onDismissRequest = { showQualityMenu = false },
                modifier = Modifier.background(Color(0xFF242424))
            ) {
                qualityOptions.forEach { (key, option) ->
                    val isChecked = selectedQualityMode == key
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isChecked) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(option, color = if (isChecked) Color(0xFF00E5FF) else Color.White, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            onQualityModeSelected(key)
                            showQualityMenu = false
                        }
                    )
                }
            }
        }
    }
}
