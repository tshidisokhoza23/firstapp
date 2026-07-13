package com.example.firstapp.home.ui.screens

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.NiceOrange
import com.example.firstapp.common.ui.theme.FirstAppTheme
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.media.model.Advert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    adverts: List<Advert> = emptyList(),
    viewedAdverts: List<Advert> = emptyList(),
    points: Double = 0.0,
    onAdvertClick: (Advert) -> Unit = { _ -> },
    onRefresh: () -> Unit = {},
    onResetAds: () -> Unit = {}
) {
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            cooldownSeconds -= 1
        }
    }

    val onAdFinished = { ad: Advert ->
        onAdvertClick(ad)
        cooldownSeconds = 15
    }

    android.util.Log.d("RewardsScreen", "Adverts size: ${adverts.size}, viewed: ${viewedAdverts.size}")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Earn Rewards",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Stars, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = java.lang.String.format(java.util.Locale.US, "%.1f Points Available", points),
                            color = AppAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AppAccent)
                }
            }
            
            TextButton(
                onClick = onResetAds,
                modifier = Modifier.padding(top = 4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Reset Progress (Dev)", color = Color.Red.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }

        if (adverts.isNotEmpty()) {
            item {
                Text(
                    text = "Available Tasks",
                    color = AppAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(adverts) { ad ->
                val index = adverts.indexOf(ad)
                val isNext = index == 0
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .alpha(if (isNext) 1f else 0.8f),
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { if (isNext && cooldownSeconds <= 0) onAdFinished(ad) }
                ) {
                    RewardItem(
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                if (isNext) {
                                    Icon(Icons.Default.VideoLibrary, null, tint = AppAccent, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        title = if (ad.title.isNotEmpty()) ad.title else "Reward Ad #${index + 1}",
                        coins = "0.1",
                        actionText = if (isNext) (if (cooldownSeconds > 0) "${cooldownSeconds}s" else "Watch") else "Locked",
                        isCompleted = false,
                        enabled = isNext && cooldownSeconds <= 0,
                        onClick = { if (isNext) onAdFinished(ad) }
                    )
                }
            }
        } else if (viewedAdverts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Celebration, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No more ads for today!", color = Color.Gray, textAlign = TextAlign.Center)
                        Text("Come back tomorrow for more coins.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (viewedAdverts.isNotEmpty()) {
            item {
                Text(
                    text = "Viewed Tasks",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(viewedAdverts) { ad ->
                Surface(
                    modifier = Modifier.fillMaxWidth().alpha(0.6f),
                    color = Color(0xFF121212),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    RewardItem(
                        icon = {
                            Icon(Icons.Default.VideoLibrary, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                        },
                        title = if (ad.title.isNotEmpty()) ad.title else "Ad from ${ad.advertiserName}",
                        coins = "0.1",
                        isCompleted = true,
                        onClick = {}
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun RewardItem(
    icon: @Composable () -> Unit,
    title: String,
    coins: String,
    isCompleted: Boolean = false,
    actionText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(NiceOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+$coins",
                    color = NiceOrange,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (actionText != null) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) Color(0xFFF50057) else Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier.height(40.dp),
                enabled = enabled
            ) {
                Text(actionText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp, 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCompleted) Color(0xFF1E1E1E) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RewardsScreenPreview() {
    FirstAppTheme {
        RewardsScreen()
    }
}
