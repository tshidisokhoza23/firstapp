package com.example.firstapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExploreScreen() {
    val genres = listOf(
        "Pop" to Color(0xFFE91E63),
        "Rock" to Color(0xFF673AB7),
        "Hip-Hop" to Color(0xFF2196F3),
        "Jazz" to Color(0xFFFF9800),
        "Classical" to Color(0xFF795548),
        "Electronic" to Color(0xFF00BCD4),
        "R&B" to Color(0xFFF44336),
        "Country" to Color(0xFF4CAF50)
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Browse all",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(genres) { (name, color) ->
                GenreCard(name, color)
            }
        }
    }
}

@Composable
fun GenreCard(name: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable { /* Filter */ }
            .padding(12.dp)
    ) {
        Text(
            text = name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
