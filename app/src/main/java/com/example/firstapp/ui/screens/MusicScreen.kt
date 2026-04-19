package com.example.firstapp.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.model.Song
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MusicViewModel

@Composable
fun MusicScreen(
    songs: List<Song>,
    viewModel: MusicViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf("Recommendations") }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Recommendations", "Library", "Search").forEach { screen ->
                    Text(
                        text = screen,
                        color = if (currentScreen == screen) AppAccent else Color.White.copy(alpha = 0.6f),
                        fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { currentScreen = screen }
                            .padding(8.dp)
                    )
                }
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    "Recommendations" -> RecommendationsScreen(songs) { viewModel.playSong(it) }
                    "Library" -> LibraryScreen(songs) { viewModel.playSong(it) }
                    "Search" -> SearchScreen(songs) { viewModel.playSong(it) }
                }
            }
            
            // Space for the global mini player which is in MainScreen
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
