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
    adsWatched: Int = 0,
    points: Double = 0.0,
    totalAdsToWatch: Int = 20,
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

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121212),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$adsWatched / $totalAdsToWatch",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Ads Watched Today",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val progressValue = if (totalAdsToWatch > 0) adsWatched.toFloat() / totalAdsToWatch.toFloat() else 0f
                    val clampedProgress = if (progressValue < 0f) 0f else if (progressValue > 1f) 1f else progressValue
                    
                    LinearProgressIndicator(
                        progress = { clampedProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = AppAccent,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    
                    if (adverts.isNotEmpty()) {
                        Button(
                            onClick = { onAdFinished(adverts[0]) },
                            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (cooldownSeconds > 0) Color.Gray else AppAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = cooldownSeconds <= 0
                        ) {
                            Text(
                                text = if (cooldownSeconds > 0) "Next Reward Ad in ${cooldownSeconds}s" else "Open Reward Ad & Earn",
                                color = if (cooldownSeconds > 0) Color.White else AppBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (adverts.isNotEmpty()) {
            item {
                Text(
                    text = "Next Available Tasks",
                    color = AppAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Only the first ad in the list is actionable
            val nextAd = adverts[0]
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF121212),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { if (cooldownSeconds <= 0) onAdFinished(nextAd) }
                ) {
                    RewardItem(
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VideoLibrary, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Text("AD", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                            }
                        },
                        title = "Watch Reward Ad # ${adsWatched + 1}",
                        coins = "0.1",
                        actionText = if (cooldownSeconds > 0) "${cooldownSeconds}s" else "Go",
                        isCompleted = false,
                        enabled = cooldownSeconds <= 0,
                        onClick = { onAdFinished(nextAd) }
                    )
                }
            }
            
            if (adverts.size > 1) {
                val upcomingAds = adverts.subList(1, adverts.size)
                items(upcomingAds) { ad ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().alpha(0.5f),
                        color = Color(0xFF121212),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        RewardItem(
                            icon = {
                                Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            },
                            title = "Upcoming Ad Task",
                            coins = "0.1",
                            isCompleted = false,
                            enabled = false,
                            onClick = {}
                        )
                    }
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
