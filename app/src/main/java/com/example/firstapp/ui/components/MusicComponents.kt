package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppTextSecondary
import com.example.firstapp.model.Song
import com.example.firstapp.ui.theme.AppTextSecondary

@Composable
fun MusicSectionHeader(title: String) {
    Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
}

@Composable
fun SongCard(song: Song, isPlaying: Boolean = false, onClick: () -> Unit) {
    Column(modifier = Modifier.width(150.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(song.albumArtColor),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = if (isPlaying) AppAccent else Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(song.artist, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun SongListItem(song: Song, isPlaying: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(song.albumArtColor),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isPlaying) AppAccent else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(song.artist, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1)
        }
        Text(song.duration, color = AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun AlbumCard(name: String, artist: String, color: Color) {
    Column {
        Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(color))
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artist, color = AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun ArtistListItem(name: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray.copy(0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PlaylistCard(name: String) {
    Box(modifier = Modifier.aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)))), contentAlignment = Alignment.Center) {
        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}