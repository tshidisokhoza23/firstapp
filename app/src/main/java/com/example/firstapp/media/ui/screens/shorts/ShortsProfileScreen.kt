package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.common.model.User
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import java.util.*
import kotlin.collections.*
import kotlin.text.*
import kotlin.ranges.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsProfileScreen(
    userId: String,
    shortsViewModel: ShortsViewModel,
    userViewModel: UserViewModel,
    mediaViewModel: MediaViewModel,
    onBack: () -> Unit,
    onShortClick: (ShortMedia) -> Unit,
    onMessageClick: (String) -> Unit,
) {
    var user by remember { mutableStateOf<User?>(null) }
    val userShortsState by shortsViewModel.shorts.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    var localFollowed by remember(user) { mutableStateOf(false) }

    LaunchedEffect(userId) {
        userViewModel.getUserById(userId).collect { resource ->
            if (resource is Resource.Success) {
                user = resource.data
            }
        }
        shortsViewModel.getUserShorts(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.NotificationsNone, null, tint = Color.White) }
                    IconButton(onClick = { }) { 
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply, 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        ) 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        val shorts = userShortsState.data ?: emptyList<ShortMedia>()
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Profile Info
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = AppSecondary
                ) {
                    val avatar = user?.avatarUrl
                    if (!avatar.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val dName = user?.displayName ?: "Creator"
                Text(
                    text = "$dName 🇿🇦",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val uName = user?.username ?: "username"
                Text(
                    text = "@$uName",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val following = user?.followingCount?.toString() ?: "0"
                    StatItem(following, "Following")
                    val followers = user?.followingCount?.toLong() ?: 0L
                    StatItem(formatCount(followers), "Followers")
                    StatItem("875.5K", "Likes")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            mediaViewModel.followUser(userId)
                            localFollowed = !localFollowed
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (localFollowed) AppSecondary else Color(0xFFFE2C55)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (localFollowed) "Following" else "Follow",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Button(
                        onClick = { onMessageClick(userId) },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppSecondary),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Surface(
                        onClick = { },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = AppSecondary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bio
                val bio = user?.bio ?: "No bio yet"
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = bio,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "please subscribe ❤️",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Links
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color(0xFFFE2C55), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$dName's board", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                        Icon(Icons.Default.Stars, null, tint = Color(0xFFFE2C55), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Subscription", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tabs
                Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    TabItem(Icons.Default.GridOn, selectedTab == 0) { selectedTab = 0 }
                    TabItem(Icons.Default.PlayArrow, selectedTab == 1) { selectedTab = 1 }
                    TabItem(Icons.Default.Repeat, selectedTab == 2) { selectedTab = 2 }
                }
            }

            // Grid items
            if (userShortsState is Resource.Loading && shorts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppAccent)
                    }
                }
            } else {
                val gridRows = shorts.chunked(3)
                
                items(gridRows.size) { rowIndex ->
                    val row = gridRows[rowIndex]
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (short in row) {
                            Box(modifier = Modifier.weight(1f).padding(1.dp)) {
                                ShortGridItem(short, onShortClick)
                            }
                        }
                        // Fill empty spaces in the last row
                        val remaining = 3 - row.size
                        for (i in 0 until remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
fun RowScope.TabItem(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() }
            .drawBehind {
                if (isSelected) {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, 
            null, 
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ShortGridItem(short: ShortMedia, onClick: (ShortMedia) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clickable { onClick(short) }
    ) {
        val thumb = short.thumbnailUrl
        if (!thumb.isNullOrEmpty()) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray))
        }
        
        // Pinned Tag - use hashcode for logic
        if (short.id.hashCode() % 7 == 0) {
            Surface(
                modifier = Modifier.padding(4.dp),
                color = Color(0xFFFE2C55),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "Pinned",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        
        // View Count
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            val likeCountStr = short.likes
            val likeCount = try { likeCountStr.filter { it.isDigit() }.toLong() } catch(e: Exception) { 0L }
            val viewCount = likeCount * 100 
            Text(
                text = formatCount(viewCount),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> java.lang.String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> java.lang.String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
