package com.example.firstapp.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.model.User
import java.io.File
import java.io.FileOutputStream
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.model.Post
import com.example.firstapp.model.PostReply
import com.example.firstapp.model.PostType
import com.example.firstapp.model.Song
import com.example.firstapp.ui.theme.*
import com.example.firstapp.util.formatTwitterTime
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.media3.common.util.UnstableApi

@Composable
fun MediaScreen(
    posts: List<Post>,
    songs: List<Song> = emptyList(),
    mediaFeedTab: Int,
    selectedPost: Post?,
    openOwnProfileRequest: Int = 0,
    closeOwnProfileRequest: Int = 0,
    ownProfileName: String = "You",
    ownProfileHandle: String = "you",
    ownProfileAvatarColor: Color = Color(0xFF3A4B57),
    onPostSelect: (Post?) -> Unit,
    onCreatePost: (content: String, mediaUrls: List<String>) -> Unit,
    onLikePost: (postId: Int) -> Unit,
    onRepostPost: (postId: Int) -> Unit,
    onReplySubmit: (postId: Int, parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit,
    onReplyRepost: (replyId: Int) -> Unit,
    onBookmarkPost: (postId: Int) -> Unit,
    onBookmarkReply: (replyId: Int) -> Unit,
    onViewPost: (postId: Int) -> Unit = {},
    onProfileViewChange: (Boolean) -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onCreatePostClick: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCreatePost by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var replyingToPost by remember { mutableStateOf<Post?>(null) }
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Detailed View State
    var activePostForDetail by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(selectedPost) {
        if (selectedPost != null) {
            activePostForDetail = selectedPost
        }
    }

    LaunchedEffect(onCreatePostClick) {
        if (onCreatePostClick > 0) {
            showCreatePost = true
        }
    }

    LaunchedEffect(openOwnProfileRequest) {
        if (openOwnProfileRequest > 0) {
            selectedUser = null
            showProfile = true
            activePostForDetail = null
            onProfileViewChange(true)
        }
    }
    
    LaunchedEffect(closeOwnProfileRequest) {
        if (closeOwnProfileRequest > 0) {
            showProfile = false
            onProfileViewChange(false)
        }
    }

    val backHandler = {
        if (activePostForDetail != null) {
            activePostForDetail = null
            onPostSelect(null)
        } else if (showProfile) {
            showProfile = false
            onProfileViewChange(false)
        }
    }

    androidx.activity.compose.BackHandler(enabled = activePostForDetail != null || showProfile) {
        backHandler()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            if (activePostForDetail == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search media...", color = AppTextSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AppAccent
                            ),
                            leadingIcon = {
                                IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                                }
                            },
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = if (showProfile) "Profile" else when(mediaFeedTab) {
                                0 -> "My Feed"
                                1 -> "Following"
                                else -> "Trending"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, null, tint = AppTextPrimary)
                            }
                            if (!showProfile) {
                                IconButton(onClick = { showCreatePost = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Create Post", tint = AppAccent)
                                }
                            } else {
                                IconButton(onClick = { 
                                    showProfile = false
                                    onProfileViewChange(false)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppAccent)
                                }
                            }
                        }
                    }
                }
            }

            when {
                activePostForDetail != null -> {
                    PostDetailSection(
                        post = activePostForDetail!!,
                        onBack = { 
                            activePostForDetail = null
                            onPostSelect(null)
                        },
                        onLike = onLikePost,
                        onRepost = onRepostPost,
                        onBookmark = onBookmarkPost,
                        onReplyClick = { replyingToPost = it },
                        onReplyLike = onReplyLike,
                        onReplyRepost = onReplyRepost,
                        onReplyBookmark = onBookmarkReply,
                        onPlaySong = onPlaySong
                    )
                }
                showProfile -> {
                    UserProfileSection(
                        user = selectedUser,
                        ownName = ownProfileName,
                        ownHandle = ownProfileHandle,
                        ownAvatarColor = ownProfileAvatarColor,
                        posts = posts,
                        onPostClick = { activePostForDetail = it },
                        onPlaySong = onPlaySong
                    )
                }
                else -> {
                    MediaFeed(
                        posts = posts,
                        songs = songs,
                        searchQuery = searchQuery,
                        onPostSelect = { 
                            onPostSelect(it)
                            activePostForDetail = it
                            if (it != null) onViewPost(it.id)
                        },
                        onLike = onLikePost,
                        onRepost = onRepostPost,
                        onBookmark = onBookmarkPost,
                        onProfileClick = { user ->
                            selectedUser = user
                            showProfile = true
                            onProfileViewChange(true)
                        },
                        onPlaySong = onPlaySong
                    )
                }
            }
        }

        if (showCreatePost) {
            CreatePostDialog(
                onDismiss = { showCreatePost = false },
                onSubmit = { content, urls ->
                    onCreatePost(content, urls)
                    showCreatePost = false
                }
            )
        }

        replyingToPost?.let { post ->
            ReplyDialog(
                post = post,
                onDismiss = { replyingToPost = null },
                onSubmit = { content ->
                    onReplySubmit(post.id, null, content)
                    replyingToPost = null
                }
            )
        }
    }
}

@Composable
fun MediaFeed(
    posts: List<Post>,
    songs: List<Song> = emptyList(),
    searchQuery: String = "",
    onPostSelect: (Post?) -> Unit,
    onLike: (postId: Int) -> Unit,
    onRepost: (postId: Int) -> Unit,
    onBookmark: (postId: Int) -> Unit,
    onProfileClick: (User?) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> }
) {
    val filteredPosts = remember(posts, searchQuery) {
        if (searchQuery.isBlank()) posts
        else posts.filter { 
            it.content.contains(searchQuery, ignoreCase = true) || 
            it.authorName.contains(searchQuery, ignoreCase = true) ||
            it.authorHandle.contains(searchQuery, ignoreCase = true)
        }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (songs.isNotEmpty()) {
            item {
                Text(
                    "Trending Music",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(songs) { song ->
                        TrendingMusicCard(song, onClick = { onPlaySong(song, songs) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
            }
        }

        items(filteredPosts) { post ->
            PostItem(
                post = post,
                onClick = { onPostSelect(post) },
                onLike = { onLike(post.id) },
                onRepost = { onRepost(post.id) },
                onBookmark = { onBookmark(post.id) },
                onProfileClick = {
                    onProfileClick(User(id = post.authorId, username = post.authorHandle, display_name = post.authorName, avatar_url = post.authorAvatarUrl, is_verified = post.isVerifiedAuthor))
                },
                onReplyClick = { onPostSelect(post) }
            )
            HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun TrendingMusicCard(song: Song, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        color = AppSecondary.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(song.albumArtColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = AppTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PostContent(text: String, onMentionClick: (String) -> Unit = {}, onHashtagClick: (String) -> Unit = {}) {
    val annotatedString = buildAnnotatedString {
        val words = text.split(" ")
        words.forEachIndexed { index, word ->
            when {
                word.startsWith("@") && word.length > 1 -> {
                    pushStringAnnotation(tag = "mention", annotation = word.substring(1))
                    withStyle(style = SpanStyle(color = AppAccent, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                    pop()
                }
                word.startsWith("#") && word.length > 1 -> {
                    pushStringAnnotation(tag = "hashtag", annotation = word.substring(1))
                    withStyle(style = SpanStyle(color = AppAccent)) {
                        append(word)
                    }
                    pop()
                }
                else -> append(word)
            }
            if (index < words.size - 1) append(" ")
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(color = AppTextPrimary, lineHeight = 22.sp),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "mention", start = offset, end = offset)
                .firstOrNull()?.let { onMentionClick(it.item) }
            annotatedString.getStringAnnotations(tag = "hashtag", start = offset, end = offset)
                .firstOrNull()?.let { onHashtagClick(it.item) }
        }
    )
}

@UnstableApi
@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    @Suppress("DEPRECATION")
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PostPoll(options: List<String>, totalVotes: Int = 1240) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            val percentage = if (index == 0) 65 else (35 / (options.size - 0.5)).toInt()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { /* Vote */ }
            ) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage / 100f)
                        .fillMaxHeight()
                        .background(if (index == 0) AppAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(option, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("$percentage%", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
        Text("$totalVotes votes \u2022 2 hours left", color = AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun PostItem(
    post: Post,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onBookmark: () -> Unit,
    onProfileClick: () -> Unit,
    onReplyClick: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    showThreadLine: Boolean = false,
    isThreadChild: Boolean = false
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Avatar + Thread Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp).clickable { onProfileClick() },
                shape = CircleShape,
                color = post.authorAvatarColor ?: AppSecondary
            ) {
                if (post.authorAvatarUrl != null) {
                    AsyncImage(
                        model = post.authorAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }
            if (showThreadLine || post.hasThread) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Column: Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f).clickable { onProfileClick() }, verticalAlignment = Alignment.CenterVertically) {
                    Text(post.authorName, color = AppTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (post.isVerifiedAuthor) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, tint = AppAccent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("@${post.authorHandle}", color = AppTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${formatTwitterTime(post.timestamp)}", color = AppTextSecondary, fontSize = 12.sp)
                
                IconButton(onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onBookmark() 
                }, modifier = Modifier.size(24.dp).padding(start = 8.dp)) {
                    Icon(
                        if (post.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (post.isBookmarked) AppAccent else AppTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            PostContent(text = post.content)

            if (post.type == PostType.POLL) {
                PostPoll(options = listOf("Option 1", "Option 2", "Option 3"))
            }
            
            if (post.type == PostType.SONG && post.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                // Render a shared song card
                Surface(
                    onClick = { /* handle song play */ },
                    color = AppSecondary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(AppPrimary)) {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shared Song", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Original Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                    }
                }
            }
            
            if (post.mediaUrls.isNotEmpty() && post.type != PostType.SONG) {
                Spacer(modifier = Modifier.height(12.dp))
                if (post.type == PostType.VIDEO_WITH_TEXT) {
                    VideoPlayer(url = post.mediaUrls.first())
                } else if (post.mediaUrls.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                        AsyncImage(
                            model = post.mediaUrls.first(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(post.mediaUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(240.dp, 160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppSecondary.copy(alpha = 0.3f)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostStat(Icons.Default.ChatBubbleOutline, post.repliesCount, onClick = onReplyClick)
                PostStat(
                    if (post.isReposted) Icons.Default.Repeat else Icons.Default.Repeat, 
                    post.shares, 
                    color = if (post.isReposted) Color(0xFF4CAF50) else AppTextSecondary,
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onRepost()
                    }
                )
                PostStat(
                    if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    post.likes, 
                    color = if (post.isLiked) Color(0xFFF44336) else AppTextSecondary,
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onLike()
                    }
                )
                PostStat(Icons.Default.BarChart, post.views)
                IconButton(onClick = { /* Share */ }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Share, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun PostStat(icon: ImageVector, text: String, color: Color = AppTextSecondary, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        if (text.isNotEmpty() && text != "0") {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, color = color, fontSize = 12.sp)
        }
    }
}

@Composable
fun UserProfileSection(
    user: User?,
    ownName: String,
    ownHandle: String,
    ownAvatarColor: Color,
    posts: List<Post>,
    onPostClick: (Post) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> }
) {
    val displayName = user?.display_name ?: ownName
    val handle = user?.username ?: ownHandle
    val avatarColor = if (user == null) ownAvatarColor else (if (user.is_verified) AppAccent else AppSecondary)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Banner Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.verticalGradient(listOf(AppSecondary, AppBackground)))
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1557683316-973673baf926?w=800",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
        }
        
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    modifier = Modifier.size(84.dp).offset(y = (-42).dp),
                    shape = CircleShape,
                    color = avatarColor,
                    border = BorderStroke(4.dp, AppBackground)
                ) {
                    val avatarUrl = user?.avatar_url
                    if (avatarUrl != null) {
                        AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                    }
                }
                
                Button(
                    onClick = { /* Edit or Follow */ },
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (user == null) AppSecondary else AppAccent),
                    border = if (user == null) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (user == null) "Edit Profile" else "Follow", fontWeight = FontWeight.Bold)
                }
            }
            
            Column(modifier = Modifier.offset(y = (-32).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(displayName, color = AppTextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    if (user?.is_verified == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                    }
                }
                Text("@$handle", color = AppTextSecondary, fontSize = 15.sp)
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user?.bio ?: "No bio yet. Tap to add one.",
                    color = AppTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = AppTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Joined ${user?.created_at?.take(10) ?: "April 2026"}", color = AppTextSecondary, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    ProfileStat("128", "Following")
                    Spacer(modifier = Modifier.width(20.dp))
                    ProfileStat("1.2K", "Followers")
                }
            }
        }
        
        HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f), thickness = 0.5.dp)
        
        // Tabs
        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileTabItem("Posts", true, modifier = Modifier.weight(1f))
            ProfileTabItem("Replies", false, modifier = Modifier.weight(1f))
            ProfileTabItem("Media", false, modifier = Modifier.weight(1f))
            ProfileTabItem("Likes", false, modifier = Modifier.weight(1f))
        }

        val userPosts = posts.filter { it.authorHandle == handle }
        Column {
            userPosts.forEach { post ->
                PostItem(
                    post = post, 
                    onClick = { onPostClick(post) }, 
                    onLike = {}, 
                    onRepost = {}, 
                    onBookmark = {}, 
                    onProfileClick = {},
                    onPlaySong = { /* handle find song if needed */ }
                )
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.3f))
            }
            if (userPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No posts yet", color = AppTextSecondary)
                }
            }
        }
    }
}

@Composable
fun ProfileTabItem(label: String, isSelected: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable { },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp),
            color = if (isSelected) Color.White else AppTextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp
        )
        if (isSelected) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(AppAccent, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
        }
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Row {
        Text(count, color = AppTextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = AppTextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailSection(
    post: Post,
    onBack: () -> Unit,
    onLike: (Int) -> Unit,
    onRepost: (Int) -> Unit,
    onBookmark: (Int) -> Unit,
    onReplyClick: (Post) -> Unit,
    onReplyLike: (Int) -> Unit,
    onReplyRepost: (Int) -> Unit,
    onReplyBookmark: (Int) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> }
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Post", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                PostItem(
                    post = post,
                    onClick = {},
                    onLike = { onLike(post.id) },
                    onRepost = { onRepost(post.id) },
                    onBookmark = { onBookmark(post.id) },
                    onProfileClick = {},
                    onReplyClick = { onReplyClick(post) },
                    onPlaySong = { /* handle find song if needed */ },
                    showThreadLine = post.replies.isNotEmpty()
                )
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f), thickness = 8.dp)
            }

            if (post.replies.isNotEmpty()) {
                item {
                    Text(
                        "Replies",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                items(post.replies) { reply ->
                    PostReplyItem(
                        reply = reply,
                        onLike = { onReplyLike(reply.id) },
                        onRepost = { onReplyRepost(reply.id) },
                        onBookmark = { onReplyBookmark(reply.id) }
                    )
                    HorizontalDivider(color = AppSecondary.copy(alpha = 0.3f), modifier = Modifier.padding(start = 72.dp))
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No replies yet. Be the first to reply!", color = AppTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun PostReplyItem(
    reply: PostReply,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onBookmark: () -> Unit,
    showConnector: Boolean = false
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = reply.authorAvatarColor ?: AppSecondary
            ) {
                if (reply.authorAvatarUrl != null) {
                    AsyncImage(model = reply.authorAvatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                }
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.authorName, color = AppTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (reply.isVerifiedAuthor) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, null, tint = AppAccent, modifier = Modifier.size(12.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("@${reply.authorHandle} \u2022 ${formatTwitterTime(reply.timestamp)}", color = AppTextSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(reply.content, color = AppTextPrimary, fontSize = 15.sp, lineHeight = 20.sp)
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PostStat(Icons.Default.FavoriteBorder, reply.likes, color = if (reply.isLiked) Color.Red else AppTextSecondary, onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLike()
                })
                PostStat(Icons.Default.Repeat, "0", color = if (reply.isReposted) Color.Green else AppTextSecondary, onClick = onRepost)
                IconButton(onClick = onBookmark, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ReplyDialog(
    post: Post,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text("Reply to @${post.authorHandle}", color = Color.White, fontSize = 18.sp) },
        text = {
            Column {
                Text(
                    text = post.content,
                    color = AppTextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Post your reply") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = AppTextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (content.isNotBlank()) onSubmit(content) }) {
                Text("Reply", color = AppAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTextSecondary)
            }
        }
    )
}

@Composable
fun CreatePostDialog(onDismiss: () -> Unit, onSubmit: (String, List<String>) -> Unit) {
    var content by remember { mutableStateOf("") }
    val selectedMediaUris = remember { mutableStateListOf<Uri>() }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedMediaUris.addAll(uris)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                Button(
                    onClick = { onSubmit(content, selectedMediaUris.map { it.toString() }) },
                    enabled = content.isNotBlank() || selectedMediaUris.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent, disabledContainerColor = AppAccent.copy(alpha = 0.5f))
                ) {
                    Text("Post", color = AppBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = AppPrimary) {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.weight(1f).heightIn(min = 150.dp),
                        placeholder = { Text("What's happening?", fontSize = 18.sp, color = AppTextSecondary) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                    )
                }

                if (selectedMediaUris.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedMediaUris) { uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedMediaUris.remove(uri) },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { launcher.launch("image/*") }) { Icon(Icons.Default.Image, null, tint = AppAccent) }
                    IconButton(onClick = { launcher.launch("video/*") }) { Icon(Icons.Default.Videocam, null, tint = AppAccent) }
                    IconButton(onClick = { /* Poll */ }) { Icon(Icons.Default.Poll, null, tint = AppAccent) }
                    IconButton(onClick = { /* Location */ }) { Icon(Icons.Default.LocationOn, null, tint = AppAccent) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(context.cacheDir, "temp_media_${System.currentTimeMillis()}")
        val outputStream = FileOutputStream(tempFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (_: Exception) {
        null
    }
}

fun resolveDisplayName(context: Context, uri: Uri): String {
    var name = "file"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}
