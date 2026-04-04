package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Channel
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppSecondary
import com.example.firstapp.ui.theme.AppTextPrimary
import com.example.firstapp.ui.theme.AppTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(channel: Channel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(CircleShape)
                                .background(channel.avatarColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(channel.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("${channel.followers} followers", fontSize = 12.sp, color = AppTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = AppTextPrimary,
                    navigationIconContentColor = AppTextPrimary,
                    actionIconContentColor = AppTextPrimary
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSecondary)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Follow channel", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Post Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSecondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Image Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .background(channel.lastPostImageColor)
                    ) {
                        // Download Overlay
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.BottomStart)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("83 kB", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // Text Content
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = channel.lastPostText,
                            color = AppTextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "08:12",
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // Reactions Row
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️🤍💯🖤", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3K", color = AppTextSecondary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Reply, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("478", color = AppTextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}
