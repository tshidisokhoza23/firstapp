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
                                    Text("Total Points", color = Color.Gray, fontSize = 14.sp)
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

                    item {
                        // Tasks List Title
                        Text(
                            "Daily Tasks",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
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
                    } else {
                        items(dash.tasks) { task ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TaskRow(
                                    task = task,
                                    dailyLimitReached = dash.ads_watched_today >= dash.max_daily_ads,
                                    cooldownSeconds = cooldownSeconds,
                                    onWatchClick = {
                                        android.util.Log.d("RewardsScreen", "Watch clicked for task: ${task.id}")
                                        if (activity != null) {
                                            viewModel.watchAd(
                                                activity,
                                                AdUnitConfig.REWARDED_POINTS_ID,
                                                task.id
                                            )
                                        } else {
                                            android.util.Log.e("RewardsScreen", "Activity is null, cannot show ad")
                                        }
                                    }
                                )
                            }
                        }
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
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFFF9800))
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
        }
    }
}
