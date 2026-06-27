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
import com.example.firstapp.music.model.Song
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.formatTwitterTime
import com.example.firstapp.chat.data.CallType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import androidx.paging.LoadState
import com.example.firstapp.media.data.api.TrendingTopic

@Composable
fun MediaScreen(
    posts: LazyPagingItems<Post>,
    followingPosts: LazyPagingItems<Post>,
    songs: List<Song> = emptyList(),
    trendingTopics: List<TrendingTopic> = emptyList(),
    suggestedUsers: List<Contact> = emptyList(),
    mediaFeedTab: Int,
    selectedPost: Post?,
    openOwnProfileRequest: Int = 0,
    closeOwnProfileRequest: Int = 0,
    ownProfileName: String = "You",
    ownProfileHandle: String = "you",
    ownProfileAvatarColor: Color = Color(0xFF3A4B57),
    onPostSelect: (Post?) -> Unit,
    onCreatePost: (content: String, mediaUrls: List<String>, tags: List<String>) -> Unit,
    onLikePost: (postId: Int, likes: String, isLiked: Boolean) -> Unit,
    onRepostPost: (postId: Int) -> Unit,
    onReplySubmit: (postId: Int, parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit,
    onReplyRepost: (replyId: Int) -> Unit,
    onBookmarkPost: (postId: Int) -> Unit,
    onBookmarkReply: (replyId: Int) -> Unit,
    onViewPost: (postId: Int) -> Unit = {},
    onProfileViewChange: (Boolean) -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onVotePoll: (Int, Int) -> Unit = { _, _ -> },
    onFollowUser: (Int) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onCallClick: (Int, CallType) -> Unit = { _, _ -> },
    onCreatePostClick: Int = 0,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCreatePost by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var replyingToPost by remember { mutableStateOf<Post?>(null) }
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Detailed View State
    var activePostForDetail by remember { mutableStateOf<Post?>(null) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }

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
                                IconButton(onClick = onRefresh) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AppTextPrimary)
                                }
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
                        onLike = { onLikePost(it, activePostForDetail!!.likes, activePostForDetail!!.isLiked) },
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
                        onPlaySong = onPlaySong,
                        onCallClick = { type -> selectedUser?.let { onCallClick(it.id, type) } }
                    )
                }
                else -> {
                    MediaFeed(
                        posts = posts,
                        followingPosts = followingPosts,
                        songs = songs,
                        trendingTopics = trendingTopics,
                        suggestedUsers = suggestedUsers,
                        searchQuery = searchQuery,
                        mediaFeedTab = mediaFeedTab,
                        onPostSelect = { 
                            onPostSelect(it)
                            activePostForDetail = it
                            if (it != null) onViewPost(it.id)
                        },
                        onLike = { post -> onLikePost(post.id, post.likes, post.isLiked) },
                        onRepost = onRepostPost,
                        onBookmark = onBookmarkPost,
                        onProfileClick = { user ->
                            selectedUser = user
                            showProfile = true
                            onProfileViewChange(true)
                        },
                        onPlaySong = onPlaySong,
                        onImageClick = { fullScreenImageUri = it },
                        onVideoClick = { fullScreenVideoUri = it },
                        onVotePoll = onVotePoll,
                        onFollowUser = onFollowUser,
                        onArtistClick = onArtistClick
                    )
                }
            }
        }

        if (fullScreenImageUri != null) {
            MediaFullScreenImagePlayer(uri = fullScreenImageUri!!, onBack = { fullScreenImageUri = null })
        }
        if (fullScreenVideoUri != null) {
            MediaFullScreenVideoPlayer(uri = fullScreenVideoUri!!, onBack = { fullScreenVideoUri = null })
        }

        if (showCreatePost) {
            CreatePostDialog(
                onDismiss = { showCreatePost = false },
                onSubmit = { content, urls ->
                    onCreatePost(content, urls, emptyList())
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
    posts: LazyPagingItems<Post>,
    followingPosts: LazyPagingItems<Post>,
    songs: List<Song> = emptyList(),
    trendingTopics: List<TrendingTopic> = emptyList(),
    suggestedUsers: List<Contact> = emptyList(),
    searchQuery: String = "",
    mediaFeedTab: Int = 0,
    onPostSelect: (Post?) -> Unit,
    onLike: (Post) -> Unit,
    onRepost: (Int) -> Unit,
    onBookmark: (Int) -> Unit,
    onProfileClick: (User?) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onImageClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onVotePoll: (Int, Int) -> Unit = { _, _ -> },
    onFollowUser: (Int) -> Unit = {},
    onArtistClick: (String) -> Unit = {}
) {
    val activePosts = if (mediaFeedTab == 1) followingPosts else posts

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 180.dp)
    ) {
        if (mediaFeedTab == 2 && trendingTopics.isNotEmpty()) {
            item {
                TrendingTopicsSection(trendingTopics)
            }
        }

        if (songs.isNotEmpty() && mediaFeedTab == 0) {
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
                        TrendingMusicCard(
                            song = song, 
                            onClick = { onPlaySong(song, songs) },
                            onArtistClick = onArtistClick
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
            }
        }

        if (mediaFeedTab == 2 && suggestedUsers.isNotEmpty()) {
            item {
                SuggestedUsersSection(suggestedUsers, onFollow = onFollowUser)
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
            }
        }

        items(
            count = activePosts.itemCount,
            key = activePosts.itemKey { it.id },
            contentType = activePosts.itemContentType { "post" }
        ) { index ->
            val post = activePosts[index]
            if (post != null) {
                PostItem(
                    post = post,
                    onClick = { onPostSelect(post) },
                    onLike = { onLike(post) },
                    onRepost = { onRepost(post.id) },
                    onBookmark = { onBookmark(post.id) },
                    onProfileClick = {
                        onProfileClick(User(id = post.authorId, username = post.authorHandle, display_name = post.authorName, avatar_url = post.authorAvatarUrl, is_verified = post.isVerifiedAuthor))
                    },
                    onReplyClick = { onPostSelect(post) },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onVotePoll = onVotePoll
                )
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
            }
        }

        when (activePosts.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppAccent)
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    Button(onClick = { activePosts.retry() }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Retry")
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun TrendingTopicsSection(topics: List<TrendingTopic>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Trending for you", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(12.dp))
        topics.take(5).forEach { topic ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("#${topic.tag}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${topic.post_count} posts", color = AppTextSecondary, fontSize = 13.sp)
            }
        }
    }
    HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
}

@Composable
fun SuggestedUsersSection(users: List<Contact>, onFollow: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Who to follow", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(users) { user ->
                Card(
                    modifier = Modifier.width(150.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSecondary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(AppAccent.copy(alpha = 0.2f))) {
                            if (user.avatarUrl != null) {
                                AsyncImage(model = user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                            } else {
                                Text(user.displayName.take(1), modifier = Modifier.align(Alignment.Center), color = AppAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(user.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("@${user.username}", color = AppTextSecondary, fontSize = 12.sp, maxLines = 1)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onFollow(user.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingMusicCard(song: Song, onClick: () -> Unit, onArtistClick: (String) -> Unit = {}) {
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
                maxLines = 1,
                modifier = Modifier.clickable { onArtistClick(song.artist) }
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
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PostPoll(poll: PostPoll, onVote: (Int) -> Unit) {
    val totalVotes = remember(poll.options) { poll.options.sumOf { it.voteCount } }.coerceAtLeast(1L)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        poll.options.forEach { option ->
            val percentage = ((option.voteCount.toFloat() / totalVotes) * 100).toInt()
            val isSelected = poll.userVote == option.id

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AppAccent else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onVote(option.id) }
            ) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage / 100f)
                        .fillMaxHeight()
                        .background(if (isSelected) AppAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(option.text, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
                    Text("$percentage%", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
        Text("$totalVotes votes \u2022 ${poll.expiresAt}", color = AppTextSecondary, fontSize = 12.sp)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
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
    onImageClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onVotePoll: (Int, Int) -> Unit = { _, _ -> },
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

            if (post.type == PostType.POLL && post.poll != null) {
                PostPoll(
                    poll = post.poll,
                    onVote = { optionId -> onVotePoll(post.id, optionId) }
                )
            }
            
            if (post.type == PostType.SONG && post.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = { 
                        val songIdTag = post.tags.find { it.startsWith("song_id:") }
                        val songId = songIdTag?.substringAfter(":")?.toIntOrNull()
                        if (songId != null) {
                            onPlaySong(Song(id = songId, title = post.content, artist = post.authorName, albumArtUrl = post.mediaUrls.firstOrNull(), albumArtColor = AppPrimary, audioUrl = "", duration = "", category = "", playCount = "0", isLiked = false, isExplicit = false))
                        }
                    },
                    color = AppSecondary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(AppPrimary)) {
                            if (post.mediaUrls.isNotEmpty()) {
                                AsyncImage(model = post.mediaUrls.first(), contentDescription = null, contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shared Song", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(post.content.take(40), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                    }
                }
            }
            
            if (post.mediaUrls.isNotEmpty() && post.type != PostType.SONG) {
                Spacer(modifier = Modifier.height(12.dp))
                if (post.type == PostType.VIDEO_WITH_TEXT) {
                    Box(modifier = Modifier.clickable { onVideoClick(post.mediaUrls.first()) }) {
                        VideoPlayer(url = post.mediaUrls.first())
                    }
                } else if (post.mediaUrls.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onImageClick(post.mediaUrls.first()) }) {
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
                                    .background(AppSecondary.copy(alpha = 0.3f))
                                    .clickable { onImageClick(url) },
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
                    Icons.Default.Repeat, 
                    post.shares, 
                    color = if (post.isReposted) Color(0xFF4CAF50) else AppTextSecondary,
                    onClick = {
                        onRepost()
                    }
                )
                PostStat(
                    if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    post.likes, 
                    color = if (post.isLiked) Color(0xFFF44336) else AppTextSecondary,
                    onClick = {
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
    posts: LazyPagingItems<Post>,
    onPostClick: (Post) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onCallClick: (CallType) -> Unit = {}
) {
    val handle = user?.username ?: ownHandle
    val avatarColor = if (user == null) ownAvatarColor else (if (user.is_verified) AppAccent else AppSecondary)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.verticalGradient(listOf(AppSecondary, AppBackground)))
        ) {
            val bannerUrl = user?.banner_url ?: "https://images.unsplash.com/photo-1557683316-973673baf926?w=800"
            AsyncImage(
                model = bannerUrl,
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
                
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user != null) {
                        IconButton(
                            onClick = { onCallClick(CallType.VOICE) },
                            modifier = Modifier.background(AppSecondary, CircleShape).size(40.dp)
                        ) {
                            Icon(Icons.Default.Call, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { onCallClick(CallType.VIDEO) },
                            modifier = Modifier.background(AppSecondary, CircleShape).size(40.dp)
                        ) {
                            Icon(Icons.Default.VideoCall, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = if (user == null) AppSecondary else AppAccent),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(if (user == null) "Edit Profile" else "Follow", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Column(modifier = Modifier.offset(y = (-32).dp)) {
                Text(user?.display_name ?: ownName, color = AppTextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("@$handle", color = AppTextSecondary, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = user?.bio ?: "", color = AppTextPrimary, fontSize = 15.sp)
            }
        }
        
        HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f), thickness = 0.5.dp)

        Column {
            for (i in 0 until posts.itemCount) {
                val post = posts[i]
                if (post != null && post.authorHandle == handle) {
                    PostItem(
                        post = post, 
                        onClick = { onPostClick(post) }, 
                        onLike = {}, 
                        onRepost = {}, 
                        onBookmark = {}, 
                        onProfileClick = {},
                        onPlaySong = { }
                    )
                    HorizontalDivider(color = AppSecondary.copy(alpha = 0.3f))
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 180.dp)
        ) {
            item {
                PostItem(
                    post = post,
                    onClick = {},
                    onLike = { onLike(post.id) },
                    onRepost = { onRepost(post.id) },
                    onBookmark = { onBookmark(post.id) },
                    onProfileClick = {},
                    onReplyClick = { onReplyClick(post) },
                    onPlaySong = { },
                    showThreadLine = post.replies.isNotEmpty()
                )
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f), thickness = 8.dp)
            }

            if (post.replies.isNotEmpty()) {
                items(post.replies) { reply ->
                    PostReplyItem(
                        reply = reply,
                        onLike = { onReplyLike(reply.id) },
                        onRepost = { onReplyRepost(reply.id) },
                        onBookmark = { onReplyBookmark(reply.id) }
                    )
                    HorizontalDivider(color = AppSecondary.copy(alpha = 0.3f), modifier = Modifier.padding(start = 72.dp))
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
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.authorName, color = AppTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("@${reply.authorHandle} \u2022 ${formatTwitterTime(reply.timestamp)}", color = AppTextSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(reply.content, color = AppTextPrimary, fontSize = 15.sp)
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PostStat(Icons.Default.FavoriteBorder, reply.likes, color = if (reply.isLiked) Color.Red else AppTextSecondary, onClick = onLike)
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

@Composable
fun MediaFullScreenImagePlayer(uri: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).zIndex(100f)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clickable { onBack() },
            contentScale = ContentScale.Fit
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart).background(Color.Black.copy(0.4f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MediaFullScreenVideoPlayer(uri: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).zIndex(100f)) {
        VideoPlayer(url = uri)
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart).background(Color.Black.copy(0.4f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}
