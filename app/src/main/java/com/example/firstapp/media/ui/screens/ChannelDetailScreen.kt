package com.example.firstapp.media.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import com.example.firstapp.media.model.Channel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextPrimary
import com.example.firstapp.common.ui.theme.AppTextSecondary
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.FloatingActionButton
import com.example.firstapp.common.ui.theme.AppAccent

import com.example.firstapp.media.model.Post
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    channel: Channel,
    posts: List<Post>,
    onBack: () -> Unit,
    onSubscribe: (String) -> Unit = {},
    onMute: (String, Boolean) -> Unit = { _, _ -> },
    onCreatePost: () -> Unit = {},
    onAddAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    var isSubscribed by remember { mutableStateOf(channel.isSubscribed) }
    var isMuted by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(channel.avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(channel.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(channel.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                                if (channel.isAdmin) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.AdminPanelSettings, null, tint = AppAccent, modifier = Modifier.size(16.dp))
                                }
                            }
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
                    IconButton(onClick = { /* Share */ }) { Icon(Icons.Default.Share, null) }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(AppSecondary)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(context, "Channel refreshed", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications", color = Color.White) },
                                onClick = { 
                                    showMenu = false 
                                    isMuted = !isMuted
                                    onMute(channel.id, isMuted)
                                }
                            )
                            if (channel.isAdmin) {
                                HorizontalDivider(color = Color.White.copy(0.1f))
                                DropdownMenuItem(
                                    text = { Text("Add Admin", color = Color.White) },
                                    onClick = { 
                                        showMenu = false
                                        onAddAdmin()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Add, null, tint = Color.White) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (channel.isAdmin) {
                FloatingActionButton(
                    onClick = onCreatePost,
                    containerColor = AppAccent,
                    contentColor = AppBackground
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post to Channel")
                }
            }
        },
        bottomBar = {
            if (!channel.isSubscribed && !channel.isAdmin) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppSecondary,
                    tonalElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                isSubscribed = true
                                onSubscribe(channel.id)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Follow channel", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Channel Info Header
                Text(
                    text = "About this channel",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = channel.description.takeIf { it.isNotBlank() } ?: "Daily updates and news from ${channel.name}.",
                    color = AppTextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(24.dp))

                // Recent Updates Title
                Text(
                    text = "Recent updates",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (posts.isEmpty()) {
                item {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PermMedia, 
                                null, 
                                tint = Color.White.copy(alpha = 0.1f), 
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No updates yet", 
                                color = AppTextSecondary, 
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (channel.isAdmin) {
                                Text(
                                    "Share something with your followers", 
                                    color = AppTextSecondary.copy(alpha = 0.6f), 
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    ChannelPostItem(post)
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ChannelPostItem(post: Post) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSecondary),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            // Post Media
            if (post.mediaUrls.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color.DarkGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = post.mediaUrls.first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // Text Content
            if (post.content.isNotBlank()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = post.content,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("❤️ ${post.likes}", color = AppTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("💬 ${post.repliesCount}", color = AppTextSecondary, fontSize = 12.sp)
                        }
                        Text(
                            text = post.timestamp,
                            color = AppTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
