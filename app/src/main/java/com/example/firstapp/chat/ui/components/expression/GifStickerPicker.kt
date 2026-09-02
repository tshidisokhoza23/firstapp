package com.example.firstapp.chat.ui.components.expression

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun GifStickerPicker(
    onGifSelected: (String) -> Unit,
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: GIFs, 1: Stickers
    var searchQuery by remember { mutableStateOf("") }

    // Placeholder data - in a real app, these would come from an API (Giphy, etc.)
    val gifs = remember(searchQuery) {
        listOf(
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmZ3bmZ3bmZ3/3o7TKQH09TTRBfVInK/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmZ3bmZ3bmZ3/l0HlHFRbEzpwwmbfG/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmZ3bmZ3bmZ3/3o7TKSj0D7k256nU2A/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmZ3bmZ3bmZ3/26gskAdTJJN2zU06c/giphy.gif"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search GIFs & Stickers") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gif, null)
                    Spacer(Modifier.width(8.dp))
                    Text("GIFs")
                }}
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.StickyNote2, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stickers")
                }}
            )
        }

        Spacer(Modifier.height(16.dp))

        // Content Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gifs) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable { 
                            if (selectedTab == 0) onGifSelected(url) else onStickerSelected(url)
                            onDismiss()
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
