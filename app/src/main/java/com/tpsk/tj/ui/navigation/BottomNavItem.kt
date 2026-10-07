package com.tpsk.tj.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
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
    object Jobs : BottomNavItem("jobs_tab", "Job", Icons.Default.Work, 3)
    object Mingle : BottomNavItem("mingle_tab", "Court", Icons.Outlined.AutoAwesome, 4)
    object Stream : BottomNavItem("stream_tab", "Stream", Icons.Default.MusicNote, 5)
    object Ride : BottomNavItem("ride_tab", "Ride", Icons.Default.DirectionsCar, 6)
    object More : BottomNavItem("more_tab", "More", Icons.Default.MoreHoriz, 7)

    companion object {
        val items = java.util.Arrays.asList(Chat, Social, Scroll, Jobs, More)
        val allItems = java.util.Arrays.asList(Chat, Social, Scroll, Jobs, Mingle, Stream, Ride, More)
        
        fun fromIndex(index: Int): BottomNavItem {
            for (item in allItems) {
                if (item.index == index) return item
            }
            return Chat
        }
    }
}
