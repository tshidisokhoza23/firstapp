package com.tpsk.tj.home.ui.components

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
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.ui.navigation.Screen

data class ShortcutItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
    val isEnabled: Boolean = true
)

@Composable
fun MoreShortcutsSheet(
    isVisible: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val shortcuts = java.util.Arrays.asList(
        ShortcutItem("Wallet", Icons.Default.AccountBalanceWallet, Color(0xFFFFA000), Screen.Wallet.route, isEnabled = true),
        ShortcutItem("Vocab", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF2196F3), Screen.Vocab.route, isEnabled = true),
        ShortcutItem("Stream", Icons.Default.MusicNote, Color(0xFF9C27B0), Screen.Stream.route, isEnabled = false),
        ShortcutItem("Ride", Icons.Default.DirectionsCar, Color(0xFF00BCD4), Screen.Ride.route, isEnabled = false)
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
                    .padding(bottom = 16.dp + bottomPadding), // Gap above bottom nav
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val itemColor = if (item.isEnabled) item.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            if (item.isEnabled) {
                onClick()
            } else {
                android.widget.Toast.makeText(context, "${item.label} is coming soon! 🚀", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(itemColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = itemColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (item.isEnabled) item.label else "${item.label} (Soon)",
            color = if (item.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
