package com.example.firstapp.profile.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.firstapp.common.model.*
import com.example.firstapp.media.model.Post
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.common.util.MediaUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.media.ui.screens.PostItem

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.paging.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    userId: String? = null,
    isGroup: Boolean = false,
    onBack: () -> Unit,
    posts: LazyPagingItems<Post>,
    userViewModel: UserViewModel,
    mediaViewModel: MediaViewModel,
    chatViewModel: ChatViewModel = hiltViewModel(),
    sharedMediaViewModel: SharedMediaViewModel = hiltViewModel(),
    onNavigateToSharedMedia: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by userViewModel.currentUser.collectAsState()
    val isMyProfile = userId.isNullOrBlank() || userId == currentUser?.id
    
    val contacts by userViewModel.contacts.collectAsState()
    val chatsResource by chatViewModel.chats.collectAsState()
    val chat = (chatsResource.data ?: emptyList()).find { it.id == userId }
    
    val user = if (isMyProfile) currentUser else {
        val contact = contacts.find { it.id == userId }
        if (contact != null) {
            User(
                id = contact.id, 
                username = contact.username, 
                display_name = chat?.senderName ?: contact.displayName, 
                avatar_url = contact.avatarUrl, 
                bio = contact.bio, 
                location = "",
                phone_number = contact.phoneNumber
            )
        } else if (chat != null) {
            User(
                id = chat.id,
                display_name = chat.senderName,
                avatar_url = chat.avatarUrl,
                username = ""
            )
        } else null
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = MediaUtils.getFileFromUri(context, it)
            if (file != null) userViewModel.uploadProfilePicture(file, false)
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = MediaUtils.getFileFromUri(context, it)
            if (file != null) userViewModel.uploadProfilePicture(file, true)
        }
    }

    val isRefreshing = posts.loadState.refresh is LoadState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGroup) "Group Info" else if (isMyProfile) "My Profile" else "Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isMyProfile) {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { posts.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        AsyncImage(
                            model = user?.banner_url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().background(AppSecondary).clickable(enabled = isMyProfile) { bannerLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, AppBackground))))
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-50).dp)) {
                        AsyncImage(
                            model = user?.avatar_url,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(CircleShape).border(4.dp, AppBackground, CircleShape).background(AppSecondary).clickable(enabled = isMyProfile) { imageLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(user?.display_name ?: "Unknown", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("@${user?.username ?: ""}", color = AppTextSecondary, fontSize = 16.sp)
                        
                        if (!isMyProfile && user != null) {
                            Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onNavigateToChat(user.id) }, colors = ButtonDefaults.buttonColors(containerColor = AppAccent), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Message")
                                }
                                OutlinedButton(onClick = { /* Call */ }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call")
                                }
                            }
                        }

                        // Chat specific options
                        if (userId != null && !isMyProfile) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = AppSecondary.copy(alpha = 0.5f))
                            ) {
                                Column {
                                    ProfileOptionItem(Icons.Default.Image, "Media, Links, and Docs") { onNavigateToSharedMedia(userId) }
                                    ProfileOptionItem(Icons.Default.Star, "Starred Messages") { /* Navigate to Starred for this chat */ }
                                    ProfileOptionItem(
                                        if (chat?.isMuted == true) Icons.Default.VolumeUp else Icons.Default.VolumeOff, 
                                        if (chat?.isMuted == true) "Unmute Notifications" else "Mute Notifications"
                                    ) { chatViewModel.muteChat(userId, chat?.isMuted != true) }
                                }
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = AppSecondary.copy(alpha = 0.5f))
                            ) {
                                Column {
                                    ProfileOptionItem(Icons.Default.Block, "Block ${user?.display_name ?: "User"}", color = Color.Red) { }
                                    ProfileOptionItem(Icons.Default.ThumbDown, "Report ${user?.display_name ?: "User"}", color = Color.Red) { }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        if (isRefreshing) "Loading Posts..." else if (isMyProfile) "Your Posts" else "Posts",
                        modifier = Modifier.padding(16.dp),
                        color = AppAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id }
                ) { index ->
                    val post = posts[index]
                    if (post != null) {
                        PostItem(
                            post = post,
                            onClick = {},
                            onLike = { mediaViewModel.likePost(post.id, post.likes, post.isLiked) },
                            onRepost = { mediaViewModel.repostPost(post.id) },
                            onBookmark = { mediaViewModel.bookmarkPost(post.id) },
                            onReplyClick = { /* handle */ },
                            onProfileClick = {},
                            onPlaySong = { _, _ -> },
                            onVotePoll = { _ -> }
                        )
                        HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
                    }
                }

                if (posts.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileOptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = Color.White, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = color) },
        leadingContent = { Icon(icon, null, tint = if (color == Color.Red) color else AppAccent) },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
