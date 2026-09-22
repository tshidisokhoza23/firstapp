package com.tpsk.tj.chat.ui.rewards

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.chat.model.RewardTask
import com.tpsk.tj.chat.ui.viewmodel.RewardUiState
import com.tpsk.tj.chat.ui.viewmodel.RewardViewModel
import com.tpsk.tj.common.data.ads.AdUnitConfig

@Composable
fun RewardsScreen(
    viewModel: RewardViewModel,
    topBarHeight: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val cooldownSeconds by viewModel.cooldownSeconds.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("What are these rewards?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "You can earn points by watching short video ads. " +
                            "These points can be redeemed for various in-app benefits, " +
                            "exclusive content, or wallet top-ups depending on available offers.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(topBarHeight))

        when (val state = uiState) {
            is RewardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is RewardUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                        Button(
                            onClick = { viewModel.fetchDashboard() },
                            modifier = Modifier.padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is RewardUiState.Success -> {
                val dash = state.dashboard
                val (completedTasks, availableTasks) = dash.tasks.partition { it.is_completed }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Header
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Total Points", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { showInfoDialog = true },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Stars,
                                            contentDescription = "Coin",
                                            tint = com.tpsk.tj.common.ui.theme.NiceOrange,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val pointsText = if (dash.total_points % 1.0 == 0.0) {
                                            dash.total_points.toInt().toString()
                                        } else {
                                            String.format(java.util.Locale.US, "%.2f", dash.total_points)
                                        }
                                        Text(
                                            pointsText,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    Text(
                                        if (dash.max_daily_ads == 0) "${dash.ads_watched_today}" else "${dash.ads_watched_today}/${dash.max_daily_ads}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (availableTasks.isNotEmpty()) {
                        item {
                            Text(
                                "Daily Tasks",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(availableTasks.indices.toList()) { index ->
                            val task = availableTasks[index]
                            val canWatch = cooldownSeconds <= 0 && (dash.max_daily_ads == 0 || dash.ads_watched_today < dash.max_daily_ads)

                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TaskRow(
                                    task = task,
                                    dailyLimitReached = dash.max_daily_ads > 0 && dash.ads_watched_today >= dash.max_daily_ads,
                                    cooldownSeconds = cooldownSeconds,
                                    isLocked = false,
                                    onWatchClick = {
                                        if (canWatch) {
                                            android.util.Log.d("RewardsScreen", "Watch clicked for task: ${task.id}")
                                            if (activity != null) {
                                                viewModel.watchAd(
                                                    activity,
                                                    AdUnitConfig.REWARDED_POINTS_ID,
                                                    task.id
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (completedTasks.isNotEmpty()) {
                        item {
                            Text(
                                "Watched",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(completedTasks) { task ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TaskRow(
                                    task = task,
                                    dailyLimitReached = false,
                                    cooldownSeconds = 0,
                                    isLocked = false,
                                    onWatchClick = {}
                                )
                            }
                        }
                    }

                    if (dash.tasks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No tasks available right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        // Extra scroll space at the bottom (Principal Requirement)
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(
    task: RewardTask,
    dailyLimitReached: Boolean,
    cooldownSeconds: Int,
    isLocked: Boolean = false,
    onWatchClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    task.is_completed -> Icons.Default.CheckCircle
                    isLocked -> Icons.Default.Lock
                    else -> Icons.Default.PlayCircle
                }
                val iconTint = if (task.is_completed) Color(0xFF4CAF50) else com.tpsk.tj.common.ui.theme.NiceOrange
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                val pointsText = if (task.points % 1.0 == 0.0) {
                    task.points.toInt().toString()
                } else {
                    String.format(java.util.Locale.US, "%.2f", task.points)
                }
                Text("+$pointsText points", color = com.tpsk.tj.common.ui.theme.NiceOrange, fontSize = 14.sp)
            }

            val buttonColor: Color
            val textColor: Color
            val buttonText: String
            val enabled: Boolean

            when {
                task.is_completed -> {
                    buttonColor = MaterialTheme.colorScheme.surface
                    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    buttonText = "Done"
                    enabled = false
                }
                isLocked -> {
                    buttonColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    buttonText = "Locked"
                    enabled = false
                }
                dailyLimitReached -> {
                    buttonColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    buttonText = "Limit Reached"
                    enabled = false
                }
                cooldownSeconds > 0 -> {
                    buttonColor = MaterialTheme.colorScheme.surface
                    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    val mins = cooldownSeconds / 60
                    val secs = cooldownSeconds % 60
                    buttonText = String.format("%02d:%02d", mins, secs)
                    enabled = false
                }
                else -> {
                    buttonColor = MaterialTheme.colorScheme.primary
                    textColor = MaterialTheme.colorScheme.onPrimary
                    buttonText = "Watch"
                    enabled = true
                }
            }

            if (!task.is_completed && !isLocked) {
                Button(
                    onClick = onWatchClick,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        disabledContainerColor = buttonColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(buttonText, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else if (task.is_completed) {
                Text("Done", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
