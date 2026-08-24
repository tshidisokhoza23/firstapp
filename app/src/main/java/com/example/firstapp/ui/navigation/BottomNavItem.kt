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
    object Social : BottomNavItem("social_tab", "Social", Icons.Default.Public, 1)
    object Scroll : BottomNavItem("scroll_tab", "Scroll", Icons.Default.Videocam, 2)
    object Stream : BottomNavItem("stream_tab", "Stream", Icons.Default.MusicNote, 3)
    object Ride : BottomNavItem("ride_tab", "Ride", Icons.Default.DirectionsCar, 4)
    object More : BottomNavItem("more_tab", "More", Icons.Default.MoreHoriz, 5)

    companion object {
        val items = java.util.Arrays.asList(Chat, Social, Scroll, Stream, Ride, More)
        fun fromIndex(index: Int): BottomNavItem {
            for (item in items) {
                if (item.index == index) return item
            }
            return Chat
        }
    }
}
