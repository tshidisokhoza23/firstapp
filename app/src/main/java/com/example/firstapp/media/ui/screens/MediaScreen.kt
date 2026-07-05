@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.firstapp.media.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.model.User
import java.io.File
import java.io.FileOutputStream
import com.example.firstapp.media.model.*
import com.example.firstapp.media.ui.components.AdvertItem
import com.example.firstapp.music.model.Song
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.formatTwitterTime
import com.example.firstapp.chat.data.CallType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.IosShare
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import androidx.paging.LoadState
import kotlinx.coroutines.flow.Flow
import com.example.firstapp.media.data.api.TrendingTopic
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.DialogProperties
import java.util.*
import kotlin.collections.*
import kotlin.io.*
import com.example.firstapp.chat.ui.components.FullScreenImagePlayer
import com.example.firstapp.chat.ui.components.FullScreenVideoPlayer
import java.util.Locale

@Composable
fun MediaScreen(
    posts: LazyPagingItems<Post>,
    selectedPost: Post?,
    openOwnProfileRequest: Int = 0,
    closeOwnProfileRequest: Int = 0,
    ownProfileId: String = "",
    ownProfileName: String = "You",
    ownProfileHandle: String = "you",
    ownProfileAvatarUrl: String? = null,
    ownProfileAvatarColor: Color = Color(0xFF3A4B57),
    onPostSelect: (Post?) -> Unit,
    onCreatePost: (content: String, mediaUrls: List<String>, tags: List<String>, postType: String?) -> Unit,
    onLikePost: (postId: String, likes: String, isLiked: Boolean) -> Unit,
    onRepostPost: (postId: String) -> Unit,
    onReplySubmit: (postId: String, parentReplyId: String?, content: String) -> Unit,
    onReplyLike: (replyId: String) -> Unit,
    onReplyRepost: (replyId: String) -> Unit,
    onBookmarkPost: (postId: String) -> Unit,
    onBookmarkReply: (replyId: String) -> Unit,
    onViewPost: (postId: String) -> Unit = {},
    onProfileViewChange: (Boolean) -> Unit = {},
    onGetUserPosts: (String) -> Flow<PagingData<Post>> = { _ -> kotlinx.coroutines.flow.flowOf() },
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onVotePoll: (postId: String, optionId: String) -> Unit = { _, _ -> },
    onFollowUser: (userId: String) -> Unit = {},
    onCallClick: (userId: String, CallType) -> Unit = { _, _ -> },
    onArtistClick: (String) -> Unit = {},
    onCreatePostClick: Int = 0,
    onRefresh: () -> Unit = {},
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            posts.refresh()
        }
    }

    var showCreatePost by remember { mutableStateOf(false) }
    var replyingToPost by remember { mutableStateOf<Post?>(null) }
    var replyingToReply by remember { mutableStateOf<PostReply?>(null) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }

    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showProfile by remember { mutableStateOf(false) }

    LaunchedEffect(openOwnProfileRequest) {
        if (openOwnProfileRequest > 0) {
            selectedUser = User(id = ownProfileId, username = ownProfileHandle, display_name = ownProfileName, avatar_url = ownProfileAvatarUrl)
            showProfile = true
            onPostSelect(null)
            onProfileViewChange(true)
        }
    }

    LaunchedEffect(onCreatePostClick) {
        if (onCreatePostClick > 0) {
            showCreatePost = true
        }
    }

    LaunchedEffect(closeOwnProfileRequest) {
        if (closeOwnProfileRequest > 0) {
            showProfile = false
            onProfileViewChange(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        androidx.activity.compose.BackHandler(
            enabled = selectedPost != null || showProfile || fullScreenImageUri != null || fullScreenVideoUri != null || showCreatePost || replyingToPost != null
        ) {
            when {
                fullScreenImageUri != null -> fullScreenImageUri = null
                fullScreenVideoUri != null -> fullScreenVideoUri = null
                showCreatePost -> showCreatePost = false
                replyingToPost != null -> {
                    replyingToPost = null
                    replyingToReply = null
                }
                selectedPost != null -> onPostSelect(null)
                showProfile -> {
                    showProfile = false
                    onProfileViewChange(false)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            when {
                selectedPost != null -> {
                    PostDetailSection(
                        post = selectedPost,
                        onBack = { onPostSelect(null) },
                        onLike = { onLikePost(it, selectedPost.likes, selectedPost.isLiked) },
                        onRepost = onRepostPost,
                        onBookmark = onBookmarkPost,
                        onReplyClick = { p, r -> 
                            replyingToPost = p
                            replyingToReply = r
                        },
                        onReplyLike = onReplyLike,
                        onReplyRepost = onReplyRepost,
                        onReplyBookmark = onBookmarkReply,
                        onPlaySong = onPlaySong,
                        onProfileClick = { user ->
                            selectedUser = user
                            showProfile = true
                        }
                    )
                }
                showProfile && selectedUser != null -> {
                    UserProfileSection(
                        user = selectedUser!!,
                        onBack = { 
                            showProfile = false
                            onProfileViewChange(false)
                        },
                        onPostClick = onPostSelect,
                        onGetUserPosts = onGetUserPosts,
                        onFollowClick = { onFollowUser(it) },
                        onCallClick = onCallClick,
                        onPlaySong = onPlaySong
                    )
                }
                else -> {
                    MainFeedSection(
                        posts = posts,
                        onPostClick = onPostSelect,
                        onLike = onLikePost,
                        onRepost = onRepostPost,
                        onBookmark = onBookmarkPost,
                        onReplyClick = { p, r -> 
                            replyingToPost = p
                            replyingToReply = r
                        },
                        onProfileClick = { user ->
                            selectedUser = user
                            showProfile = true
                            onProfileViewChange(true)
                        },
                        onPlaySong = onPlaySong,
                        onVotePoll = onVotePoll,
                        onRefresh = onRefresh
                    )
                }
            }
        }

        if (showCreatePost) {
            CreatePostDialog(
                onDismiss = { showCreatePost = false },
                onSubmit = { content, media, tags, type ->
                    onCreatePost(content, media, tags, type)
                    showCreatePost = false
                }
            )
        }

        if (replyingToPost != null) {
            ReplyDialog(
                post = replyingToPost!!,
                reply = replyingToReply,
                onDismiss = { 
                    replyingToPost = null
                    replyingToReply = null
                },
                onSubmit = { content ->
                    onReplySubmit(replyingToPost!!.id, replyingToReply?.id, content)
                    replyingToPost = null
                    replyingToReply = null
                }
            )
        }
    }
}

@Composable
fun MainFeedSection(
    posts: LazyPagingItems<Post>,
    onPostClick: (Post) -> Unit,
    onLike: (String, String, Boolean) -> Unit,
    onRepost: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onReplyClick: (Post, PostReply?) -> Unit,
    onProfileClick: (User) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    when {
        posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppPrimary)
            }
            return
        }
        posts.loadState.refresh is LoadState.Error && posts.itemCount == 0 -> {
            val error = (posts.loadState.refresh as LoadState.Error).error
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Could not load media feed",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error.localizedMessage ?: "Please check your connection and sign-in.",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    posts.retry()
                    onRefresh()
                }) {
                    Text("Retry")
                }
            }
            return
        }
        posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No posts yet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pull to refresh or try again in a moment.",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            }
            return
        }
    }

    PullToRefreshBox(
        isRefreshing = posts.loadState.refresh is LoadState.Loading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                count = posts.itemCount,
                key = posts.itemKey { it.id },
                contentType = posts.itemContentType { "post" }
            ) { index ->
                val post = posts[index]
                if (post != null) {
                    PostItem(
                        post = post,
                        onClick = { onPostClick(post) },
                        onLike = { onLike(post.id, post.likes, post.isLiked) },
                        onRepost = { onRepost(post.id) },
                        onBookmark = { onBookmark(post.id) },
                        onReplyClick = { onReplyClick(post, null) },
                        onProfileClick = { onProfileClick(post.authorToUser()) },
                        onPlaySong = onPlaySong,
                        onVotePoll = { onVotePoll(post.id, it) }
                    )
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onBookmark: () -> Unit,
    onReplyClick: () -> Unit,
    onProfileClick: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onVotePoll: (String) -> Unit
) {
    when (post.type) {
        PostType.EXTERNAL_AD_PLACEHOLDER -> {
            ExternalAdItem()
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(12.dp)
            ) {
                if (post.type == PostType.INTERNAL_AD) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Promoted", color = SecondaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(post.authorAvatarColor)
                            .clickable(onClick = onProfileClick)
                    ) {
                        if (post.authorAvatarUrl != null) {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (post.isVerifiedAuthor) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = TwitterBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "@${post.authorHandle} · ${formatTwitterTime(post.timestamp)}",
                                color = SecondaryText,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = post.content,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (post.mediaUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MediaGrid(post.mediaUrls)
                        }

                        if (post.poll != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PollView(post.poll, onVote = onVotePoll)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InteractionIcon(Icons.Outlined.ChatBubbleOutline, post.repliesCount, SecondaryText, onReplyClick)
                            InteractionIcon(Icons.Default.Repeat, post.shares, if (post.isReposted) AppPrimary else SecondaryText, onRepost)
                            InteractionIcon(
                                if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                post.likes,
                                if (post.isLiked) Color.Red else SecondaryText,
                                onLike
                            )
                            InteractionIcon(Icons.Outlined.BarChart, post.views, SecondaryText) {}
                            InteractionIcon(Icons.Outlined.IosShare, "", SecondaryText, onBookmark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExternalAdItem() {
    AndroidView(
        factory = { ctx ->
            com.google.android.gms.ads.AdView(ctx).apply {
                setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
                loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
            }
        },
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    )
}

@Composable
fun InteractionIcon(icon: ImageVector, count: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        if (count.isNotEmpty() && count != "0") {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = count, color = color, fontSize = 12.sp)
        }
    }
}

@Composable
fun MediaGrid(urls: List<String>) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.77f)
            .clip(shape)
            .border(0.5.dp, DividerColor, shape)
    ) {
        if (urls.size == 1) {
            AsyncImage(
                model = urls[0],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = urls[0],
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(2.dp))
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AsyncImage(
                        model = urls[1],
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                    if (urls.size > 2) {
                        Spacer(modifier = Modifier.height(2.dp))
                        AsyncImage(
                            model = urls[2],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PollView(poll: PostPoll, onVote: (String) -> Unit) {
    val totalVotes = poll.options.sumOf { it.voteCount }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        poll.options.forEach { option ->
            val percentage = if (totalVotes > 0) (option.voteCount.toFloat() / totalVotes) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TwitterBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onVote(option.id) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percentage)
                        .background(TwitterBlue.copy(alpha = 0.2f))
                )
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.text, color = Color.White, fontSize = 14.sp)
                    Text("${(percentage * 100).toInt()}%", color = SecondaryText, fontSize = 14.sp)
                }
            }
        }
        Text(
            "$totalVotes votes · ${poll.expiresAt}",
            color = SecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun PostDetailSection(
    post: Post,
    onBack: () -> Unit,
    onLike: (String) -> Unit,
    onRepost: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onReplyClick: (Post, PostReply?) -> Unit,
    onReplyLike: (String) -> Unit,
    onReplyRepost: (String) -> Unit,
    onReplyBookmark: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onProfileClick: (User) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(post.authorAvatarColor)
                                .clickable { onProfileClick(post.authorToUser()) }
                        ) {
                            if (post.authorAvatarUrl != null) {
                                AsyncImage(
                                    model = post.authorAvatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(post.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("@${post.authorHandle}", color = SecondaryText, fontSize = 15.sp)
                        }
                    }
                    
                    Text(
                        post.content,
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    
                    if (post.mediaUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MediaGrid(post.mediaUrls)
                    }
                    
                    Text(
                        "${post.timestamp} · ${post.views} Views",
                        color = SecondaryText,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        InteractionIcon(Icons.Outlined.ChatBubbleOutline, "", if (post.repliesLocked) Color.Gray else SecondaryText) { 
                            if (!post.repliesLocked) onReplyClick(post, null) 
                        }
                        InteractionIcon(Icons.Default.Repeat, post.shares, if (post.isReposted) AppPrimary else SecondaryText) { onRepost(post.id) }
                        InteractionIcon(if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "", if (post.isLiked) Color.Red else SecondaryText) { onLike(post.id) }
                        InteractionIcon(Icons.Outlined.IosShare, "", SecondaryText) { onBookmark(post.id) }
                    }
                    
                    if (post.repliesLocked) {
                        Text(
                            "Replies are locked for this post.",
                            color = SecondaryText,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))
                }
            }
            
            items(post.replies, key = { it.id }) { reply ->
                if (reply.type == PostType.EXTERNAL_AD_PLACEHOLDER) {
                    ExternalAdItem()
                } else {
                    ReplyItem(
                        reply = reply,
                        onLike = { onReplyLike(reply.id) },
                        onRepost = { onReplyRepost(reply.id) },
                        onBookmark = { onReplyBookmark(reply.id) },
                        onReplyClick = { onReplyClick(post, reply) },
                        onProfileClick = { onProfileClick(reply.authorToUser()) }
                    )
                }
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ReplyItem(
    reply: PostReply,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onBookmark: () -> Unit,
    onReplyClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(reply.authorAvatarColor)
                .clickable(onClick = onProfileClick)
        ) {
            if (reply.authorAvatarUrl != null) {
                AsyncImage(model = reply.authorAvatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("@${reply.authorHandle} · ${formatTwitterTime(reply.timestamp)}", color = SecondaryText, fontSize = 13.sp)
            }
            Text(reply.content, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                InteractionIcon(Icons.Outlined.ChatBubbleOutline, "", SecondaryText, onReplyClick)
                InteractionIcon(Icons.Default.Repeat, reply.likes, if (reply.isLiked) AppPrimary else SecondaryText, onRepost)
                InteractionIcon(if (reply.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, reply.likes, if (reply.isLiked) Color.Red else SecondaryText, onLike)
                InteractionIcon(Icons.Outlined.IosShare, "", SecondaryText, onBookmark)
            }
        }
    }
}

@Composable
fun UserProfileSection(
    user: User,
    onBack: () -> Unit,
    onPostClick: (Post) -> Unit,
    onGetUserPosts: (String) -> Flow<PagingData<Post>>,
    onFollowClick: (String) -> Unit,
    onCallClick: (String, CallType) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    val userPosts = remember(user.id) { onGetUserPosts(user.id) }.collectAsLazyPagingItems()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.display_name, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(SecondaryText)) {
                            if (user.avatar_url != null) {
                                AsyncImage(model = user.avatar_url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row {
                            IconButton(onClick = { onCallClick(user.id, CallType.VOICE) }) {
                                Icon(Icons.Default.Call, null, tint = Color.White)
                            }
                            IconButton(onClick = { onCallClick(user.id, CallType.VIDEO) }) {
                                Icon(Icons.Default.VideoCall, null, tint = Color.White)
                            }
                            Button(
                                onClick = { onFollowClick(user.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Follow", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Text(user.display_name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 12.dp))
                    Text("@${user.username}", color = SecondaryText, fontSize = 16.sp)
                    
                    if (user.bio != null) {
                        Text(user.bio!!, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                    
                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        Text("${user.following_count}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(" Following", color = SecondaryText)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${user.followers_count}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(" Followers", color = SecondaryText)
                    }
                    
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(top = 16.dp))
                }
            }
            
            items(
                count = userPosts.itemCount,
                key = userPosts.itemKey { it.id },
                contentType = userPosts.itemContentType { "post" }
            ) { index ->
                val post = userPosts[index]
                if (post != null) {
                    PostItem(
                        post = post,
                        onClick = { onPostClick(post) },
                        onLike = { /* handle */ },
                        onRepost = { /* handle */ },
                        onBookmark = { /* handle */ },
                        onReplyClick = { /* handle */ },
                        onProfileClick = { /* already here */ },
                        onPlaySong = onPlaySong,
                        onVotePoll = { /* handle */ }
                    )
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(onDismiss: () -> Unit, onSubmit: (String, List<String>, List<String>, String?) -> Unit) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Post") },
        text = {
            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What's happening?") }
            )
        },
        confirmButton = {
            Button(onClick = { onSubmit(content, emptyList(), emptyList(), null) }) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = SurfaceColor,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun ReplyDialog(post: Post, reply: PostReply?, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reply to @${reply?.authorHandle ?: post.authorHandle}") },
        text = {
            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Post your reply") }
            )
        },
        confirmButton = {
            Button(onClick = { onSubmit(content) }) {
                Text("Reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = SurfaceColor,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

fun Post.authorToUser() = User(
    id = authorId,
    username = authorHandle,
    display_name = authorName,
    avatar_url = authorAvatarUrl,
    is_verified = isVerifiedAuthor
)

fun PostReply.authorToUser() = User(
    id = authorId,
    username = authorHandle,
    display_name = authorName,
    avatar_url = authorAvatarUrl,
    is_verified = true 
)

fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}
