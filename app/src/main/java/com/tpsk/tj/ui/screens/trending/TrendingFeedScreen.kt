package com.tpsk.tj.ui.screens.trending

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingFeedScreen(
    onNavigateBack: () -> Unit,
    onTopicClick: (String) -> Unit,
    onRefresh: () -> Unit = {},
    darkTheme: Boolean = isSystemInDarkTheme(),
    viewModel: MediaViewModel = hiltViewModel()
) {
    val bgColor = if (darkTheme) Color(0xFF121417) else Color(0xFFFFFFFF)
    val textColor = if (darkTheme) Color(0xFFF3F4F6) else Color(0xFF000000)
    val dividerColor = if (darkTheme) Color(0xFF22262C) else Color(0xFFEEEEEE)

    val uiState by viewModel.uiState.collectAsState()
    val trendingTopics = uiState.trendingTopics
    LaunchedEffect(Unit) { viewModel.fetchTrendingTopics() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trending Now", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔥", fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.fetchTrendingTopics()
                        onRefresh()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (trendingTopics.isEmpty()) {
                item {
                    Text(
                        text = "Nothing is moving fast enough to trend yet.",
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp)
                    )
                }
            }
            itemsIndexed(trendingTopics.take(20)) { _, topic ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTopicClick(topic.tag) }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = topic.tag,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${formatTrendCount(topic.post_count)} posts",
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.65f)
                    )
                }
                HorizontalDivider(
                    color = dividerColor,
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

private fun formatTrendCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
