package com.example.firstapp.chat.ui.rewards

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
import com.example.firstapp.chat.model.RewardTask
import com.example.firstapp.chat.ui.viewmodel.RewardUiState
import com.example.firstapp.chat.ui.viewmodel.RewardViewModel
import com.example.firstapp.common.data.ads.AdUnitConfig

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
            title = { Text("What are these rewards?", color = Color.White) },
            text = {
                Text(
                    "You can earn points by watching short video ads. " +
                            "These points can be redeemed for various in-app benefits, " +
                            "exclusive content, or wallet top-ups depending on available offers.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it", color = Color(0xFF0056D2))
                }
            },
            containerColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Pure Dark Background
    ) {
        Spacer(modifier = Modifier.height(topBarHeight))

        when (val state = uiState) {
            is RewardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0056D2))
                }
            }
            is RewardUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color.White, textAlign = TextAlign.Center)
                        Button(
                            onClick = { viewModel.fetchDashboard() },
                            modifier = Modifier.padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0056D2))
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
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Header
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
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
                                        Text("Total Points", color = Color.Gray, fontSize = 14.sp)
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { showInfoDialog = true },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${dash.total_points}",
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Today", color = Color.Gray, fontSize = 14.sp)
                                    Text(
                                        "${dash.ads_watched_today}/${dash.max_daily_ads}",
                                        color = Color.White,
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
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(availableTasks.indices.toList()) { index ->
                            val task = availableTasks[index]
                            val isNext = index == 0
                            val canWatch = isNext && cooldownSeconds <= 0 && dash.ads_watched_today < dash.max_daily_ads

                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TaskRow(
                                    task = task,
                                    dailyLimitReached = dash.ads_watched_today >= dash.max_daily_ads,
                                    cooldownSeconds = if (isNext) cooldownSeconds else 0,
                                    isLocked = !isNext,
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
                                color = Color.Gray,
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
                                Text("No tasks available right now.", color = Color.Gray)
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
        color = Color(0xFF121212),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    task.is_completed -> Icons.Default.CheckCircle
                    isLocked -> Icons.Default.Lock
                    else -> Icons.Default.PlayCircle
                }
                val iconTint = if (task.is_completed) Color(0xFF4CAF50) else Color(0xFFFF9800)
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("+${task.points} points", color = Color(0xFFFF9800), fontSize = 14.sp)
            }

            val buttonColor: Color
            val textColor: Color
            val buttonText: String
            val enabled: Boolean

            when {
                task.is_completed -> {
                    buttonColor = Color(0xFF333333)
                    textColor = Color.Gray
                    buttonText = "Done"
                    enabled = false
                }
                isLocked -> {
                    buttonColor = Color(0xFF222222)
                    textColor = Color.DarkGray
                    buttonText = "Locked"
                    enabled = false
                }
                dailyLimitReached -> {
                    buttonColor = Color(0xFF222222)
                    textColor = Color.DarkGray
                    buttonText = "Limit Reached"
                    enabled = false
                }
                cooldownSeconds > 0 -> {
                    buttonColor = Color(0xFF333333)
                    textColor = Color.Gray
                    val mins = cooldownSeconds / 60
                    val secs = cooldownSeconds % 60
                    buttonText = String.format("%02d:%02d", mins, secs)
                    enabled = false
                }
                else -> {
                    buttonColor = Color(0xFF0056D2)
                    textColor = Color.White
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
                Text("Done", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
