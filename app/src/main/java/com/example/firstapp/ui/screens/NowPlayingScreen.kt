package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Song
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppTextSecondary

@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    progress: Float,
    isLiked: Boolean,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit
) {
    val context = LocalContext.current
    var currentProgress by remember { mutableStateOf(progress) }

    // Update currentProgress when progress changes from outside (e.g. from ViewModel)
    LaunchedEffect(progress) {
        currentProgress = progress
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2C3E50), AppBackground))).statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            Text("Now Playing", color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out this song: ${song.title} by ${song.artist}")
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }) { Icon(Icons.Default.Share, null, tint = Color.White) }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Surface(modifier = Modifier.aspectRatio(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)), color = song.albumArtColor) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(100.dp)) }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(song.artist, color = AppTextSecondary, fontSize = 18.sp, maxLines = 1)
            }
            IconButton(onClick = onToggleLike) { Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else Color.White, modifier = Modifier.size(28.dp)) }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Slider(
            value = currentProgress,
            onValueChange = { currentProgress = it },
            onValueChangeFinished = { onSeek(currentProgress) },
            colors = SliderDefaults.colors(thumbColor = AppAccent, activeTrackColor = AppAccent)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0:00", color = AppTextSecondary, fontSize = 12.sp)
            Text(song.duration, color = AppTextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, null, tint = if (isShuffleEnabled) AppAccent else Color.White) }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            FloatingActionButton(onClick = onTogglePlay, containerColor = Color.White, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            IconButton(onClick = onToggleRepeat) { Icon(if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (repeatMode != 0) AppAccent else Color.White) }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}