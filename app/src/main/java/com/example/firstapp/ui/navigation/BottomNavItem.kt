package com.example.firstapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val index: Int
) {
    object Chat : BottomNavItem("chat_tab", "Chat", Icons.AutoMirrored.Filled.Chat, 0)
    object Media : BottomNavItem("media_tab", "Media", Icons.Default.PermMedia, 1)
    object Shorts : BottomNavItem("shorts_tab", "Shorts", Icons.Default.PlayArrow, 2)
    object Music : BottomNavItem("music_tab", "Music", Icons.Default.MusicNote, 3)
    object Drive : BottomNavItem("drive_tab", "Drive", Icons.Default.DirectionsCar, 4)
    object More : BottomNavItem("more_tab", "More", Icons.Default.MoreHoriz, 5)

    companion object {
        val items = java.util.Arrays.asList(Chat, Media, Shorts, Music, Drive, More)
        fun fromIndex(index: Int): BottomNavItem {
            for (item in items) {
                if (item.index == index) return item
            }
            return Chat
        }
    }
}
