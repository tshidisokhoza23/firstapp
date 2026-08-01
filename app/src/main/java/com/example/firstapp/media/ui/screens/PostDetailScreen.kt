@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.firstapp.media.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.model.User
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.media.model.*
import com.example.firstapp.media.ui.components.*
import com.example.firstapp.common.ui.components.DynamicAdCard
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.music.model.Song
import java.util.Locale
import kotlin.text.*

@Composable
fun PostDetailScreen(
    postId: String,
    mediaViewModel: MediaViewModel,
    adManager: AdManager,
    ownProfileId: String,
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    onMediaClick: (String, String, Long) -> Unit
) {
    val uiState by mediaViewModel.uiState.collectAsState()
    val post = uiState.selectedPost
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }

    androidx.activity.compose.BackHandler {
        mediaViewModel.selectPost(null)
        onBack()
    }

    LaunchedEffect(postId) {
        mediaViewModel.selectPostById(postId)
        mediaViewModel.viewPost(postId)
    }

    if (post == null || post.id != postId) {
        Box(modifier = Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (uiState.errorMessage != null) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.errorMessage!!, color = Color.White, fontSize = 14.sp)
                } else {
                    CircularProgressIndicator(color = AppAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading thread...", color = Color.White, fontSize = 14.sp)
                }
                
                // Add a timeout fallback or retry button
                var showRetry by remember(uiState.errorMessage) { mutableStateOf(uiState.errorMessage != null) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(5000)
                    showRetry = true
                }
                
                if (showRetry) {
                    Button(
                        onClick = { 
                            mediaViewModel.selectPostById(postId)
                            mediaViewModel.viewPost(postId)
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        return
    }

    var showRepostOptions by remember { mutableStateOf(false) }
    var replyingToPost by remember { mutableStateOf<Post?>(null) }
    var replyingToReply by remember { mutableStateOf<Post?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thread", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                color = AppBackground,
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f))) {
                        Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center).size(18.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { replyingToPost = post },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text("Post your reply", color = SecondaryText, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(post.authorAvatarColor)
                                .clickable { onProfileClick(post.authorId) }
                        ) {
                            AsyncImage(
                                model = com.example.firstapp.common.util.MediaUtils.formatUrl(post.authorAvatarUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.graphics.painter.ColorPainter(post.authorAvatarColor)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onProfileClick(post.authorId) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(post.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (post.isVerifiedAuthor) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, null, tint = TwitterBlue, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text("@${post.authorHandle}", color = SecondaryText, fontSize = 14.sp)
                        }
                    }
                    
                    Text(
                        text = post.content, 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        modifier = Modifier.padding(top = 16.dp),
                        lineHeight = 26.sp
                    )
                    
                    if (post.mediaUrls.size > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val mType = post.mediaType?.lowercase(Locale.ROOT) ?: ""
                        val isVideo = mType == "video" || mType == "short"
                        
                        if (isVideo && post.mediaUrls.size == 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(post.aspectRatio?.toFloat() ?: 0.8f)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                VideoPlayer(
                                    videoUrl = post.mediaUrls[0],
                                    isPlaying = true,
                                    modifier = Modifier.fillMaxSize(),
                                    useController = true,
                                    onPositionUpdate = { currentPlaybackPosition = it }
                                )
                                // Add a specific area or overlay that allows going full screen without blocking controls
                                // Twitter uses a button or double tap, but let's add a small overlay icon or just make it clickable if not on controls
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .clickable { 
                                            onMediaClick(post.mediaUrls[0], "video", currentPlaybackPosition) 
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        } else {
                            MediaGrid(post.mediaUrls) { url ->
                                val type = if (com.example.firstapp.common.util.MediaUtils.isVideo(url)) "video" else "image"
                                onMediaClick(url, type, 0L)
                            }
                        }
                    }
                    
                    Text(
                        text = "15:51 · 22 Jul 26 · ${post.views} Views", 
                        color = SecondaryText, 
                        fontSize = 14.sp, 
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InteractionIcon(Icons.Default.ChatBubbleOutline, post.repliesCount, if (post.repliesLocked) Color.Gray else SecondaryText) { 
                            if (!post.repliesLocked) replyingToPost = post 
                        }
                        Spacer(Modifier.width(12.dp))
                        InteractionIcon(
                            icon = Icons.Default.Repeat, 
                            count = post.shares, 
                            color = if (post.isReposted) Color(0xFF00BA7C) else SecondaryText, 
                            onClick = { showRepostOptions = true }
                        )
                        Spacer(Modifier.width(12.dp))
                        InteractionIcon(
                            icon = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            count = post.likes, 
                            color = if (post.isLiked) Color(0xFFF91880) else SecondaryText, 
                            onClick = { mediaViewModel.likePost(post.id, post.likes, post.isLiked) }
                        )
                        Spacer(Modifier.width(12.dp))
                        InteractionIcon(
                            icon = Icons.Default.BarChart,
                            count = post.views,
                            color = SecondaryText,
                            onClick = { /* View stats */ }
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { com.example.firstapp.common.util.ShareUtils.sharePost(com.example.firstapp.common.util.ContextProvider.context, post) }) {
                            Icon(Icons.Default.Share, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(top = 16.dp))
                }
            }
            
            itemsIndexed(post.replies, key = { index, r -> "${r.id}_$index" }) { index, reply ->
                Column {
                    PostItem(
                        adManager = adManager,
                        post = reply,
                        isOwnPost = reply.authorId == ownProfileId || (reply.authorHandle.isNotEmpty() && reply.authorHandle == post.authorHandle && post.authorId == ownProfileId),
                        showThreadLine = true,
                        isLastInThread = index == post.replies.size - 1,
                        onClick = { 
                            mediaViewModel.selectPost(reply)
                            // Ideally we'd navigate to self but with new postId
                        },
                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                        onRepostClick = { p -> mediaViewModel.repostPost(p.id) },
                        onBookmark = { id -> mediaViewModel.bookmarkPost(id) },
                        onDelete = { id -> mediaViewModel.deletePost(id) },
                        onReplyClick = { p, r -> 
                            replyingToPost = p
                            replyingToReply = r
                        },
                        onProfileClick = { id -> onProfileClick(id) },
                        onPlaySong = { _, _ -> },
                        onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
                        onMediaClick = onMediaClick,
                        onShareClick = { p -> com.example.firstapp.common.util.ShareUtils.sharePost(com.example.firstapp.common.util.ContextProvider.context, p) }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }
        }
    }

    if (showRepostOptions) {
        ModalBottomSheet(
            onDismissRequest = { showRepostOptions = false },
            containerColor = AppSecondary
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text(if (post.isReposted) "Undo Repost" else "Repost", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Repeat, null, tint = if (post.isReposted) Color(0xFF00BA7C) else Color.White) },
                    modifier = Modifier.clickable {
                        mediaViewModel.repostPost(post.id)
                        showRepostOptions = false
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Quote Post", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Edit, null, tint = Color.White) },
                    modifier = Modifier.clickable {
                        showRepostOptions = false
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    val currentUser = com.example.firstapp.LocalCurrentUser.current

    if (replyingToPost != null) {
        val rootId = replyingToPost!!.rootPostId ?: replyingToPost!!.id
        PostComposerDialog(
            replyingTo = replyingToReply ?: replyingToPost,
            avatarUrl = com.example.firstapp.common.util.MediaUtils.formatUrl(currentUser.avatarUrl),
            onDismiss = { 
                replyingToPost = null
                replyingToReply = null
            },
            onSubmit = { content, media, tags, type, poll, loc ->
                mediaViewModel.createPost(
                    content = content,
                    mediaUris = media,
                    postType = type ?: "reply",
                    tags = tags,
                    pollOptions = poll,
                    location = loc,
                    parentPostId = replyingToReply?.id ?: replyingToPost?.id,
                    rootPostId = rootId
                )
                replyingToPost = null
                replyingToReply = null
            }
        )
    }
}
