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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.*
import kotlin.collections.*
import kotlin.io.*
import com.example.firstapp.chat.ui.components.FullScreenImagePlayer
import com.example.firstapp.chat.ui.components.FullScreenVideoPlayer

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
    onCreatePost: (content: String, mediaUrls: List<String>, tags: List<String>, postType: String?, pollOptions: List<String>?, location: String?) -> Unit,
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
    refreshTrigger: Int = 0,
    onDirectMessagesClick: () -> Unit = {}
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
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Media", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(ownProfileAvatarColor)
                                            .clickable { 
                                                selectedUser = User(id = ownProfileId, username = ownProfileHandle, display_name = ownProfileName, avatar_url = ownProfileAvatarUrl)
                                                showProfile = true
                                                onProfileViewChange(true)
                                            }
                                    ) {
                                        if (ownProfileAvatarUrl != null) {
                                            AsyncImage(
                                                model = ownProfileAvatarUrl,
                                                contentDescription = "Profile",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.align(Alignment.Center).size(20.dp)
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { showCreatePost = true }) {
                                        Icon(Icons.Default.Add, contentDescription = "Create Post")
                                    }
                                    var showMenu by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false },
                                            containerColor = AppSecondary
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Direct Messages", color = Color.White) },
                                                leadingIcon = { Icon(Icons.Default.Mail, null, tint = Color.White) },
                                                onClick = { 
                                                    showMenu = false
                                                    onDirectMessagesClick()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Settings", color = Color.White) },
                                                leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color.White) },
                                                onClick = { showMenu = false }
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = AppBackground,
                                    titleContentColor = Color.White,
                                    navigationIconContentColor = Color.White,
                                    actionIconContentColor = Color.White
                                )
                            )
                        },
                        containerColor = AppBackground
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
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
            }
        }

        if (showCreatePost) {
            CreatePostDialog(
                avatarUrl = ownProfileAvatarUrl,
                avatarColor = ownProfileAvatarColor,
                onDismiss = { showCreatePost = false },
                onSubmit = { content, media, tags, type, poll, loc ->
                    onCreatePost(content, media, tags, type, poll, loc)
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
fun CreatePostDialog(
    avatarUrl: String? = null,
    avatarColor: Color = Color.Gray,
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>, List<String>, String?, List<String>?, String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedAudioUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var showPoll by remember { mutableStateOf(false) }
    var pollOptions by remember { mutableStateOf<List<String>>(listOf("", "")) }
    
    val context = LocalContext.current
    
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            val current = selectedMediaUris.toMutableList()
            uris.forEach { if (!current.contains(it)) current.add(it) }
            selectedMediaUris = current
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            val current = selectedAudioUris.toMutableList()
            uris.forEach { if (!current.contains(it)) current.add(it) }
            selectedAudioUris = current
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.7f)
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(24.dp),
                color = AppBackground,
                border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        
                        Button(
                            onClick = { 
                                val postType = when {
                                    showPoll -> "poll"
                                    selectedMediaUris.isNotEmpty() -> "media"
                                    selectedAudioUris.isNotEmpty() -> "audio"
                                    else -> "text_only"
                                }
                                val mediaList = java.util.ArrayList<String>()
                                for (u in selectedMediaUris) mediaList.add(u.toString())
                                for (u in selectedAudioUris) mediaList.add(u.toString())
                                val finalPoll = if (showPoll) pollOptions.filter { it.isNotBlank() } else null
                                onSubmit(content, mediaList, emptyList(), postType, finalPoll, selectedLocation)
                            },
                            enabled = content.isNotEmpty() || selectedMediaUris.isNotEmpty() || selectedAudioUris.isNotEmpty() || showPoll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppPrimary,
                                contentColor = Color.White,
                                disabledContainerColor = AppPrimary.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            modifier = Modifier.padding(end = 8.dp).height(36.dp)
                        ) {
                            Text("Post", fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor)
                            ) {
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            TextField(
                                value = content,
                                onValueChange = { content = it },
                                placeholder = { 
                                    Text("What's happening?", color = SecondaryText, fontSize = 18.sp) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = AppPrimary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)
                            )
                        }

                        if (selectedMediaUris.isNotEmpty() || selectedAudioUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedMediaUris) { uri ->
                                    val mimeType = context.contentResolver.getType(uri)
                                    val isVideo = mimeType != null && mimeType.startsWith("video")
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isVideo) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedMediaUris = selectedMediaUris.filter { it != uri } },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                
                                items(selectedAudioUris) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceColor)
                                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Mic, null, tint = AppPrimary, modifier = Modifier.size(32.dp))
                                            Text(
                                                text = getFileName(context, uri) ?: "Audio",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedAudioUris = selectedAudioUris.filter { it != uri } },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedLocation != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .background(AppPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = AppPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selectedLocation!!, color = AppPrimary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close, 
                                    null, 
                                    tint = AppPrimary, 
                                    modifier = Modifier.size(14.dp).clickable { selectedLocation = null }
                                )
                            }
                        }

                        if (showPoll) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                border = BorderStroke(1.dp, DividerColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    pollOptions.forEachIndexed { index, option ->
                                        OutlinedTextField(
                                            value = option,
                                            onValueChange = { newValue ->
                                                val newList = pollOptions.toMutableList()
                                                newList[index] = newValue
                                                pollOptions = newList
                                            },
                                            placeholder = { Text("Choice ${index + 1}", fontSize = 14.sp) },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = AppPrimary,
                                                unfocusedBorderColor = DividerColor
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            trailingIcon = if (pollOptions.size > 2) {
                                                {
                                                    IconButton(onClick = { 
                                                        val newList = pollOptions.toMutableList()
                                                        newList.removeAt(index)
                                                        pollOptions = newList
                                                    }) {
                                                        Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            } else null
                                        )
                                    }
                                    if (pollOptions.size < 4) {
                                        TextButton(onClick = { 
                                            val newList = pollOptions.toMutableList()
                                            newList.add("")
                                            pollOptions = newList
                                        }) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add choice")
                                        }
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerColor)
                                    TextButton(onClick = { showPoll = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                                        Text("Remove poll")
                                    }
                                }
                            }
                        }
                    }
                    
                    // Bottom Tools
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                    ) {
                        // Privacy indicator
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { /* logic */ },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Public, null, tint = AppPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Everyone can reply", color = AppPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                        
                        // Media icons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) 
                            }) { Icon(Icons.Default.Image, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                            
                            IconButton(onClick = { 
                                mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) 
                            }) { Icon(Icons.Default.Videocam, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                            
                            IconButton(onClick = { 
                                if (!showPoll) {
                                    showPoll = true
                                    selectedMediaUris = emptyList()
                                    selectedAudioUris = emptyList()
                                }
                            }) { Icon(Icons.Default.Poll, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                            
                            IconButton(onClick = {
                                mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) { Icon(Icons.Default.Gif, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                            
                            IconButton(onClick = { 
                                selectedLocation = "Cape Town, SA" // Mock for now
                            }) { Icon(Icons.Default.LocationOn, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                            
                            IconButton(onClick = { 
                                audioLauncher.launch("audio/*")
                            }) { Icon(Icons.Default.Mic, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
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

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        val path = uri.path
        if (path != null) {
            val cut = path.lastIndexOf('/')
            if (cut != -1) {
                result = path.substring(cut + 1)
            } else {
                result = path
            }
        }
    }
    return result
}
