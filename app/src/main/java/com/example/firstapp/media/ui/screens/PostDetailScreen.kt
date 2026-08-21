@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.firstapp.media.ui.screens

import androidx.compose.animation.animateContentSize
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
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.model.User
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.media.model.*
import com.example.firstapp.media.ui.components.*
import com.example.firstapp.common.ui.components.DynamicAdCard
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.util.formatTwitterTime
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
    onMediaClick: (String, String, Long) -> Unit,
    onReplyDetailClick: (String) -> Unit = {}
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
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (uiState.errorMessage != null) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading thread...", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                }
                
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
                title = { Text("Thread", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center).size(18.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { replyingToPost = post },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text("Post your reply", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 1. ANCESTORS (If any)
            post.originalPost?.let { parent ->
                item {
                    VerticalFeedPostCard(
                        post = parent,
                        isOwnPost = parent.authorId == ownProfileId,
                        showThreadLine = true,
                        showLineAbove = false, 
                        showLineBelow = true,
                        onLikeClick = { mediaViewModel.likePost(parent.id, parent.likes, parent.isLiked) },
                        onCommentClick = { replyingToPost = parent },
                        onRepostClick = { mediaViewModel.repostPost(parent.id) },
                        onShareClick = { com.example.firstapp.common.util.ShareUtils.sharePost(com.example.firstapp.common.util.ContextProvider.context, parent) },
                        onFollowClick = { },
                        onMoreClick = { },
                        onProfileClick = onProfileClick,
                        onMediaClick = onMediaClick,
                        onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
                        modifier = Modifier.clickable { onReplyDetailClick(parent.id) }
                    )
                }
            }

            // 2. MAIN SUBJECT POST (Aligned avatar, full width content)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Header Row: Align avatar with the sidebar center of ancestors/replies
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Avatar column matches PostSideBar width (64dp)
                        PostSideBar(
                            avatarUrl = post.authorAvatarUrl,
                            avatarColor = post.authorAvatarColor,
                            depth = 0,
                            showLineAbove = post.originalPost != null,
                            showLineBelow = post.replies.isNotEmpty(),
                            onProfileClick = { onProfileClick(post.authorId) }
                        )
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = if (post.originalPost != null) 12.dp else 12.dp, end = 16.dp)
                                .clickable { onProfileClick(post.authorId) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = post.authorName, 
                                    color = MaterialTheme.colorScheme.onBackground, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 16.sp
                                )
                                if (post.isVerifiedAuthor) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, null, tint = TwitterBlue, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "@${post.authorHandle}", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 14.sp
                            )
                        }

                        if (post.authorId != ownProfileId) {
                            Button(
                                onClick = { /* Follow */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.padding(top = 12.dp, end = 16.dp).height(32.dp)
                            ) {
                                Text("Follow", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Content Text (Full width, not indented relative to avatar row)
                    Text(
                        text = post.content, 
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 19.sp, 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        lineHeight = 24.sp
                    )
                    
                    // Media Section
                    if (post.mediaUrls.isNotEmpty()) {
                        val mType = post.mediaType?.lowercase(Locale.ROOT) ?: ""
                        val isVideo = mType == "video" || mType == "short"
                        
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            if (isVideo && post.mediaUrls.size == 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(post.aspectRatio?.toFloat() ?: 1.0f)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    VideoPlayer(
                                        videoUrl = post.mediaUrls[0],
                                        isPlaying = true,
                                        modifier = Modifier.fillMaxSize(),
                                        useController = true,
                                        onPositionUpdate = { currentPlaybackPosition = it }
                                    )
                                }
                            } else {
                                MediaGrid(post.mediaUrls) { url ->
                                    val type = if (com.example.firstapp.common.util.MediaUtils.isVideo(url)) "video" else "image"
                                    onMediaClick(url, type, 0L)
                                }
                            }
                        }
                    }
                    
                    // Timestamp & Views
                    Text(
                        text = formatTwitterTime(post.timestamp) + " · ${post.views} Views", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp, 
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Interaction Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 12.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InteractionIcon(Icons.Default.ChatBubbleOutline, post.repliesCount, MaterialTheme.colorScheme.onSurfaceVariant) { 
                            replyingToPost = post 
                        }
                        Spacer(Modifier.weight(1f))
                        InteractionIcon(Icons.Default.Repeat, post.shares, if (post.isReposted) Color(0xFF00BA7C) else MaterialTheme.colorScheme.onSurfaceVariant) { 
                            showRepostOptions = true 
                        }
                        Spacer(Modifier.weight(1f))
                        InteractionIcon(if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, post.likes, if (post.isLiked) Color(0xFFF91880) else MaterialTheme.colorScheme.onSurfaceVariant) { 
                            mediaViewModel.likePost(post.id, post.likes, post.isLiked) 
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { com.example.firstapp.common.util.ShareUtils.sharePost(com.example.firstapp.common.util.ContextProvider.context, post) }) {
                            Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
            
            // 3. REPLIES
            itemsIndexed(post.replies, key = { index, r -> "${r.id}_$index" }) { index, reply ->
                ThreadedReplyItem(
                    adManager = adManager,
                    post = reply,
                    ownProfileId = ownProfileId,
                    isLastInParent = index == post.replies.size - 1,
                    onPostClick = onReplyDetailClick,
                    onReplyClick = { p: Post, r: Post? -> 
                        replyingToPost = p
                        replyingToReply = r
                    },
                    onProfileClick = onProfileClick,
                    onMediaClick = onMediaClick,
                    onLike = { id: String, likes: Long, liked: Boolean -> mediaViewModel.likePost(id, likes, liked) },
                    onRepostClick = { p: Post -> mediaViewModel.repostPost(p.id) },
                    onVotePoll = { pid: String, oid: String -> mediaViewModel.votePoll(pid, oid) },
                    onShareClick = { p: Post -> com.example.firstapp.common.util.ShareUtils.sharePost(com.example.firstapp.common.util.ContextProvider.context, p) }
                )
            }
        }
    }

    if (showRepostOptions) {
        ModalBottomSheet(
            onDismissRequest = { showRepostOptions = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text(if (post.isReposted) "Undo Repost" else "Repost", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Default.Repeat, null, tint = if (post.isReposted) Color(0xFF00BA7C) else MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.clickable {
                        mediaViewModel.repostPost(post.id)
                        showRepostOptions = false
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Quote Post", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) },
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

/**
 * A threaded reply component that supports recursive nesting and expansion.
 */
@Composable
fun ThreadedReplyItem(
    adManager: AdManager,
    post: Post,
    ownProfileId: String,
    isLastInParent: Boolean,
    depth: Int = 0,
    onPostClick: (String) -> Unit, // Callback for navigation
    onReplyClick: (Post, Post?) -> Unit,
    onProfileClick: (String) -> Unit,
    onMediaClick: (String, String, Long) -> Unit,
    onLike: (String, Long, Boolean) -> Unit,
    onRepostClick: (Post) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onShareClick: (Post) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        VerticalFeedPostCard(
            post = post,
            isOwnPost = post.authorId == ownProfileId,
            depth = depth,
            showThreadLine = true,
            showLineAbove = true,
            showLineBelow = !isLastInParent || isExpanded || post.replies.isNotEmpty(),
            onLikeClick = { onLike(post.id, post.likes, post.isLiked) },
            onCommentClick = { onReplyClick(post, null) },
            onRepostClick = { onRepostClick(post) },
            onShareClick = { onShareClick(post) },
            onFollowClick = { },
            onMoreClick = { },
            onProfileClick = onProfileClick,
            onMediaClick = onMediaClick,
            onVotePoll = onVotePoll,
            modifier = Modifier.clickable { 
                onPostClick(post.id) 
            }
        )

        if (post.replies.isNotEmpty()) {
            if (!isExpanded) {
                // "Show replies" aligned with content column (after 64dp sidebar)
                Text(
                    text = "Show replies (${post.replies.size})",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 64.dp, top = 8.dp, bottom = 12.dp)
                        .clickable { isExpanded = true }
                )
            } else {
                post.replies.forEachIndexed { index, child ->
                    ThreadedReplyItem(
                        adManager = adManager,
                        post = child,
                        ownProfileId = ownProfileId,
                        isLastInParent = index == post.replies.size - 1,
                        depth = depth + 1,
                        onPostClick = onPostClick,
                        onReplyClick = onReplyClick,
                        onProfileClick = onProfileClick,
                        onMediaClick = onMediaClick,
                        onLike = onLike,
                        onRepostClick = onRepostClick,
                        onVotePoll = onVotePoll,
                        onShareClick = onShareClick
                    )
                }
            }
        }
    }
}
