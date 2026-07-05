package com.example.firstapp.home.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.ui.navigation.Screen

data class ShortcutItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@Composable
fun MoreShortcutsSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val shortcuts = java.util.Arrays.asList(
        ShortcutItem("Wallet", Icons.Default.AccountBalanceWallet, Color(0xFFFFA000), Screen.Wallet.route),
        ShortcutItem("Updates", Icons.Default.History, Color(0xFF4CAF50), Screen.Main.route + "?tab=media"),
        ShortcutItem("Vocab", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF2196F3), Screen.Vocab.route),
        ShortcutItem("Files", Icons.Default.Folder, Color(0xFFFFC107), Screen.Files.route),
        ShortcutItem("Calendar", Icons.Default.CalendarToday, Color(0xFFE91E63), Screen.Calendar.route),
        ShortcutItem("Direct Mes...", Icons.AutoMirrored.Filled.Chat, Color(0xFF00BCD4), Screen.DirectMessages.route),
        ShortcutItem("Archived", Icons.Default.Archive, Color(0xFF8BC34A), Screen.ArchivedChats.route),
        ShortcutItem("Post Job", Icons.Default.AddBusiness, Color(0xFFFF5722), Screen.PostJob.route),
        ShortcutItem("Job Profile", Icons.Default.Work, Color(0xFF795548), Screen.JobProfile.route),
        ShortcutItem("Applicatio...", Icons.AutoMirrored.Filled.Assignment, Color(0xFF2196F3), Screen.Applications.route)
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) { }
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp), // Gap above bottom nav
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1E232C)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shortcuts",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(shortcuts) { item ->
                            ShortcutCell(item = item) {
                                onNavigate(item.route)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutCell(item: ShortcutItem, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(item.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = item.color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = item.label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
