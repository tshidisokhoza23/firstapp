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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingFeedScreen(
    onNavigateBack: () -> Unit,
    onTopicClick: (String) -> Unit,
    onRefresh: () -> Unit = {},
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val bgColor = if (darkTheme) Color(0xFF121417) else Color(0xFFFFFFFF)
    val textColor = if (darkTheme) Color(0xFFF3F4F6) else Color(0xFF000000)
    val dividerColor = if (darkTheme) Color(0xFF22262C) else Color(0xFFEEEEEE)

    // Dynamic Top 20 Trending Dataset
    val trendingTopics = remember {
        listOf(
            "The ConCourt",
            "Edwin Sifuna",
            "Fikile Mbalula",
            "Julius Malema",
            "#HuntersICED",
            "#ChillersLager",
            "#Sibiya",
            "LGE 2026",
            "#MissThembisa",
            "#FridayFeeling",
            "Bafana Bafana",
            "#AmapianoAwards",
            "Kabelo",
            "Stage 2",
            "Midrand N1",
            "#SowetoDerby",
            "SARS",
            "Gold Reef",
            "#TaxSeason",
            "Uncle Waffles"
        )
    }

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
                    IconButton(onClick = onRefresh) {
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
            itemsIndexed(trendingTopics.take(20)) { _, topic ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTopicClick(topic) }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = topic,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
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
