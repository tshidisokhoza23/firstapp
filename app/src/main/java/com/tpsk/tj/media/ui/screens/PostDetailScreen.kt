@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tpsk.tj.media.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.media.model.*
import com.tpsk.tj.media.ui.components.*
import com.tpsk.tj.common.ui.components.DynamicAdCard
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.model.FlattenedThreadItem
import com.tpsk.tj.media.model.flattenThread
import java.util.Locale
import kotlin.text.*

@Composable
fun PostDetailScreen(
    postId: String,
    mediaViewModel: MediaViewModel,
    adManager: AdManager,
    ownProfileId: String,
    mainViewModel: com.tpsk.tj.home.ui.viewmodel.MainViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    onMediaClick: (String, String, Long) -> Unit,
    onReplyDetailClick: (String) -> Unit = {}
) {
    val uiState by mediaViewModel.uiState.collectAsState()
    val autoplayEnabled by mainViewModel.autoplayVideos.collectAsState()
    val autoplayGifsEnabled by mainViewModel.autoplayGifs.collectAsState()
    val post = uiState.selectedPost
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }

    val backgroundBrush = if (LocalIsDarkTheme.current) {
        Brush.verticalGradient(
            colors = listOf(AppBackgroundDarkTop, AppBackgroundDarkBottom)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.White, AppBackgroundLight)
        )
    }

    androidx.activity.compose.BackHandler {
        mediaViewModel.selectPost(null)
        onBack()
    }

    LaunchedEffect(postId) {
        mediaViewModel.selectPostById(postId)
        mediaViewModel.viewPost(postId)
    }

    if (post == null || post.id != postId) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush), contentAlignment = Alignment.Center) {
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

    val flattenedItems = remember(post) { flattenThread(post) }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Thread", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                ThreadComposerBar(
                    onSend = { content ->
                        mediaViewModel.createPost(
                            content = content,
                            postType = "reply",
                            parentPostId = post.id,
                            rootPostId = post.rootPostId ?: post.id
                        )
                    },
                    replyToName = post.authorName
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // MAIN THREAD (Root + Replies)
                itemsIndexed(
                    items = flattenedItems,
                    key = { _, item -> item.post.id }
                ) { index, item ->
                    val isPostByMe = item.post.authorId == ownProfileId
                    PostItem(
                        adManager = adManager,
                        post = item.post,
                        isOwnPost = isPostByMe,
                        showThreadLine = true,
                        showLineAbove = item.hasVerticalLineAbove,
                        showLineBelow = item.hasVerticalLineBelow,
                        depth = item.depth,
                        isLastInThread = item.isLastInBranch,
                        activePostId = postId,
                        autoplayEnabled = autoplayEnabled,
                        autoplayGifsEnabled = autoplayGifsEnabled,
                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                        onReplyClick = { p, _ -> replyingToPost = p },
                        onRepostClick = { mediaViewModel.repostPost(item.post.id, item.post.repostCount, item.post.isReposted) },
                        onShareClick = { 
                            mediaViewModel.sharePost(item.post.id, item.post.shares)
                            com.tpsk.tj.common.util.ShareUtils.sharePost(com.tpsk.tj.common.util.ContextProvider.context, item.post) 
                        },
                        onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
                        onProfileClick = onProfileClick,
                        onMediaClick = onMediaClick,
                        onQuotedPostClick = { quotedPost -> onReplyDetailClick(quotedPost.id) },
                        onClick = {
                            if (item.post.id != post.id) {
                                onReplyDetailClick(item.post.id)
                            }
                        }
                    )

                    // Add a divider after the Main Post (the root of the thread)
                    if (index == 0 && flattenedItems.size > 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showRepostOptions) {
        ModalBottomSheet(
            onDismissRequest = { showRepostOptions = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                ListItem(
                    headlineContent = { Text(if (post.isReposted) "Undo Repost" else "Repost", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Default.Repeat, null, tint = if (post.isReposted) Color(0xFF00BA7C) else MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.clickable {
                        mediaViewModel.repostPost(post.id, post.repostCount, post.isReposted)
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

    val currentUser = com.tpsk.tj.LocalCurrentUser.current

    if (replyingToPost != null) {
        val rootId = replyingToPost!!.rootPostId ?: replyingToPost!!.id
        PostComposerDialog(
            replyingTo = replyingToReply ?: replyingToPost,
            avatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(currentUser.avatarUrl),
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

