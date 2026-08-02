package com.example.firstapp.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.home.ui.components.ShortcutItem
import com.example.firstapp.home.ui.components.ShortcutCell

@Composable
fun MoreTabScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onAddChannel: () -> Unit
) {
    val shortcuts = java.util.Arrays.asList(
        ShortcutItem("Wallet", Icons.Default.AccountBalanceWallet, Color(0xFFFFA000), Screen.Wallet.route),
        ShortcutItem("Updates", Icons.Default.History, Color(0xFF4CAF50), Screen.Main.route + "?tab=media"),
        ShortcutItem("Vocab", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF2196F3), Screen.Vocab.route),
        ShortcutItem("Files", Icons.Default.Folder, Color(0xFFFFC107), Screen.Files.route),
        ShortcutItem("Calendar", Icons.Default.CalendarToday, Color(0xFFE91E63), Screen.Calendar.route),
        ShortcutItem("Archived", Icons.Default.Archive, Color(0xFF8BC34A), Screen.ArchivedChats.route),
        ShortcutItem("Job Profile", Icons.Default.Work, Color(0xFF795548), Screen.JobProfile.route),
        ShortcutItem("Applicatio...", Icons.AutoMirrored.Filled.Assignment, Color(0xFF2196F3), Screen.Applications.route)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "Shortcuts",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(shortcuts) { item ->
                ShortcutCell(item = item) {
                    if (item.route == "logout") {
                        onLogout()
                    } else if (item.route == "add_channel") {
                        onAddChannel()
                    } else {
                        navController.navigate(item.route)
                    }
                }
            }
        }
    }
}
