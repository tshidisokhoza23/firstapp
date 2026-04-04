package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Post
import com.example.firstapp.model.PostReply
import com.example.firstapp.model.PostType
import com.example.firstapp.ui.theme.*

@Composable
fun MediaScreen(posts: List<Post>) {
    var showProfile by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    if (selectedPost != null) {
        PostDetailScreen(post = selectedPost!!, onBack = { selectedPost = null })
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {},
                    containerColor = AppAccent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post")
                }
            },
            containerColor = AppBackground
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(posts) { post ->
                    PostItem(post, onClick = { selectedPost = post })
                    HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Avatar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(post.authorAvatarColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.authorName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${post.authorHandle} · ${post.timestamp}",
                    color = AppTextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = AppTextSecondary)
            }

            Text(
                text = post.content,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp),
                lineHeight = 20.sp
            )

            // Media Preview
            if (post.type != PostType.TEXT_ONLY && post.mediaPlaceholderColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(post.mediaPlaceholderColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.type == PostType.VIDEO_WITH_TEXT) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PostAction(Icons.Outlined.ChatBubbleOutline, post.repliesCount)
                PostAction(Icons.Outlined.Repeat, post.shares)
                PostAction(Icons.Outlined.FavoriteBorder, post.likes)
                PostAction(Icons.Outlined.BarChart, post.views)
                Icon(Icons.Outlined.Share, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PostAction(icon: ImageVector, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(count, color = AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun PostDetailScreen(post: Post, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Post", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                PostItem(post)
                HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
            }
            
            items(post.replies) { reply ->
                ReplyItem(reply)
                HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ReplyItem(reply: PostReply, depth: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (16 + (depth * 20)).dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(reply.authorAvatarColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${reply.authorHandle} · ${reply.timestamp}", color = AppTextSecondary, fontSize = 12.sp)
            }
            Text(reply.content, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Outlined.FavoriteBorder, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(reply.likes, color = AppTextSecondary, fontSize = 12.sp)
            }

            // Nested Replies
            if (reply.nestedReplies.isNotEmpty()) {
                reply.nestedReplies.forEach { nested ->
                    ReplyItem(nested, depth + 1)
                }
            }
        }
    }
}

@Composable
fun ProfileView(onBack: () -> Unit, posts: List<Post>) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tshidiso", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${posts.size} Posts", color = AppTextSecondary, fontSize = 12.sp)
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                // Header Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF673AB7), Color(0xFF9C27B0))))
                )

                // Profile Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .offset(y = (-40).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF673AB7))
                                .padding(4.dp)
                                .background(AppBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF673AB7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("T", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Edit profile", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Tshidiso", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("@tshidiso_dev", color = AppTextSecondary, fontSize = 14.sp)
                    
                    Text(
                        "Fullstack Developer | Building the future with FirstApp 🚀 | Android & Cloud Enthusiast",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProfileStat("1,234", "Following")
                        ProfileStat("5,678", "Followers")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tabs
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Posts",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    HorizontalDivider(color = AppAccent, thickness = 3.dp, modifier = Modifier.width(50.dp))
                }
            }

            items(posts) { post ->
                PostItem(post)
                HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Row {
        Text(count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = AppTextSecondary, fontSize = 14.sp)
    }
}
